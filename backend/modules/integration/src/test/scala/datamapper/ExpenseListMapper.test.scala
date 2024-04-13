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
import exsplit.database.AppSessionPool

class ExpenseListMapperSuite extends DatabaseSuite:
  def createTestCircle(session: AppSessionPool[IO]): IO[CircleReadMapper] =
    val input = CreateCircleInput(UserId("1"), "User", "Test Circle")
    val circlesRepo = CirclesRepository.fromSession(session)
    for result <- circlesRepo.create(input)
    yield result

  def createExpenseList(
      expenseListRepo: ExpenseListRepository[IO],
      session: AppSessionPool[IO]
  ): IO[ExpenseListReadMapper] =
    for
      circle <- createTestCircle(session)
      createInput = CreateExpenseListInput(
        CircleId(circle.id),
        "Test Expense List"
      )
      expenseList <- expenseListRepo.create(createInput)
    yield expenseList

  test("You should be able to create an expense list in the database"):
    val session = sessionPool()
    val expenseListRepo = ExpenseListRepository.fromSession(session)

    for
      circle <- createTestCircle(session)
      result <- createExpenseList(expenseListRepo, session).attempt
    yield assertEquals(result.isRight, true)

  test("You should be able to create an expense list and then read it"):
    val session = sessionPool()
    val expenseListRepo = ExpenseListRepository.fromSession(session)

    for
      expected <- createExpenseList(expenseListRepo, session)
      obtained <- expenseListRepo.get(ExpenseListId(expected.id)).rethrow
    yield assertEquals(obtained, expected)

  test(
    "You should be able not be able to create an expense list for a circle that doesn't exist"
  ):
    val session = sessionPool()
    val expenseListRepo = ExpenseListRepository.fromSession(session)
    val createInput = CreateExpenseListInput(
      CircleId("doesn't exist"),
      "Test Expense List"
    )
    for obtained <- expenseListRepo.create(createInput).attempt
    yield assert(obtained.isLeft)

  test("You should be able to create an expense list and then delete it"):
    val session = sessionPool()
    val expenseListRepo = ExpenseListRepository.fromSession(session)

    for
      expected <- createExpenseList(expenseListRepo, session)
      success <- expenseListRepo
        .delete(ExpenseListId(expected.id))
        .attempt
    yield assert(success.isRight)

  test("You should be able to update an expense list"):
    val session = sessionPool()
    val expected = "new name"
    val expenseListRepo = ExpenseListRepository.fromSession(session)
    for
      created <- createExpenseList(expenseListRepo, session)
      updateInput = ExpenseListWriteMapper(created.id, expected)
      _ <- expenseListRepo.update(updateInput)
      obtained <- expenseListRepo.get(ExpenseListId(created.id)).rethrow
    yield assertEquals(obtained.name, expected)

  test("You should be able to get all expense lists belonging to a circle"):
    val session = sessionPool()
    val expected = 3
    val expenseListRepo = ExpenseListRepository.fromSession(session)
    for
      testCircle <- createTestCircle(session)
      createInput = CreateExpenseListInput(CircleId(testCircle.id), "Test")
      _ <- expenseListRepo.create(createInput)
      _ <- expenseListRepo.create(createInput)
      _ <- expenseListRepo.create(createInput)
      expenseLists <- expenseListRepo.byCircleId(CircleId(testCircle.id))
    yield assertEquals(expenseLists.length, expected)
