package exsplit.expenses

import exsplit.spec._
import exsplit.spec.ExpenseServiceOperation.CreateExpenseError
import cats.syntax.all._
import cats.data._
import cats._
import exsplit.expenseList._

object ExpensesEntryPoint:
  def createService[F[_]: MonadThrow](
      expenseRepository: ExpenseRepository[F],
      expenseListRepository: ExpenseListRepository[F]
  ): ExpenseServiceImpl[F] =
    ExpenseServiceImpl(expenseRepository, expenseListRepository)

case class ExpenseServiceImpl[F[_]: MonadThrow](
    expenseRepository: ExpenseRepository[F],
    expenseListRepository: ExpenseListRepository[F]
) extends ExpenseService[F]:

  private def withValidExpenseList[A](
      expenseListId: ExpenseListId
  )(action: ExpenseListOut => F[A]): F[A] =
    for
      expenseList <- expenseListRepository.getExpenseList(expenseListId).rethrow
      result <- action(expenseList)
    yield result

  private def withValidExpense[A](
      expenseId: ExpenseId
  )(action: ExpenseOut => F[A]): F[A] =
    for
      expense <- expenseRepository.getExpense(expenseId).rethrow
      result <- action(expense)
    yield result

  def createExpense(
      expenseListId: ExpenseListId,
      expense: Expense
  ): F[CreateExpenseOutput] =
    withValidExpenseList(expenseListId): expenseList =>
      expenseRepository
        .createExpense(expenseList, expense)
        .map(CreateExpenseOutput(_))

  def getExpense(id: ExpenseId): F[GetExpenseOutput] =
    withValidExpense(id): expense =>
      GetExpenseOutput(expense).pure[F]

  def deleteExpense(id: ExpenseId): F[Unit] =
    withValidExpense(id): expense =>
      expenseRepository.deleteExpense(expense)

  def updateExpense(
      id: ExpenseId,
      paidBy: Option[CircleMember],
      description: Option[String],
      price: Option[Amount],
      date: Option[Date],
      owedToPayer: Option[List[OwedAmount]]
  ): F[Unit] =
    withValidExpense(id): expense =>
      expenseRepository.updateExpense(
        expense,
        paidBy,
        description,
        price,
        date,
        owedToPayer
      )

trait ExpenseRepository[F[_]]:
  def getExpense(id: ExpenseId): F[Either[NotFoundError, ExpenseOut]]

  def createExpense(
      expenseListId: ExpenseListOut,
      expense: Expense
  ): F[ExpenseOut]

  def deleteExpense(expense: ExpenseOut): F[Unit]

  def updateExpense(
      expense: ExpenseOut,
      paidBy: Option[CircleMember],
      description: Option[String],
      price: Option[Amount],
      date: Option[Date],
      owedToPayer: Option[List[OwedAmount]]
  ): F[Unit]
