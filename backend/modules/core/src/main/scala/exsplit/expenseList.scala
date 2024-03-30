package exsplit.expenseList
import exsplit.spec._
import cats.syntax.all._
import cats._
import cats.data._
import exsplit.circles._
import exsplit.datamapper.circles._
object ExpenseListEntryPoint:
  def createService[F[_]: MonadThrow](
      expenseListRepository: ExpenseListRepository[F],
      circlesMembersRepository: CircleMemberMapper[F],
      circlesRepository: CirclesMapper[F]
  ): ExpenseListServiceImpl[F] =
    ExpenseListServiceImpl(
      expenseListRepository,
      circlesMembersRepository,
      circlesRepository
    )

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
    circlesMembersRepository: CircleMemberMapper[F],
    circlesRepository: CirclesMapper[F]
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
      withValidCircleMember(fromMemberId, circlesMembersRepository):
        fromMember =>
          withValidCircleMember(toMemberId, circlesMembersRepository):
            toMember =>
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
