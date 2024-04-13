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
import smithy4s.Timestamp
import exsplit.expenses.ExpensesEntryPoint
import exsplit.datamapper.expenseList.ExpenseListRepository
import exsplit.datamapper.expenses.ExpenseRepository
import exsplit.database._

/** Entry point for creating an instance of the ExpenseService with
  * authorization. It is a wrapper around the ExpensesEntryPoint that adds
  * authorization to the service.
  */
object ExpenseServiceWithAuth:
  /** Creates an instance of ExpenseService with authorization from the provided
    * session. The session pool is eventually passed down to involved
    * repositories.
    *
    * @param userInfo
    *   The email of the authenticated user. Obtained from the fiber local
    *   through the middleware.
    * @param pool
    *   The AppSessionPool used for database operations.
    * @return
    *   An instance of ExpenseService with authorization.
    */
  def fromSession[F[_]: Concurrent: Parallel](
      userInfo: F[Email],
      pool: AppSessionPool[F]
  ): ExpenseService[F] =
    val service = ExpensesEntryPoint.fromSession(pool)
    val userMapper = UserMapper.fromSession(pool)
    val circlesRepo = CirclesRepository.fromSession(pool)
    val membersRepo = CircleMembersRepository.fromSession(pool)
    val expListRepo = ExpenseListRepository.fromSession(pool)
    val expenseRepo = ExpenseRepository.fromSession(pool)
    makeAuthService(
      userInfo,
      service,
      userMapper,
      expenseRepo,
      circlesRepo,
      membersRepo,
      expListRepo
    )
  private def makeAuthService[F[_]: MonadThrow: Parallel](
      userInfo: F[Email],
      service: ExpenseService[F],
      userMapper: UserMapper[F],
      expenseRepo: ExpenseRepository[F],
      circlesRepo: CirclesRepository[F],
      membersRepo: CircleMembersRepository[F],
      expListRepo: ExpenseListRepository[F]
  ): ExpenseServiceWithAuth[F] =
    val memberAuth = CircleMemberAuth(userMapper, circlesRepo, membersRepo)
    val listAuth = ExpenseListAuth(userMapper, circlesRepo, expListRepo)
    val expAuth = ExpenseAuth(userMapper, circlesRepo, expListRepo, expenseRepo)
    ExpenseServiceWithAuth(userInfo, memberAuth, listAuth, expAuth, service)

/** ExpenseService with authorization. It is a wrapper around the
  * ExpensesEntryPoint that adds authorization to the service. Authorization
  * checks are run concurrently where possible. After the checks are successful,
  * the service is run. The eventual interpreter of the service is able to run
  * this as a regular ExpenseService.
  *
  * @param userInfo
  * @param circleMemberAuth
  * @param expenseListAuth
  * @param expenseAuth
  * @param service
  */
case class ExpenseServiceWithAuth[F[_]: Monad: Parallel](
    userInfo: F[Email],
    circleMemberAuth: CircleMemberAuth[F],
    expenseListAuth: ExpenseListAuth[F],
    expenseAuth: ExpenseAuth[F],
    service: ExpenseService[F]
) extends ExpenseService[F]:

  def createExpense(
      expenseListId: ExpenseListId,
      paidBy: CircleMemberId,
      description: String,
      price: Amount,
      date: Timestamp,
      owedToPayer: List[OwedAmount]
  ): F[CreateExpenseOutput] =
    (
      expenseListAuth.authCheck(userInfo, expenseListId),
      circleMemberAuth.authCheck(userInfo, paidBy)
    ).parTupled *> service.createExpense(
      expenseListId,
      paidBy,
      description,
      price,
      date,
      owedToPayer
    )
  def deleteExpense(id: ExpenseId): F[Unit] =
    expenseAuth.authCheck(userInfo, id) *> service.deleteExpense(id)

  def getExpense(id: ExpenseId): F[GetExpenseOutput] =
    expenseAuth.authCheck(userInfo, id) *> service.getExpense(id)

  def updateExpense(
      id: ExpenseId,
      paidBy: Option[CircleMemberId],
      description: Option[String],
      price: Option[Amount],
      date: Option[Timestamp],
      owedToPayer: Option[List[OwedAmount]]
  ): F[Unit] =
    (
      expenseAuth.authCheck(userInfo, id),
      paidBy.traverse(circleMemberAuth.authCheck(userInfo, _))
    ).parTupled *> service.updateExpense(
      id,
      paidBy,
      description,
      price,
      date,
      owedToPayer
    )
