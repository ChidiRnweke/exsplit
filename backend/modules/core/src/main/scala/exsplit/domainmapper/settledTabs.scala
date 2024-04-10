package exsplit.domainmapper

import exsplit.spec._
import cats.effect._
import cats.syntax.all._
import cats._
import exsplit.datamapper.settledTabs._

extension [F[_]: Functor](mapper: SettledTabMapper[F])
  def create(
      expenseListId: ExpenseListId,
      fromMember: CircleMemberId,
      toMember: CircleMemberId,
      amount: Amount
  ): F[SettledTabReadMapper] =
    val input =
      SettleExpenseListInput(expenseListId, fromMember, toMember, amount)
    for settledTab <- mapper.create(input)
    yield settledTab
