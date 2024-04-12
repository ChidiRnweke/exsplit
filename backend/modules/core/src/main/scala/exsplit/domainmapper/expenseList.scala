package exsplit.domainmapper

import exsplit.spec._
import exsplit.datamapper.expenseList._
import cats.effect._
import cats.syntax.all._
import cats._
import exsplit.datamapper.expenses._
import exsplit.domainmapper._
import exsplit.datamapper.circles._

extension [F[_]: MonadThrow](expenseMapper: ExpenseListRepository[F])
  def getExpenseListOut(id: ExpenseListId): F[ExpenseListOut] =
    expenseMapper.get(id).rethrow.map(_.toExpenseListOut)

  def createExpenseList(circleId: CircleId, name: String): F[ExpenseListOut] =
    val input = CreateExpenseListInput(circleId, name)
    expenseMapper.create(input).map(_.toExpenseListOut)

extension (expenseList: ExpenseListReadMapper)
  def toExpenseListOut: ExpenseListOut =
    ExpenseListOut(expenseList.id, expenseList.name, expenseList.circleId)

extension (expenseLists: List[ExpenseListReadMapper])
  def toExpenseListOuts: List[ExpenseListOut] =
    expenseLists.map(_.toExpenseListOut)

extension [F[_]: MonadThrow: Parallel](repo: ExpenseListRepository[F])

  def getExpenseListDetail(
      id: ExpenseListId,
      expenseRepo: ExpenseRepository[F],
      owedAmountRepo: OwedAmountRepository[F],
      circleMembersRepo: CircleMembersRepository[F]
  ): F[ExpenseListDetailOut] =
    (
      repo.getExpenseListOut(id),
      expenseRepo.listExpenseOut(id, circleMembersRepo, owedAmountRepo)
    ).parFlatMapN: (expenseList, expenses) =>
      for
        owedAmounts <- expenses.parFlatTraverse(e =>
          owedAmountRepo.getOwedAmounts(ExpenseId(e.id))
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
  )(action: ExpenseListOut => F[A]): F[A] =
    for
      expenseList <- repo.getExpenseListOut(expenseListId)
      result <- action(expenseList)
    yield result
