package exsplit.domainmapper

import exsplit.spec._
import cats.effect._
import cats.syntax.all._
import cats._
import exsplit.datamapper.settledTabs._

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
