package exsplit.expenseList
import exsplit.spec._
import cats.syntax.all._
import cats._
import cats.data._
import cats.effect._
import exsplit.circles._
import exsplit.expenses._
import exsplit.datamapper.circles._
import exsplit.datamapper.expenseList._
import exsplit.datamapper.expenses._
import exsplit.domainmapper._
import exsplit.datamapper.settledTabs._
import skunk.Session

object ExpenseListEntryPoint:
  def fromSession[F[_]: Concurrent: Parallel](
      session: Session[F]
  ): F[ExpenseListService[F]] =
    (
      ExpenseListRepository.fromSession(session),
      CircleMembersRepository.fromSession(session),
      ExpenseRepository.fromSession(session),
      CirclesRepository.fromSession(session),
      OwedAmountRepository.fromSession(session),
      SettledTabRepository.fromSession(session)
    ).mapN(ExpenseListServiceImpl(_, _, _, _, _, _))

case class ExpenseListServiceImpl[F[_]: MonadThrow: Parallel](
    expenseListRepo: ExpenseListRepository[F],
    circleMembersRepo: CircleMembersRepository[F],
    expenseRepository: ExpenseRepository[F],
    circlesRepo: CirclesRepository[F],
    owedAmountsRepo: OwedAmountRepository[F],
    settledTabRepository: SettledTabRepository[F]
) extends ExpenseListService[F]:

  def createExpenseList(
      circleId: CircleId,
      name: String
  ): F[CreateExpenseListOutput] =
    circlesRepo.withValidCircle(circleId): circle =>
      expenseListRepo
        .createExpenseList(circleId, name)
        .map(CreateExpenseListOutput(_))

  def getExpenseList(
      expenseListId: ExpenseListId,
      onlyOutstanding: Option[Boolean]
  ): F[GetExpenseListOutput] =
    for
      expenseList <- expenseListRepo.getExpenseListDetail(
        expenseListId,
        expenseRepository,
        owedAmountsRepo,
        circleMembersRepo
      )
      filtered = handleOutStandingFilter(expenseList, onlyOutstanding)
    yield GetExpenseListOutput(filtered)

  private def handleOutStandingFilter(
      expenseList: ExpenseListDetailOut,
      onlyOutstanding: Option[Boolean]
  ): ExpenseListDetailOut =
    def filterOutStanding(expenseList: ExpenseListDetailOut) =
      expenseList.copy(totalOwed = expenseList.totalOwed.filter(_.amount > 0))

    onlyOutstanding match
      case Some(true) =>
        filterOutStanding(expenseList)
      case _ =>
        expenseList

  def getSettledExpenseLists(
      expenseListId: ExpenseListId
  ): F[GetSettledExpenseListsOutput] =
    expenseListRepo.withValidExpenseList(expenseListId): expenseList =>
      val tabs = ???
      val settledTabsOut = SettledTabsOut(tabs)
      GetSettledExpenseListsOutput(settledTabsOut).pure[F]

  def settleExpenseList(
      expenseListId: ExpenseListId,
      fromMemberId: CircleMemberId,
      toMemberId: CircleMemberId,
      amount: Amount
  ): F[Unit] =
    expenseListRepo.withValidExpenseList(expenseListId): expenseList =>
      circleMembersRepo.withValidCircleMember(fromMemberId): fromMember =>
        circleMembersRepo.withValidCircleMember(toMemberId): toMember =>
          settledTabRepository
            .create(expenseListId, fromMemberId, toMemberId, amount)
            .void

  def deleteExpenseList(id: ExpenseListId): F[Unit] =
    expenseListRepo.withValidExpenseList(id): expenseList =>
      expenseListRepo.delete(id)

  def getExpenseLists(circleId: CircleId): F[GetExpenseListsOutput] =
    circlesRepo.withValidCircle(circleId): circle =>
      (expenseListRepo
        .byCircleId(circleId)
        .map(_.toExpenseListOuts))
        .map(lists => GetExpenseListsOutput(ExpenseListsOut(lists)))

  def updateExpenseList(id: ExpenseListId, name: String): F[Unit] =
    expenseListRepo.withValidExpenseList(id): expenseList =>
      val write = ExpenseListWriteMapper(id.value, name)
      expenseListRepo.update(write)
