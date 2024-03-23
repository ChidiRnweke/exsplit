package exsplit.expenseList
import exsplit.spec._
import cats.syntax.all._
import cats._
import cats.data._
import exsplit.circles._
object ExpenseListEntryPoint:
  def createService[F[_]: MonadThrow](
      expenseListRepository: ExpenseListRepository[F],
      circlesRepository: CirclesRepository[F]
  ): ExpenseListServiceImpl[F] =
    ExpenseListServiceImpl(expenseListRepository, circlesRepository)

def withValidExpenseList[F[_]: MonadThrow, A](
    expenseListId: ExpenseListId,
    expenseListRepository: ExpenseListRepository[F]
)(action: ExpenseListOut => F[A]): F[A] =
  for
    expenseList <- expenseListRepository.getExpenseList(expenseListId).rethrow
    result <- action(expenseList)
  yield result

def withValidExpenseListDetail[F[_]: MonadThrow, A](
    expenseListId: ExpenseListId,
    expenseListRepository: ExpenseListRepository[F]
)(action: ExpenseListDetailOut => F[A]): F[A] =
  for
    expenseList <- expenseListRepository
      .getExpenseListDetail(expenseListId)
      .rethrow
    result <- action(expenseList)
  yield result

case class ExpenseListServiceImpl[F[_]: MonadThrow](
    expenseListRepository: ExpenseListRepository[F],
    circlesRepository: CirclesRepository[F]
) extends ExpenseListService[F]:

  def createExpenseList(
      circleId: CircleId,
      name: String
  ): F[CreateExpenseListOutput] =
    withValidCircle(circleId, circlesRepository): circle =>
      expenseListRepository
        .createExpenseList(circle, name)
        .map(CreateExpenseListOutput(_))

  def getExpenseList(
      expenseListId: ExpenseListId,
      onlyOutstanding: Option[Boolean]
  ): F[GetExpenseListOutput] =
    withValidExpenseListDetail(expenseListId, expenseListRepository):
      expenseList =>
        val filteredExpenseList = handleOutStandingFilter(
          expenseList,
          onlyOutstanding
        )
        GetExpenseListOutput(filteredExpenseList).pure[F]

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
    withValidExpenseList(expenseListId, expenseListRepository): expenseList =>
      val tabs = expenseListRepository.getAllTabs(expenseList)
      val settledTabsOut = SettledTabsOut(tabs)
      GetSettledExpenseListsOutput(settledTabsOut).pure[F]
  def settleExpenseList(
      expenseListId: ExpenseListId,
      fromMemberId: CircleMemberId,
      toMemberId: CircleMemberId,
      amount: Amount
  ): F[Unit] =
    withValidExpenseList(expenseListId, expenseListRepository): expenseList =>
      withValidCircleMember(fromMemberId, circlesRepository): fromMember =>
        withValidCircleMember(toMemberId, circlesRepository): toMember =>
          expenseListRepository
            .settleExpenseList(expenseList, fromMember, toMember, amount)

  def deleteExpenseList(id: ExpenseListId): F[Unit] =
    withValidExpenseList(id, expenseListRepository): expenseList =>
      expenseListRepository.deleteExpenseList(expenseList)

  def getExpenseLists(circleId: CircleId): F[GetExpenseListsOutput] =
    withValidCircle(circleId, circlesRepository): circle =>
      for
        expenseLists <- expenseListRepository.getExpenseLists(circle)
        output = ExpenseListsOut(expenseLists)
      yield GetExpenseListsOutput(output)

  def updateExpenseList(id: ExpenseListId, name: String): F[Unit] =
    withValidExpenseList(id, expenseListRepository): expenseList =>
      expenseListRepository.updateExpenseList(expenseList, name)

trait ExpenseListRepository[F[_]]:

  def getExpenseListDetail(
      expenseListId: ExpenseListId
  ): F[Either[NotFoundError, ExpenseListDetailOut]]

  def getExpenseList(
      expenseListId: ExpenseListId
  ): F[Either[NotFoundError, ExpenseListOut]]

  def createExpenseList(
      circle: CircleOut,
      name: String
  ): F[ExpenseListOut]

  def deleteExpenseList(expenseList: ExpenseListOut): F[Unit]
  def getExpenseLists(circleId: CircleOut): F[List[ExpenseListOut]]
  def updateExpenseList(expenseList: ExpenseListOut, name: String): F[Unit]
  def getAllTabs(expenseList: ExpenseListOut): List[SettledTabOut]
  def settleExpenseList(
      expenseList: ExpenseListOut,
      fromMember: CircleMemberOut,
      toMember: CircleMemberOut,
      amount: Amount
  ): F[Unit]
