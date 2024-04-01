package exsplit.expenseList
import exsplit.spec._
import cats.syntax.all._
import cats._
import cats.data._
import exsplit.circles._
import exsplit.expenses._
import exsplit.datamapper.circles._
import exsplit.datamapper.expenseList._
import exsplit.datamapper.expenses._
import exsplit.domainmapper.ExpenseListOps._
import exsplit.domainmapper._
import exsplit.domainmapper.SettledTabsOps._
import exsplit.datamapper.settledTabs._

object ExpenseListEntryPoint:
  def createService[F[_]: MonadThrow](
      expenseListDomainMapper: ExpenseListDomainMapper[F],
      circleMembersRepo: CircleMembersRepository[F],
      circlesRepo: CirclesRepository[F],
      settledTabRepository: SettledTabRepository[F]
  ): ExpenseListServiceImpl[F] =
    ExpenseListServiceImpl(
      expenseListDomainMapper,
      circleMembersRepo,
      circlesRepo,
      settledTabRepository
    )

def withValidExpenseList[F[_]: MonadThrow, A](
    expenseListId: ExpenseListId,
    expenseListMapper: ExpenseListDomainMapper[F]
)(action: ExpenseListDetailOut => F[A]): F[A] =
  for
    expenseList <- expenseListMapper.getExpenseListDetail(expenseListId)
    result <- action(expenseList)
  yield result

case class ExpenseListServiceImpl[F[_]: MonadThrow](
    expenseListRepo: ExpenseListDomainMapper[F],
    circleMembersRepo: CircleMembersRepository[F],
    circlesRepo: CirclesRepository[F],
    settledTabRepository: SettledTabRepository[F]
) extends ExpenseListService[F]:

  def createExpenseList(
      circleId: CircleId,
      name: String
  ): F[CreateExpenseListOutput] =
    withValidCircle(circleId, circlesRepo): circle =>
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
    withValidExpenseList(expenseListId, expenseListRepo): expenseList =>
      val tabs = ???
      val settledTabsOut = SettledTabsOut(tabs)
      GetSettledExpenseListsOutput(settledTabsOut).pure[F]

  def settleExpenseList(
      expenseListId: ExpenseListId,
      fromMemberId: CircleMemberId,
      toMemberId: CircleMemberId,
      amount: Amount
  ): F[Unit] =
    withValidExpenseList(expenseListId, expenseListRepo): expenseList =>
      withValidCircleMember(fromMemberId, circleMembersRepo): fromMember =>
        withValidCircleMember(toMemberId, circleMembersRepo): toMember =>
          settledTabRepository.main
            .create(expenseListId, fromMemberId, toMemberId, amount)
            .void

  def deleteExpenseList(id: ExpenseListId): F[Unit] =
    withValidExpenseList(id, expenseListRepo): expenseList =>
      expenseListRepo.repo.main.delete(id)

  def getExpenseLists(circleId: CircleId): F[GetExpenseListsOutput] =
    withValidCircle(circleId, circlesRepo): circle =>
      for
        read <- expenseListRepo.repo.byCircle.listChildren(circleId)
        expenseLists = read.toExpenseListOuts
      yield GetExpenseListsOutput(ExpenseListsOut(expenseLists))

  def updateExpenseList(id: ExpenseListId, name: String): F[Unit] =
    withValidExpenseList(id, expenseListRepo): expenseList =>
      val write = ExpenseListWriteMapper(id.value, name)
      expenseListRepo.repo.main.update(write)
