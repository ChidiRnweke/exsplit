package Expenses

import exsplit.spec._
import exsplit.spec.ExpenseServiceOperation.CreateExpenseError
import cats.syntax.all._
import cats.data._
import cats._

object ExpensesEntryPoint:
  def createService[F[_]: Functor](
      repo: ExpenseRepository[F]
  ): ExpenseServiceImpl[F] =
    ExpenseServiceImpl(repo)

case class ExpenseServiceImpl[F[_]: Functor](repo: ExpenseRepository[F])
    extends ExpenseService[F]:
  def createExpense(input: CreateExpenseInput): F[Unit] = ???
  def getExpense(
      id: ExpenseId,
      circleId: CircleId,
      expenseListId: ExpenseListId
  ): F[GetExpenseOutput] =
    repo.getExpense(id, circleId, expenseListId).map(GetExpenseOutput(_))
  def deleteExpense(
      id: ExpenseId,
      circleId: CircleId,
      expenseListId: ExpenseListId
  ): F[Unit] =
    repo.deleteExpense(id, circleId, expenseListId)
  def updateExpense(
      id: ExpenseId,
      circleId: CircleId,
      expenseListId: ExpenseListId,
      initialPayer: Option[String],
      description: Option[String],
      price: Option[Amount],
      date: Option[Date],
      owedToInitialPayer: Option[List[OwedAmount]]
  ): F[Unit] =
    repo.updateExpense(
      id,
      circleId,
      expenseListId,
      initialPayer,
      description,
      price,
      date,
      owedToInitialPayer
    )

trait ExpenseRepository[F[_]]:
  def getExpense(
      id: ExpenseId,
      circleId: CircleId,
      expenseListId: ExpenseListId
  ): F[Expense]

  def createExpense(
      circleId: CircleId,
      expenseListId: ExpenseListId,
      initialPayer: String,
      description: String,
      price: Amount,
      date: Date,
      owedToInitialPayer: List[OwedAmount]
  ): F[Unit]

  def deleteExpense(
      id: ExpenseId,
      circleId: CircleId,
      expenseListId: ExpenseListId
  ): F[Unit]

  def updateExpense(
      id: ExpenseId,
      circleId: CircleId,
      expenseListId: ExpenseListId,
      initialPayer: Option[String],
      description: Option[String],
      price: Option[Amount],
      date: Option[Date],
      owedToInitialPayer: Option[List[OwedAmount]]
  ): F[Unit]
