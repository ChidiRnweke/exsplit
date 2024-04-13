package exsplit.authorization

import cats.MonadThrow
import exsplit.authorization.extractors._
import exsplit.spec._
import exsplit.circles._
import cats.effect._
import cats.syntax.all._
import cats._
import skunk.Session
import exsplit.datamapper.user.UserMapper
import exsplit.datamapper.circles._
import exsplit.expenseList.ExpenseListEntryPoint
import exsplit.datamapper.expenseList.ExpenseListRepository
import exsplit.datamapper.expenses.ExpenseRepository
import exsplit.database._

/** Entry point for creating an instance of the ExpenseListService with
  * authorization. It is a wrapper around the ExpenseListEntryPoint that adds
  * authorization to the service.
  */
object ExpenseListServiceWithAuth:
  /** Creates an instance of ExpenseListService with authorization from the
    * provided session. The session pool is eventually passed down to involved
    * repositories.
    *
    * @param userInfo
    *   The email of the authenticated user. Obtained from the fiber local
    *   through the middleware.
    * @param pool
    *   The AppSessionPool used for database operations.
    * @return
    *   An instance of ExpenseListService with authorization.
    */
  def fromSession[F[_]: Concurrent: Parallel](
      userInfo: F[Email],
      pool: AppSessionPool[F]
  ): ExpenseListService[F] =

    val service = ExpenseListEntryPoint.fromSession(pool)
    val userMapper = UserMapper.fromSession(pool)
    val circlesRepo = CirclesRepository.fromSession(pool)
    val membersRepo = CircleMembersRepository.fromSession(pool)
    val listRepo = ExpenseListRepository.fromSession(pool)
    val expenseRepo = ExpenseRepository.fromSession(pool)
    makeAuthService(
      userInfo,
      service,
      userMapper,
      circlesRepo,
      membersRepo,
      listRepo,
      expenseRepo
    )

  private def makeAuthService[F[_]: MonadThrow: Parallel](
      userInfo: F[Email],
      service: ExpenseListService[F],
      userMapper: UserMapper[F],
      circlesRepo: CirclesRepository[F],
      membersRepo: CircleMembersRepository[F],
      listRepo: ExpenseListRepository[F],
      expenseRepo: ExpenseRepository[F]
  ) =
    val circlesAuth = CirclesAuth(circlesRepo, userMapper)
    val membersAuth = CircleMemberAuth(userMapper, circlesRepo, membersRepo)
    val listAuth = ExpenseListAuth(userMapper, circlesRepo, listRepo)
    val expenseAuth =
      ExpenseAuth(userMapper, circlesRepo, listRepo, expenseRepo)

    ExpenseListServiceWithAuth(
      userInfo,
      circlesAuth,
      membersAuth,
      listAuth,
      expenseAuth,
      service
    )

/** ExpenseListService with authorization. It wraps the ExpenseListService and
  * adds authorization checks to the methods Authorization checks are run
  * concurrently where possible. After the checks are successful, the service is
  * run. The eventual interpreter of the service is able to run this as a
  * ExpenseListService.
  *
  * @param userInfo
  *   The email of the authenticated user.
  * @param circlesAuth
  *   The authorization service for circles.
  * @param circleMemberAuth
  *   The authorization service for circle members.
  * @param expenseListAuth
  *   The authorization service for expense lists.
  * @param expenseAuth
  *   The authorization service for expenses.
  * @param service
  *   The ExpenseListService to wrap.
  */
case class ExpenseListServiceWithAuth[F[_]: Monad: Parallel](
    userInfo: F[Email],
    circlesAuth: CirclesAuth[F],
    circleMemberAuth: CircleMemberAuth[F],
    expenseListAuth: ExpenseListAuth[F],
    expenseAuth: ExpenseAuth[F],
    service: ExpenseListService[F]
) extends ExpenseListService[F]:

  def createExpenseList(
      circleId: CircleId,
      name: String
  ): F[CreateExpenseListOutput] =
    circlesAuth.authCheck(userInfo, circleId) *>
      service.createExpenseList(circleId, name)

  def deleteExpenseList(id: ExpenseListId): F[Unit] =
    expenseListAuth.authCheck(userInfo, id) *>
      service.deleteExpenseList(id)

  def getExpenseList(
      expenseListId: ExpenseListId,
      onlyOutstanding: Option[Boolean]
  ): F[GetExpenseListOutput] =
    expenseListAuth.authCheck(userInfo, expenseListId) *>
      service.getExpenseList(expenseListId, onlyOutstanding)

  def getExpenseLists(circleId: CircleId): F[GetExpenseListsOutput] =
    circlesAuth.authCheck(userInfo, circleId) *>
      service.getExpenseLists(circleId)

  def getSettledExpenseLists(
      expenseListId: ExpenseListId
  ): F[GetSettledExpenseListsOutput] =
    expenseListAuth.authCheck(userInfo, expenseListId) *>
      service.getSettledExpenseLists(expenseListId)

  def settleExpenseList(
      expenseListId: ExpenseListId,
      fromMemberId: CircleMemberId,
      toMemberId: CircleMemberId,
      amount: Amount
  ): F[Unit] =
    (
      expenseListAuth.authCheck(userInfo, expenseListId),
      circleMemberAuth.authCheck(userInfo, fromMemberId),
      circleMemberAuth.authCheck(userInfo, toMemberId)
    ).parTupled *>
      service.settleExpenseList(
        expenseListId,
        fromMemberId,
        toMemberId,
        amount
      )
  def updateExpenseList(id: ExpenseListId, name: String): F[Unit] =
    expenseListAuth.authCheck(userInfo, id) *>
      service.updateExpenseList(id, name)
