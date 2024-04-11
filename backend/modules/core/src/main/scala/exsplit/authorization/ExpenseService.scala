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

object ExpenseServiceWithAuth:
  def fromSession[F[_]: Concurrent](
      userInfo: F[Email],
      session: Session[F]
  ): F[ExpenseService[F]] =
    for
      service <- ExpensesEntryPoint.fromSession(session)
      userMapper <- UserMapper.fromSession(session)
      expenseRepo <- ExpenseRepository.fromSession(session)
      circlesRepo <- CirclesRepository.fromSession(session)
      membersRepo <- CircleMembersRepository.fromSession(session)
      expenseListRepo <- ExpenseListRepository.fromSession(session)
      circleMemberAuth = CircleMemberAuth(userMapper, circlesRepo, membersRepo)
      expenseListAuth = ExpenseListAuth(
        userMapper,
        circlesRepo,
        expenseListRepo
      )
      expenseAuth = ExpenseAuth(
        userMapper,
        circlesRepo,
        expenseListRepo,
        expenseRepo
      )
    yield ExpenseServiceWithAuth(
      userInfo,
      circleMemberAuth,
      expenseListAuth,
      expenseAuth,
      service
    )

case class ExpenseServiceWithAuth[F[_]: Monad](
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
    for
      _ <- expenseListAuth.authCheck(userInfo, expenseListId)
      _ <- circleMemberAuth.authCheck(userInfo, paidBy)
      res <- service.createExpense(
        expenseListId,
        paidBy,
        description,
        price,
        date,
        owedToPayer
      )
    yield res
  def deleteExpense(id: ExpenseId): F[Unit] =
    for _ <- expenseAuth.authCheck(userInfo, id)
    yield service.deleteExpense(id)

  def getExpense(id: ExpenseId): F[GetExpenseOutput] =
    for
      _ <- expenseAuth.authCheck(userInfo, id)
      res <- service.getExpense(id)
    yield res

  def updateExpense(
      id: ExpenseId,
      paidBy: Option[CircleMemberId],
      description: Option[String],
      price: Option[Amount],
      date: Option[Timestamp],
      owedToPayer: Option[List[OwedAmount]]
  ): F[Unit] =
    for
      _ <- expenseAuth.authCheck(userInfo, id)
      _ <- paidBy.traverse(circleMemberAuth.authCheck(userInfo, _))
      _ <- service.updateExpense(
        id,
        paidBy,
        description,
        price,
        date,
        owedToPayer
      )
    yield ()
