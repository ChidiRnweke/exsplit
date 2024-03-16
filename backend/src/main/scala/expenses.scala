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

  def createExpense(expense: Expense, expenseListId: ExpenseListId): F[Unit] =
    repo.createExpense(
      expenseListId,
      expense.initialPayer.id.value.toString(),
      expense.description,
      expense.price,
      expense.date,
      expense.owedToInitialPayer
    )

  def getExpense(
      id: ExpenseId
  ): F[GetExpenseOutput] =
    repo.getExpense(id).map(GetExpenseOutput(_))
  def deleteExpense(
      id: ExpenseId
  ): F[Unit] =
    repo.deleteExpense(id)
  def updateExpense(
      id: ExpenseId,
      initialPayer: Option[String],
      description: Option[String],
      price: Option[Amount],
      date: Option[Date],
      owedToInitialPayer: Option[List[OwedAmount]]
  ): F[Unit] =
    repo.updateExpense(
      id,
      initialPayer,
      description,
      price,
      date,
      owedToInitialPayer
    )
trait ExpenseRepository[F[_]]:
  def getExpense(id: ExpenseId): F[Expense]

  def createExpense(
      expenseListId: ExpenseListId,
      initialPayer: String,
      description: String,
      price: Amount,
      date: Date,
      owedToInitialPayer: List[OwedAmount]
  ): F[Unit]

  def deleteExpense(id: ExpenseId): F[Unit]

  def updateExpense(
      id: ExpenseId,
      initialPayer: Option[String],
      description: Option[String],
      price: Option[Amount],
      date: Option[Date],
      owedToInitialPayer: Option[List[OwedAmount]]
  ): F[Unit]
