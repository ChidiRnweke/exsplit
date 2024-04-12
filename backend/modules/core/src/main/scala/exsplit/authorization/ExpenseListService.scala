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

object ExpenseListServiceWithAuth:
  def fromSession[F[_]: Concurrent: Parallel](
      userInfo: F[Email],
      session: Session[F]
  ): F[ExpenseListService[F]] =
    (
      ExpenseListEntryPoint.fromSession(session),
      UserMapper.fromSession(session),
      CirclesRepository.fromSession(session),
      CircleMembersRepository.fromSession(session),
      ExpenseListRepository.fromSession(session),
      ExpenseRepository.fromSession(session)
    ).mapN(makeAuthService(userInfo, _, _, _, _, _, _))

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
