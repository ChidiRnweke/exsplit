package exsplit.domainmapper

import exsplit.spec._
import cats.effect._
import cats.syntax.all._
import cats._
import exsplit.datamapper.settledTabs._
import exsplit.datamapper.circles.CircleMembersRepository

extension [F[_]: Functor](mapper: SettledTabMapper[F])
  /** Creates a settled tab record in the database. This method allows you to
    * create a settled tab record without creating an input object.
    *
    * @param expenseListId
    *   The ID of the expense list.
    * @param fromMember
    *   The ID of the member who paid the expense.
    * @param toMember
    *   The ID of the member who received the expense.
    * @param amount
    *   The amount of the settled tab.
    * @return
    *   A `SettledTabReadMapper` representing the created settled tab record.
    */
  def create(
      expenseListId: ExpenseListId,
      fromMember: CircleMemberId,
      toMember: CircleMemberId,
      amount: Amount
  ): F[SettledTabReadMapper] =
    val input =
      SettleExpenseListInput(expenseListId, fromMember, toMember, amount)
    mapper.create(input)
extension [F[_]: MonadThrow](repo: SettledTabRepository[F])

  def getSettledTabs(
      expenseListId: ExpenseListId,
      circleMembersRepo: CircleMembersRepository[F]
  ): F[List[SettledTabOut]] =
    for
      tab <- repo.fromExpenseList(expenseListId)
      tabs <- tab.traverse(tab => getSettledTabOut(tab, circleMembersRepo))
    yield tabs

  def getSettledTabOut(
      tab: SettledTabReadMapper,
      circleMembersRepo: CircleMembersRepository[F]
  ): F[SettledTabOut] =
    val fromId = CircleMemberId(tab.fromMember)
    val toId = CircleMemberId(tab.toMember)
    for
      fromMember <- circleMembersRepo.getCircleMemberOut(fromId)
      toMember <- circleMembersRepo.getCircleMemberOut(toId)
    yield SettledTabOut(
      tab.id,
      tab.settledAt.toString(),
      fromMember,
      toMember,
      tab.amount
    )
