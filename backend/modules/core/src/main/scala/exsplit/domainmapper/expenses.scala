package exsplit.domainmapper

import exsplit.spec._
import exsplit.datamapper.expenseList._
import cats.effect._
import cats.syntax.all._
import cats._
import exsplit.datamapper.expenses._
import exsplit.datamapper.circles._

extension [F[_]: MonadThrow](repo: ExpenseRepository[F])
  def getExpenseOut(
      id: ExpenseId,
      owedAmountsRepo: OwedAmountRepository[F]
  ): F[ExpenseOut] =
    for
      expense <- repo.getDetail(id).rethrow
      memberId = CircleMemberId(expense.paidBy)
      paidBy = CircleMemberOut(expense.paidBy, expense.paidByName)
      owedAmounts <- owedAmountsRepo.getOwedAmounts(id)
    yield ExpenseOut(
      expense.id,
      paidBy,
      expense.description,
      expense.price,
      expense.date,
      owedAmounts
    )

  def listExpenseOut(
      id: ExpenseListId,
      circleMemberRepo: CircleMembersRepository[F],
      owedAmountsRepo: OwedAmountRepository[F]
  ): F[List[ExpenseOut]] =
    for
      expenses <- repo.fromExpenseList(id)
      expenseOuts <- expenses.traverse: expense =>
        getExpenseOut(
          ExpenseId(expense.id),
          owedAmountsRepo: OwedAmountRepository[F]
        )
    yield expenseOuts

  def withValidExpense[A](
      expenseId: ExpenseId,
      owedAmountsRepo: OwedAmountRepository[F]
  )(action: ExpenseOut => F[A]): F[A] =
    for
      expense <- repo.getExpenseOut(expenseId, owedAmountsRepo)
      result <- action(expense)
    yield result

extension (owedAmount: OwedAmountDetailRead)
  def toOwedAmountOut: OwedAmountOut =
    val from =
      CircleMemberOut(owedAmount.fromMember, owedAmount.fromMemberName)
    val to =
      CircleMemberOut(owedAmount.toMember, owedAmount.toMemberName)
    OwedAmountOut(fromMember = from, toMember = to, owedAmount.amount)
extension (owedAmounts: List[OwedAmountDetailRead])
  def toOwedAmountsOuts: List[OwedAmountOut] =
    owedAmounts.map(_.toOwedAmountOut)

extension (owedAmounts: List[OwedAmountOut])
  def toTotalOwed: List[OwedAmountOut] =
    owedAmounts
      .groupMapReduce(o => (o.fromMember, o.toMember))(_.amount)(_ + _)
      .map((fromTo, amount) => OwedAmountOut(fromTo._1, fromTo._2, amount))
      .toList

extension [F[_]: MonadThrow](owedAmountMapper: OwedAmountRepository[F])
  def getOwedAmounts(id: ExpenseId): F[List[OwedAmountOut]] =
    for owedAmounts <- owedAmountMapper.detailFromExpense(id)
    yield owedAmounts.toOwedAmountsOuts
