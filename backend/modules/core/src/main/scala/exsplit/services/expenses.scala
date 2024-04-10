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
      userInfo: F[Email],
      session: Session[F]
  ): F[ExpenseServiceImpl[F]] =
    for
      expenseListRepo <- ExpenseListRepository.fromSession(session)
      circleMembersRepo <- CircleMembersRepository.fromSession(session)
      owedAmountsRepo <- OwedAmountRepository.fromSession(session)
      expenseRepo <- ExpenseRepository.fromSession(session)
      expenseDomainMapper = ExpenseDomainMapper(
        circleMembersRepo,
        owedAmountsRepo,
        expenseRepo
      )
      expenseListDomainMapper = ExpenseListDomainMapper(
        expenseListRepo,
        expenseDomainMapper,
        owedAmountsRepo
      )
    yield ExpenseServiceImpl(
      userInfo,
      expenseDomainMapper,
      expenseListDomainMapper,
      circleMembersRepo,
      owedAmountsRepo
    )

def withValidExpense[F[_]: MonadThrow, A](
    expenseId: ExpenseId,
    expenseRepository: ExpenseDomainMapper[F]
)(action: ExpenseOut => F[A]): F[A] =
  for
    expense <- expenseRepository.getExpenseOut(expenseId)
    result <- action(expense)
  yield result

case class ExpenseServiceImpl[F[_]: MonadThrow](
    userInfo: F[Email],
    expenseRepo: ExpenseDomainMapper[F],
    expenseListRepo: ExpenseListDomainMapper[F],
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
        expenseRead <- expenseRepo.repo.main.create(createExpenseInput)
        member <- membersRepo.main.getCircleMemberOut(paidBy)
        expenseId = ExpenseId(expenseRead.id)
        owedAmounts <- owedAmountRepo.detail.getOwedAmounts(expenseId)
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
    withValidExpense(id, expenseRepo): expense =>
      GetExpenseOutput(expense).pure[F]

  def deleteExpense(id: ExpenseId): F[Unit] =
    expenseRepo.repo.main.delete(id)

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

    expenseRepo.repo.main.update(expenseWriter)
