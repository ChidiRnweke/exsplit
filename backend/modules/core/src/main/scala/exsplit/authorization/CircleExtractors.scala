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

case class CirclesAuth[F[_]: MonadThrow](
    circlesRepo: CirclesRepository[F],
    userRepo: UserMapper[F]
):
  def authCheck(userInfo: F[Email], circle: CircleId): F[Unit] =
    AuthCheck.checkAuthorization(
      userInfo,
      circle,
      circleFromUser,
      extractCircle
    )
  private val circleFromUser = UserCircleExtractor(circlesRepo, userRepo)

  private def extractCircle(circleId: CircleId): NotFoundCircleId[F] =
    Right(circleId.value).pure[F]

case class CircleMemberAuth[F[_]: MonadThrow](
    userRepo: UserMapper[F],
    circlesRepo: CirclesRepository[F],
    circleMembersRepo: CircleMembersRepository[F]
):

  def authCheck(userInfo: F[Email], circleMemberId: CircleMemberId): F[Unit] =
    AuthCheck.checkAuthorization(
      userInfo,
      circleMemberId,
      circleFromUser,
      extractCircle
    )

  def sameCircleMemberId(
      userInfo: F[Email],
      circleMemberId: CircleMemberId
  ): F[Unit] =
    for
      email <- userInfo
      memberIds <- emailToCircleMembersId(email)
      isMember = memberIds.contains(circleMemberId.value)
      _ <- if isMember then ().pure[F] else forbiddenError.raiseError[F, Unit]
    yield ()

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

  private val circleFromUser = UserCircleExtractor(circlesRepo, userRepo)

  private def extractCircle(
      circleMemberId: CircleMemberId
  ): NotFoundCircleId[F] =
    circleMembersRepo.get(circleMemberId).map(_.map(_.circleId))

case class UserAuth[F[_]: MonadThrow](userRepo: UserMapper[F]):

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

  private def emailToId(email: Email): F[Either[NotFoundError, String]] =
    userRepo.findUserByEmail(email).map(_.map(_.id))

case class ExpenseAuth[F[_]: MonadThrow](
    userRepo: UserMapper[F],
    circlesRepo: CirclesRepository[F],
    expenseListRepo: ExpenseListRepository[F],
    expenseRepo: ExpenseRepository[F]
):

  def authCheck(userInfo: F[Email], expenseId: ExpenseId): F[Unit] =
    AuthCheck.checkAuthorization(
      userInfo,
      expenseId,
      circleFromUser,
      extractCircle
    )
  private val circleFromUser = UserCircleExtractor(circlesRepo, userRepo)

  private def extractCircle(expenseId: ExpenseId): NotFoundCircleId[F] =
    for
      expenseMaybe <- expenseRepo.get(expenseId)
      expenseListId <- expenseMaybe
        .leftMap(_ => forbiddenError)
        .liftTo[F]
        .map(expense => ExpenseListId(expense.expenseListId))
      expenseList <- expenseListRepo.get(expenseListId)
    yield expenseList.map(_.circleId)

case class ExpenseListAuth[F[_]: MonadThrow](
    userRepo: UserMapper[F],
    circlesRepo: CirclesRepository[F],
    expenseListRepo: ExpenseListRepository[F]
):
  def authCheck(userInfo: F[Email], expenseListId: ExpenseListId): F[Unit] =
    AuthCheck.checkAuthorization(
      userInfo,
      expenseListId,
      circleFromUser,
      extractCircle
    )
  private val circleFromUser = UserCircleExtractor(circlesRepo, userRepo)

  private def extractCircle(expenseListId: ExpenseListId): NotFoundCircleId[F] =
    for expenseList <- expenseListRepo.get(expenseListId)
    yield expenseList.map(_.circleId)
