package exsplit.domainmapper

import exsplit.spec._
import exsplit.datamapper.expenseList._
import cats.effect._
import cats.syntax.all._
import cats._
import exsplit.datamapper.expenses._
import exsplit.datamapper.circles._

case class ExpenseDomainMapper[F[_]: MonadThrow](
    circleMemberRepo: CircleMembersRepository[F],
    owedAmountsRepo: OwedAmountRepository[F],
    repo: ExpenseRepository[F]
):
  def getExpenseOut(id: ExpenseId): F[ExpenseOut] =
    for
      expense <- repo.detail.get(id).rethrow
      memberId = CircleMemberId(expense.paidBy)
      paidBy <- circleMemberRepo.getCircleMemberOut(memberId)
      owedAmounts <- owedAmountsRepo.detail.getOwedAmounts(id)
    yield ExpenseOut(
      expense.id,
      paidBy,
      expense.description,
      expense.price,
      expense.date,
      owedAmounts
    )

  def listExpenseOut(id: ExpenseListId): F[List[ExpenseOut]] =
    for
      expenses <- repo.byExpenseList.listChildren(id)
      expenseOuts <- expenses.traverse: expense =>
        getExpenseOut(ExpenseId(expense.id))
    yield expenseOuts

extension (owedAmount: OwedAmountDetailRead)
  def toOwedAmountOut: OwedAmountOut =
    val from =
      CircleMemberOut(owedAmount.fromMember, owedAmount.fromMemberName)
    val to =
      CircleMemberOut(owedAmount.toMember, owedAmount.toMemberName)
    OwedAmountOut(fromMember = from, toMember = to, owedAmount.amount)

extension [F[_]: MonadThrow](owedAmountMapper: OwedAmountDetailMapper[F])
  def getOwedAmounts(id: ExpenseId): F[List[OwedAmountOut]] =
    for owedAmounts <- owedAmountMapper.listChildren(id)
    yield owedAmounts.toOwedAmountsOuts

extension (owedAmounts: List[OwedAmountDetailRead])
  def toOwedAmountsOuts: List[OwedAmountOut] =
    owedAmounts.map(_.toOwedAmountOut)

extension (owedAmounts: List[OwedAmountOut])
  def toTotalOwed: List[OwedAmountOut] =
    owedAmounts
      .groupMapReduce(o => (o.fromMember, o.toMember))(_.amount)(_ + _)
      .map((fromTo, amount) => OwedAmountOut(fromTo._1, fromTo._2, amount))
      .toList
