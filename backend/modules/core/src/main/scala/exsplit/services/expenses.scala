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

object ExpensesEntryPoint:
  def fromSession[F[_]: Concurrent: Parallel](
      session: Session[F]
  ): F[ExpenseService[F]] =
    (
      ExpenseRepository.fromSession(session),
      ExpenseListRepository.fromSession(session),
      CircleMembersRepository.fromSession(session),
      OwedAmountRepository.fromSession(session)
    ).mapN(ExpenseServiceImpl(_, _, _, _))

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
    def createExpenseHelper(
        expenseRead: ExpenseReadMapper,
        member: CircleMemberOut
    ): F[CreateExpenseOutput] =
      val expenseId = ExpenseId(expenseRead.id)
      for
        owedAmounts <- owedAmountRepo.getOwedAmounts(expenseId)
        out = ExpenseOut(
          expenseRead.id,
          member,
          expenseRead.description,
          expenseRead.price,
          date,
          owedAmounts
        )
      yield CreateExpenseOutput(out)

    val create = CreateExpenseInput(
      expenseListId,
      paidBy,
      description,
      price,
      date,
      owedToPayer
    )

    expenseListRepo.withValidExpenseList(expenseListId) *>
      (expenseRepo.create(create), membersRepo.getCircleMemberOut(paidBy))
        .parFlatMapN(createExpenseHelper)

  def getExpense(id: ExpenseId): F[GetExpenseOutput] =
    expenseRepo.withValidExpense(id, owedAmountRepo): expense =>
      GetExpenseOutput(expense).pure[F]

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
