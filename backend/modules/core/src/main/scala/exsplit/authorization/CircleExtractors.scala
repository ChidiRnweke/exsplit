package exsplit.authorization.extractors

import exsplit.spec._
import cats.effect._
import cats.syntax.all._
import cats._
import exsplit.auth._
import exsplit.datamapper.circles._
import exsplit.datamapper.user.UserMapper
import exsplit.datamapper.expenses.ExpenseRepository
import exsplit.datamapper.expenseList.ExpenseListRepository
import exsplit.domainmapper._
import exsplit.authorization._

type NotFoundCircleId = [F[_]] =>> F[Either[NotFoundError, String]]

extension [F[_]: Applicative](circlesRepo: CirclesRepository[F])
  def extractCircle(circleId: CircleId): NotFoundCircleId[F] =
    Right(circleId.value).pure[F]

extension [F[_]: Applicative](membersRepo: CircleMembersRepository[F])
  def extractCircle(circleMemberId: CircleMemberId): NotFoundCircleId[F] =
    membersRepo.get(circleMemberId).map(_.map(_.circleId))

extension [F[_]: MonadThrow](expenseRepo: ExpenseRepository[F])
  def extractCircle(
      expenseId: ExpenseId,
      expenseListRepo: ExpenseListRepository[F]
  ): NotFoundCircleId[F] =
    for
      expenseMaybe <- expenseRepo.get(expenseId)
      expenseListId <- expenseMaybe
        .leftMap(_ => forbiddenError)
        .liftTo[F]
        .map(expense => ExpenseListId(expense.expenseListId))
      expenseList <- expenseListRepo.get(expenseListId)
    yield expenseList.map(_.circleId)

extension [F[_]: MonadThrow](repo: ExpenseListRepository[F])
  def extractCircle(expenseListId: ExpenseListId): NotFoundCircleId[F] =
    for expenseList <- repo.get(expenseListId)
    yield expenseList.map(_.circleId)

extension [F[_]: MonadThrow](userRepo: UserMapper[F])
  def extractCircle(
      email: F[Email],
      repo: CirclesRepository[F]
  ): F[List[String]] =
    for
      email_ <- email
      userEither <- userRepo.findUserByEmail(email_)
      user <- userEither
        .leftMap(_ => forbiddenError)
        .liftTo[F]
      circles <- repo.listPrimaries(UserId(user.id))
    yield circles.map(_.id)

case class CirclesAuth[F[_]: MonadThrow](
    circlesRepo: CirclesRepository[F],
    userRepo: UserMapper[F]
):
  private val circleFromUser =
    userCircleExtractor.apply(circlesRepo)(userRepo)

  private def extractCircle: CircleId => NotFoundCircleId[F] =
    circleId => circlesRepo.extractCircle(circleId)

  def authCheck(userInfo: F[Email], circle: CircleId): F[Unit] =
    AuthCheck.checkAuthorization(
      userInfo,
      circle,
      circleFromUser,
      extractCircle
    )

case class CircleMemberAuth[F[_]: MonadThrow](
    userRepo: UserMapper[F],
    circleMembersRepo: CircleMembersRepository[F]
):
  private def emailToCircleMembersId(
      email: Email
  ): F[List[String]] =
    for
      userEither <- userRepo.findUserByEmail(email)
      user <- userEither
        .leftMap(_ => forbiddenError)
        .liftTo[F]
      circleMemberList <- circleMembersRepo.byUserId(UserId(user.id))
    yield circleMemberList.map(_.id)

  def authCheck(
      userInfo: F[Email],
      circleMemberId: CircleMemberId
  ): F[Unit] =
    for
      email <- userInfo
      memberIds <- emailToCircleMembersId(email)
      isMember = memberIds.contains(circleMemberId.value)
      _ <- if isMember then ().pure[F] else forbiddenError.raiseError[F, Unit]
    yield ()

case class UserAuth[F[_]: MonadThrow](userRepo: UserMapper[F]):
  private def emailToId(email: Email): F[Either[NotFoundError, String]] =
    userRepo.findUserByEmail(email).map(_.map(_.id))

  def authCheck(userInfo: F[Email], userId: UserId): F[Unit] =
    for
      email <- userInfo
      subjectEmail <- emailToId(email)
      subjectEmailMaybe = subjectEmail.leftMap(_ => forbiddenError)
      _ <- subjectEmailMaybe
        .liftTo[F]
        .ensure(forbiddenError)(_ == userId.value)
        .void
    yield ()

def userCircleExtractor[F[_]: MonadThrow] =
  (circlesRepo: CirclesRepository[F]) =>
    (userRepo: UserMapper[F]) =>
      (email: F[Email]) => userRepo.extractCircle(email, circlesRepo)
