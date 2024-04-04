import cats.effect._
import cats.syntax.all._
import cats.data._
import cats._
import exsplit.config._
import exsplit.integration._
import exsplit.spec._
import exsplit.datamapper.circles._
import exsplit.datamapper.user._
import exsplit.datamapper.expenseList._
import skunk.Session

class ExpenseListMapperSuite extends DatabaseSuite:
  def createTestCircle(session: Session[IO]): IO[CircleReadMapper] =
    val input = CreateCircleInput(UserId("1"), "User", "Test Circle")
    for
      circlesRepo <- CirclesRepository.fromSession(session)
      result <- circlesRepo.main.create(input)
    yield result

  def createExpenseList(
      expenseListRepo: ExpenseListRepository[IO],
      session: Session[IO]
  ): IO[ExpenseListReadMapper] =
    for
      circle <- createTestCircle(session)
      createInput = CreateExpenseListInput(
        CircleId(circle.id),
        "Test Expense List"
      )
      expenseList <- expenseListRepo.main.create(createInput)
    yield expenseList

  test("You should be able to create an expense list in the database"):
    val session = sessionPool()
    session.use(session =>
      for
        expenseListRepo <- ExpenseListRepository.fromSession(session)
        circle <- createTestCircle(session)
        result <- createExpenseList(expenseListRepo, session).attempt
      yield assertEquals(result.isRight, true)
    )

  test("You should be able to create an expense list and then read it"):
    val session = sessionPool()
    session.use: session =>
      for
        expenseListRepo <- ExpenseListRepository.fromSession(session)
        expected <- createExpenseList(expenseListRepo, session)
        obtained <- expenseListRepo.main.get(ExpenseListId(expected.id)).rethrow
      yield assertEquals(obtained, expected)

  test(
    "You should be able not be able to create an expense list for a circle that doesn't exist"
  ):
    val session = sessionPool()
    session.use: session =>
      for
        expenseListRepo <- ExpenseListRepository.fromSession(session)
        createInput = CreateExpenseListInput(
          CircleId("doesn't exist"),
          "Test Expense List"
        )
        obtained <- expenseListRepo.main.create(createInput).attempt
      yield assert(obtained.isLeft)

  test("You should be able to create an expense list and then delete it"):
    val session = sessionPool()
    session.use: session =>
      for
        expenseListRepo <- ExpenseListRepository.fromSession(session)
        expected <- createExpenseList(expenseListRepo, session)
        success <- expenseListRepo.main
          .delete(ExpenseListId(expected.id))
          .attempt
      yield assert(success.isRight)

  test("You should be able to update an expense list"):
    val session = sessionPool()
    val expected = "new name"
    session.use: session =>
      for
        expenseListRepo <- ExpenseListRepository.fromSession(session)
        created <- createExpenseList(expenseListRepo, session)
        updateInput = ExpenseListWriteMapper(created.id, expected)
        _ <- expenseListRepo.main.update(updateInput)
        obtained <- expenseListRepo.main.get(ExpenseListId(created.id)).rethrow
      yield assertEquals(obtained.name, expected)

  test("You should be able to get all expense lists belonging to a circle"):
    val session = sessionPool()
    val expected = 3
    session.use: session =>
      for
        expenseListRepo <- ExpenseListRepository.fromSession(session)
        testCircle <- createTestCircle(session)
        createInput = CreateExpenseListInput(CircleId(testCircle.id), "Test")
        _ <- expenseListRepo.main.create(createInput)
        _ <- expenseListRepo.main.create(createInput)
        _ <- expenseListRepo.main.create(createInput)
        expenseLists <- expenseListRepo.byCircle.listChildren(
          CircleId(testCircle.id)
        )
      yield assertEquals(expenseLists.length, expected)
