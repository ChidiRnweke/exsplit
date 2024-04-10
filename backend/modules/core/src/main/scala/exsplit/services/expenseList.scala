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
  def fromSession[F[_]: Concurrent](
      userInfo: F[Email],
      session: Session[F]
  ): F[ExpenseListService[F]] =
    for
      expenseListRepo <- ExpenseListRepository.fromSession(session)
      circleMembersRepo <- CircleMembersRepository.fromSession(session)
      circlesRepo <- CirclesRepository.fromSession(session)
      settledTabRepository <- SettledTabRepository.fromSession(session)
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
    yield ExpenseListServiceImpl(
      userInfo,
      expenseListDomainMapper,
      circleMembersRepo,
      circlesRepo,
      settledTabRepository
    )

case class ExpenseListServiceImpl[F[_]: MonadThrow](
    userInfo: F[Email],
    expenseListRepo: ExpenseListDomainMapper[F],
    circleMembersRepo: CircleMembersRepository[F],
    circlesRepo: CirclesRepository[F],
    settledTabRepository: SettledTabRepository[F]
) extends ExpenseListService[F]:

  def createExpenseList(
      circleId: CircleId,
      name: String
  ): F[CreateExpenseListOutput] =
    circlesRepo.withValidCircle(circleId): circle =>
      expenseListRepo.repo.main
        .createExpenseList(circleId, name)
        .map(CreateExpenseListOutput(_))

  def getExpenseList(
      expenseListId: ExpenseListId,
      onlyOutstanding: Option[Boolean]
  ): F[GetExpenseListOutput] =
    for
      expenseList <- expenseListRepo.getExpenseListDetail(expenseListId)
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
          settledTabRepository.main
            .create(expenseListId, fromMemberId, toMemberId, amount)
            .void

  def deleteExpenseList(id: ExpenseListId): F[Unit] =
    expenseListRepo.withValidExpenseList(id): expenseList =>
      expenseListRepo.repo.main.delete(id)

  def getExpenseLists(circleId: CircleId): F[GetExpenseListsOutput] =
    circlesRepo.withValidCircle(circleId): circle =>
      for
        read <- expenseListRepo.repo.byCircle.listChildren(circleId)
        expenseLists = read.toExpenseListOuts
      yield GetExpenseListsOutput(ExpenseListsOut(expenseLists))

  def updateExpenseList(id: ExpenseListId, name: String): F[Unit] =
    expenseListRepo.withValidExpenseList(id): expenseList =>
      val write = ExpenseListWriteMapper(id.value, name)
      expenseListRepo.repo.main.update(write)
