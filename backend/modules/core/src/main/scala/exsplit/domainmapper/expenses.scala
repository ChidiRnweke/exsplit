package exsplit.domainmapper

import exsplit.spec._
import exsplit.datamapper.expenseList._
import cats.effect._
import cats.syntax.all._
import cats._
import exsplit.datamapper.expenses._
import exsplit.datamapper.circles._

extension [F[_]: MonadThrow: Parallel](repo: ExpenseRepository[F])

  /** Retrieves the details of an expense and the corresponding owed amounts.
    * This method needs two repositories, one for the owed amounts and one for
    * the expenses. The owed amounts are retrieved in parallel with the expense
    * details.
    *
    * @param id
    *   The ID of the expense.
    * @param owedAmountsRepo
    *   The repository for retrieving owed amounts.
    * @return
    *   A computation that yields an `ExpenseOut` object containing the expense
    *   details and owed amounts.
    */
  def getExpenseOut(
      id: ExpenseId,
      owedAmountsRepo: OwedAmountRepository[F]
  ): F[ExpenseOut] =
    (
      repo.getDetail(id).rethrow,
      owedAmountsRepo.getOwedAmounts(id)
    ).parMapN: (expense, owedAmounts) =>
      val memberId = CircleMemberId(expense.paidBy)
      val paidBy = CircleMemberOut(expense.paidBy, expense.paidByName)
      ExpenseOut(
        expense.id,
        paidBy,
        expense.description,
        expense.price,
        expense.date,
        owedAmounts
      )

  /** Retrieves a list of expense details for a given expense list ID. Multiple
    * repositories are needed to retrieve the expense details and the owed
    * amounts in parallel.
    *
    * @param id
    *   The ID of the expense list.
    * @param circleMemberRepo
    *   The repository for retrieving circle members.
    * @param owedAmountsRepo
    *   The repository for retrieving owed amounts.
    * @return
    *   A list of expense details.
    */
  def listExpenseOut(
      id: ExpenseListId,
      circleMemberRepo: CircleMembersRepository[F],
      owedAmountsRepo: OwedAmountRepository[F]
  ): F[List[ExpenseOut]] =
    for
      expenses <- repo.fromExpenseList(id)
      expenseOuts <- expenses.parTraverse: expense =>
        getExpenseOut(
          ExpenseId(expense.id),
          owedAmountsRepo: OwedAmountRepository[F]
        )
    yield expenseOuts

extension (owedAmount: OwedAmountDetailRead)
  /** Converts the `owedAmount` object to an `OwedAmountOut` object. The former
    * is the representation of an owed amount in the database, while the latter
    * is the representation of an owed amount in the application.
    *
    * @return
    *   The converted `OwedAmountOut` object.
    */
  def toOwedAmountOut: OwedAmountOut =
    val from =
      CircleMemberOut(owedAmount.fromMember, owedAmount.fromMemberName)
    val to =
      CircleMemberOut(owedAmount.toMember, owedAmount.toMemberName)
    OwedAmountOut(fromMember = from, toMember = to, owedAmount.amount)
extension (owedAmounts: List[OwedAmountDetailRead])
  /** Converts a list of `OwedAmountDetailRead` objects to a list of
    * `OwedAmountOut` objects. The former is the representation of owed amounts
    * in the database, while the latter is the representation of owed amounts in
    * the application.
    *
    * @return
    *   The converted list of `OwedAmountOut` objects.
    */
  def toOwedAmountsOuts: List[OwedAmountOut] =
    owedAmounts.map(_.toOwedAmountOut)

extension (owedAmounts: List[OwedAmountOut])
  /** Converts a list of `OwedAmountOut` objects to a list of `OwedAmountOut`
    * objects where the amounts are summed up for each pair of members. This
    * method is used to calculate the total amount owed between each pair of
    * members.
    *
    * @return
    *   The converted list of `OwedAmountOut` objects.
    */
  def toTotalOwed: List[OwedAmountOut] =
    owedAmounts
      .groupMapReduce(o => (o.fromMember, o.toMember))(_.amount)(_ + _)
      .map((fromTo, amount) => OwedAmountOut(fromTo._1, fromTo._2, amount))
      .toList

extension [F[_]: MonadThrow](owedAmountMapper: OwedAmountRepository[F])
  /** Retrieves the owed amounts for a given expense ID. This method immediately
    * returns a list of `OwedAmountOut` objects, the representation of owed
    * amounts in the application.
    *
    * @param id
    *   The ID of the expense.
    * @return
    *   A list of `OwedAmountOut` objects wrapped in the effect type `F`.
    */
  def getOwedAmounts(id: ExpenseId): F[List[OwedAmountOut]] =
    owedAmountMapper.detailFromExpense(id).map(_.toOwedAmountsOuts)
