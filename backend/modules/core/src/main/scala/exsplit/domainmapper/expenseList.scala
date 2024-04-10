package exsplit.domainmapper

import exsplit.spec._
import exsplit.datamapper.expenseList._
import cats.effect._
import cats.syntax.all._
import cats._
import exsplit.datamapper.expenses._
import exsplit.domainmapper._

extension [F[_]: MonadThrow](expenseMapper: ExpenseListMapper[F])
  def getExpenseListOut(id: ExpenseListId): F[ExpenseListOut] =
    for expenseList <- expenseMapper.get(id).rethrow
    yield expenseList.toExpenseListOut

  def createExpenseList(circleId: CircleId, name: String): F[ExpenseListOut] =
    val input = CreateExpenseListInput(circleId, name)
    for expenseList <- expenseMapper.create(input)
    yield expenseList.toExpenseListOut

extension (expenseList: ExpenseListReadMapper)
  def toExpenseListOut: ExpenseListOut =
    ExpenseListOut(expenseList.id, expenseList.name, expenseList.circleId)

extension (expenseLists: List[ExpenseListReadMapper])
  def toExpenseListOuts: List[ExpenseListOut] =
    expenseLists.map(_.toExpenseListOut)

case class ExpenseListDomainMapper[F[_]: MonadThrow](
    repo: ExpenseListRepository[F],
    expenseRepo: ExpenseDomainMapper[F],
    owedAmountRepo: OwedAmountRepository[F]
):

  def getExpenseListDetail(id: ExpenseListId): F[ExpenseListDetailOut] =
    for
      expenseList <- repo.main.getExpenseListOut(id)
      expenses <- expenseRepo.listExpenseOut(id)
      owedAmounts <- expenses.flatTraverse(e =>
        owedAmountRepo.detail.getOwedAmounts(ExpenseId(e.id))
      )
      owedTotal = owedAmounts.toTotalOwed
      totalExpense = expenses.map(_.price).sum
    yield ExpenseListDetailOut(
      expenseList,
      expenses,
      totalExpense,
      owedTotal
    )

  def withValidExpenseList[A](
      expenseListId: ExpenseListId
  )(action: ExpenseListDetailOut => F[A]): F[A] =
    for
      expenseList <- getExpenseListDetail(expenseListId)
      result <- action(expenseList)
    yield result
