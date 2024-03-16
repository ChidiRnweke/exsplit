package exsplit.expenseList

import exsplit.spec._
import cats.syntax.all._
import cats._
import cats.data._

object ExpenseListEntryPoint:
  def createService[F[_]: Functor](
      repo: ExpenseListRepository[F]
  ): ExpenseListServiceImpl[F] =
    ExpenseListServiceImpl(repo)

case class ExpenseListServiceImpl[F[_]: Functor](repo: ExpenseListRepository[F])
    extends ExpenseListService[F]:
  def createExpenseList(
      circleId: CircleId,
      name: String
  ): F[CreateExpenseListOutput] =
    repo.createExpenseList(circleId, name).map(CreateExpenseListOutput(_))
  def getExpenseListById(
      expenseListId: ExpenseListId
  ): F[GetExpenseListByIdOutput] =
    repo
      .getExpenseListDetailById(expenseListId)
      .map(GetExpenseListByIdOutput(_))

  def deleteExpenseList(id: ExpenseListId): F[Unit] =
    repo.deleteExpenseList(id)
  def getExpenseLists(circleId: CircleId): F[GetExpenseListsOutput] =
    repo.getExpenseLists(circleId).map(GetExpenseListsOutput(_))
  def updateExpenseList(id: ExpenseListId, name: String): F[Unit] =
    repo.updateExpenseList(id, name)

trait ExpenseListRepository[F[_]]:
  def getExpenseListDetailById(
      expenseListId: ExpenseListId
  ): F[ExpenseListDetail]
  def getExpenseListById(
      expenseListId: ExpenseListId
  ): F[ExpenseList]
  def createExpenseList(
      circleId: CircleId,
      name: String
  ): F[ExpenseList]
  def deleteExpenseList(id: ExpenseListId): F[Unit]
  def getExpenseLists(circleId: CircleId): F[List[ExpenseList]]
  def updateExpenseList(id: ExpenseListId, name: String): F[Unit]
