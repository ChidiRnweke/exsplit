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

/** Contains the logic to authorize whether a user is allowed to access a
  * circle.
  *
  * @param circlesRepo
  *   The repository for circles.
  * @param userRepo
  *   The repository for users.
  */
case class CirclesAuth[F[_]: MonadThrow](
    circlesRepo: CirclesRepository[F],
    userRepo: UserMapper[F]
):
  /** Checks if the user is authorized to access the circle with the given ID.
    *
    * @param userInfo
    *   The email of the user.
    * @param circle
    *   The ID of the circle.
    * @return
    *   A computation that yields unit if the user is authorized. Otherwise, a
    *   computation that raises an error.
    */
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

/** Contains the logic to authorize whether a user is allowed to access a circle
  * member.
  *
  * @param userRepo
  *   The repository for users.
  * @param circlesRepo
  *   The repository for circles.
  * @param circleMembersRepo
  *   The repository for circle members.
  */
case class CircleMemberAuth[F[_]: MonadThrow](
    userRepo: UserMapper[F],
    circlesRepo: CirclesRepository[F],
    circleMembersRepo: CircleMembersRepository[F]
):
  /** Checks if the user is authorized to access the circle member with the
    * given ID. If they belong to the same circle as the member, they are
    * authorized.
    *
    * @param userInfo
    *   The email of the user.
    * @param circleMemberId
    *   The ID of the circle member.
    * @return
    *   A computation that yields unit if the user is authorized. Otherwise, a
    *   computation that raises an error.
    */
  def authCheck(userInfo: F[Email], circleMemberId: CircleMemberId): F[Unit] =
    AuthCheck.checkAuthorization(
      userInfo,
      circleMemberId,
      circleFromUser,
      extractCircle
    )

  /** Checks if the user is authorized to access the circle member with the
    * given ID. If they have exactly the same circle member id, they are
    * authorized. This is relevant for the user to delete or update their own
    * circle member.
    *
    * @param userInfo
    *   The email of the user.
    * @param circleMemberId
    *   The ID of the circle member.
    * @return
    *   A computation that yields unit if the user is authorized. Otherwise, a
    *   computation that raises an error.
    */
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

/** Contains the logic to authorize whether a user is allowed to access a user.
  *
  * @param userRepo
  *   The repository for users.
  */
case class UserAuth[F[_]: MonadThrow](userRepo: UserMapper[F]):
  /** Checks if the user is authorized to access the user with the given ID. If
    * the user is the same as the user with the given ID, they are authorized.
    *
    * @param userInfo
    *   The email of the user.
    * @param userId
    *   The ID of the user.
    * @return
    *   A computation that yields unit if the user is authorized. Otherwise, a
    *   computation that raises an error.
    */
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

/** Contains the logic to authorize whether a user is allowed to access an
  * expense list. The user is authorized if they belong to the same circle as
  * the expense list.
  *
  * @param userRepo
  *   The repository for users.
  * @param circlesRepo
  *   The repository for circles.
  * @param expenseListRepo
  *   The repository for expense lists.
  * @param expenseRepo
  *   The repository for expenses.
  */
case class ExpenseAuth[F[_]: MonadThrow](
    userRepo: UserMapper[F],
    circlesRepo: CirclesRepository[F],
    expenseListRepo: ExpenseListRepository[F],
    expenseRepo: ExpenseRepository[F]
):
  /** Checks if the user is authorized to access the expense with the given ID.
    * If they belong to the same circle as the expense, they are authorized.
    *
    * Note: quite a lot of IO operations are performed in this method. It is
    * subject to change in the future while retaining the same signature.
    *
    * @param userInfo
    *   The email of the user.
    * @param expenseId
    *   The ID of the expense.
    * @return
    *   A computation that yields unit if the user is authorized. Otherwise, a
    *   computation that raises an error.
    */
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

/** Contains the logic to authorize whether a user is allowed to access an
  * expense list. The user is authorized if they belong to the same circle as
  * the expense list.
  *
  * @param userRepo
  *   The repository for users.
  * @param circlesRepo
  *   The repository for circles.
  * @param expenseListRepo
  *   The repository for expense lists.
  */
case class ExpenseListAuth[F[_]: MonadThrow](
    userRepo: UserMapper[F],
    circlesRepo: CirclesRepository[F],
    expenseListRepo: ExpenseListRepository[F]
):
  /** Checks if the user is authorized to access the expense list with the given
    * ID. If they belong to the same circle as the expense list, they are
    * authorized.
    *
    * @param userInfo
    *   The email of the user.
    * @param expenseListId
    *   The ID of the expense list.
    * @return
    *   A computation that yields unit if the user is authorized. Otherwise, a
    *   computation that raises an error.
    */
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
