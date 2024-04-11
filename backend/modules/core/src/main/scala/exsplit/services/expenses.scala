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
  def fromSession[F[_]: Concurrent](
      session: Session[F]
  ): F[ExpenseServiceImpl[F]] =
    for
      expenseListRepo <- ExpenseListRepository.fromSession(session)
      circleMembersRepo <- CircleMembersRepository.fromSession(session)
      owedAmountsRepo <- OwedAmountRepository.fromSession(session)
      expenseRepo <- ExpenseRepository.fromSession(session)
    yield ExpenseServiceImpl(
      expenseRepo,
      expenseListRepo,
      circleMembersRepo,
      owedAmountsRepo
    )

case class ExpenseServiceImpl[F[_]: MonadThrow](
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
    expenseListRepo.withValidExpenseList(expenseListId): expenseList =>
      val createExpenseInput = CreateExpenseInput(
        expenseListId,
        paidBy,
        description,
        price,
        date,
        owedToPayer
      )
      for
        expenseRead <- expenseRepo.create(createExpenseInput)
        member <- membersRepo.getCircleMemberOut(paidBy)
        expenseId = ExpenseId(expenseRead.id)
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
