package exsplit.expenses

import exsplit.spec._
import cats.syntax.all._
import cats.data._
import cats._
import cats.effect._
import smithy4s.Timestamp
import exsplit.expenseList._
import exsplit.datamapper.expenses._
import exsplit.datamapper.circles._
import exsplit.datamapper.expenseList._
import exsplit.domainmapper._
import java.time.LocalDate
import skunk.Session
import exsplit.database.AppSessionPool

object ExpensesEntryPoint:
  def fromSession[F[_]: Concurrent: Parallel](
      session: AppSessionPool[F]
  ): ExpenseService[F] =
    val expenseRepo = ExpenseRepository.fromSession(session)
    val expenseListRepo = ExpenseListRepository.fromSession(session)
    val membersRepo = CircleMembersRepository.fromSession(session)
    val owedAmountRepo = OwedAmountRepository.fromSession(session)
    ExpenseServiceImpl(
      expenseRepo,
      expenseListRepo,
      membersRepo,
      owedAmountRepo
    )

case class ExpenseServiceImpl[F[_]: MonadThrow: Parallel](
    expenseRepo: ExpenseRepository[F],
    expenseListRepo: ExpenseListRepository[F],
    membersRepo: CircleMembersRepository[F],
    owedAmountRepo: OwedAmountRepository[F]
) extends ExpenseService[F]:

  def createExpense(
      expenseListId: ExpenseListId,
      paidBy: CircleMemberId,
      description: String,
      price: Amount,
      date: Timestamp,
      owedToPayer: List[OwedAmount]
  ): F[CreateExpenseOutput] =
    val input = CreateExpenseInput(
      expenseListId,
      paidBy,
      description,
      price,
      date,
      owedToPayer
    )
    def createExpenseHelper(
        exp: ExpenseReadMapper,
        member: CircleMemberOut
    ): F[CreateExpenseOutput] =
      val expenseId = ExpenseId(exp.id)
      val amounts = owedToPayer.map: o =>
        CreateOwedAmountInput(expenseId, paidBy, o.circleMemberId, o.amount)

      for
        _ <- amounts.parTraverse(owedAmountRepo.create)
        owed <- owedAmountRepo.getOwedAmounts(expenseId)
        out = ExpenseOut(exp.id, member, exp.description, exp.price, date, owed)
      yield CreateExpenseOutput(out)

    expenseListRepo.withValidExpenseList(expenseListId) *>
      (expenseRepo.create(input), membersRepo.getCircleMemberOut(paidBy))
        .parFlatMapN(createExpenseHelper)

  def getExpense(id: ExpenseId): F[GetExpenseOutput] =
    expenseRepo
      .getExpenseOut(id, owedAmountRepo)
      .map(GetExpenseOutput(_))

  def deleteExpense(id: ExpenseId): F[Unit] =
    expenseRepo.delete(id)

  def updateExpense(
      id: ExpenseId,
      paidBy: Option[CircleMemberId],
      description: Option[String],
      price: Option[Amount],
      date: Option[Timestamp],
      owedToPayer: Option[List[OwedAmount]]
  ): F[Unit] =
    val expenseWriter = ExpenseWriteMapper(
      id.value,
      paidBy.map(_.value),
      description,
      price.map(_.value),
      date
    )

    expenseRepo.update(expenseWriter)
