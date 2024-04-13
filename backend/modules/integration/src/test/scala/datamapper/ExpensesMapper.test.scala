import cats.effect._
import cats.syntax.all._
import cats.effect.std.UUIDGen
import cats.data._
import cats._
import exsplit.config._
import exsplit.integration._
import exsplit.spec._
import exsplit.datamapper.circles._
import exsplit.spec.UserId
import exsplit.datamapper.user._
import skunk.Session
import exsplit.datamapper.expenseList._
import exsplit.datamapper.expenses._
import java.util.Date
import smithy4s.Timestamp
import exsplit.database.AppSessionPool

class ExpenseMapperSuite extends DatabaseSuite:
  /** Helper function to setup the database with a user, circle, and expense
    * list.
    * @param session
    *   The session to use for the database connection.
    */
  def setup(
      session: AppSessionPool[IO]
  ): IO[(ExpenseListId, CircleMemberId, CircleMemberId)] =
    val circlesRepo = CirclesRepository.fromSession(session)
    val circleMembersRepo = CircleMembersRepository.fromSession(session)
    val expenseListRepo = ExpenseListRepository.fromSession(session)
    for
      userId <- createUser(session)
      userId2 <- createUser(session)
      input = CreateCircleInput(userId, "User", "Test Circle")

      circle <- circlesRepo.create(input)
      memberInput = AddUserToCircleInput(userId, "User", CircleId(circle.id))
      memberInput2 = AddUserToCircleInput(userId2, "User2", CircleId(circle.id))
      member <- circleMembersRepo.create(memberInput)
      member2 <- circleMembersRepo.create(memberInput2)
      expListInput = CreateExpenseListInput(CircleId(circle.id), "Test")

      expenseList <- expenseListRepo.create(expListInput)
    yield (
      ExpenseListId(expenseList.id),
      CircleMemberId(member.id),
      CircleMemberId(member2.id)
    )

  /** Helper function to create a user in the database.
    * @param session
    *   The session to use for the database connection.
    */
  def createUser(session: AppSessionPool[IO]): IO[UserId] =
    val uuid = UUIDGen[IO].randomUUID
    val userRepo = UserMapper.fromSession(session)
    for
      nextId <- uuid
      _ <- userRepo.createUser(nextId, Email("test@test.com"), "password")
    yield UserId(nextId.toString())

  /** Helper function to create 3 expenses.
    * @param session
    *   The session to use for the database connection.
    */
  def createExpenses(session: AppSessionPool[IO]): IO[ExpenseReadMapper] =
    val amount = Amount(10)
    val expenseRepo = ExpenseRepository.fromSession(session)
    for

      output <- setup(session)
      now <- IO.realTimeInstant
      today = Date.from(now)
      (expenseListId, paidBy, paidFor) = output
      owedAmounts = List(OwedAmount(paidFor, amount))
      expenseInput = CreateExpenseInput(
        expenseListId,
        paidBy,
        "test",
        amount,
        Timestamp(now.getEpochSecond(), 0),
        owedAmounts
      )

      _ <- expenseRepo.create(expenseInput)
      _ <- expenseRepo.create(expenseInput)
      result <- expenseRepo.create(expenseInput)
    yield result

  test("You should be able to create an expense in the database"):
    val session = sessionPool()
    for result <- createExpenses(session).attempt
    yield assertEquals(result.isRight, true)

  test("You should be able to get an expense from the database"):
    val session = sessionPool()
    val expenseRepo = ExpenseRepository.fromSession(session)
    for

      expected <- createExpenses(session)
      obtained <- expenseRepo.get(ExpenseId(expected.id)).rethrow
    yield assertEquals(obtained, expected)

  test("You should be able to update an expense"):
    val session = sessionPool()
    val expenseRepo = ExpenseRepository.fromSession(session)
    for
      result <- createExpenses(session)
      write = ExpenseWriteMapper(
        result.id,
        None,
        Some("updated"),
        Some(50),
        None
      )
      expected = ("updated", 50f)
      _ <- expenseRepo.update(write)
      obtained <- expenseRepo.get(ExpenseId(result.id)).rethrow
    yield assertEquals((obtained.description, obtained.price), expected)

  test("You should be able to delete an expense"):
    val session = sessionPool()
    val expenseRepo = ExpenseRepository.fromSession(session)
    for
      expected <- createExpenses(session)
      _ <- expenseRepo.delete(ExpenseId(expected.id))
      obtained <- expenseRepo.get(ExpenseId(expected.id))
    yield assert(obtained.isLeft)

  test("You should be able to read a detailed expense"):
    val session = sessionPool()
    val expenseRepo = ExpenseRepository.fromSession(session)
    for
      expected <- createExpenses(session)
      detail <- expenseRepo.getDetail(ExpenseId(expected.id)).rethrow
    yield assertEquals(detail.paidByName, "User")

  test("You should be able to read the expenses from a circle member"):
    val session = sessionPool()
    val expenseRepo = ExpenseRepository.fromSession(session)
    for
      last <- createExpenses(session)
      expenses <- expenseRepo.fromCircleMember(
        CircleMemberId(last.paidBy)
      )
    yield assertEquals(expenses.length, 3)

  test("You should be able to read the expenses from a circle member"):
    val session = sessionPool()
    val expenseRepo = ExpenseRepository.fromSession(session)
    for
      last <- createExpenses(session)
      expenses <- expenseRepo.fromExpenseList(
        ExpenseListId(last.expenseListId)
      )
    yield assertEquals(expenses.length, 3)

  test("You should be able to create an owed amount without errors"):
    val session = sessionPool()
    val expenseRepo = ExpenseRepository.fromSession(session)
    val owedAmountsRepo = OwedAmountRepository.fromSession(session)
    for
      last <- createExpenses(session)
      expenseId = ExpenseId(last.id)
      from = CircleMemberId(last.paidBy)
      owed = CreateOwedAmountInput(expenseId, from, from, Amount(10))
      obtained <- owedAmountsRepo.create(owed).attempt
    yield assert(obtained.isRight)

  test("You should be able to create and then read an owed amount"):
    val session = sessionPool()
    val expenseRepo = ExpenseRepository.fromSession(session)
    val owedAmountsRepo = OwedAmountRepository.fromSession(session)
    for
      last <- createExpenses(session)
      expenseId = ExpenseId(last.id)
      from = CircleMemberId(last.paidBy)
      owed = CreateOwedAmountInput(expenseId, from, from, Amount(10))
      expected <- owedAmountsRepo.create(owed)
      key = OwedAmountKey(expenseId, from, from)
      obtained <- owedAmountsRepo.get(key).rethrow
    yield assertEquals(expected, obtained)

  test("You should be able to delete an owned amount"):
    val session = sessionPool()
    val expenseRepo = ExpenseRepository.fromSession(session)
    val owedAmountsRepo = OwedAmountRepository.fromSession(session)
    for
      last <- createExpenses(session)
      expenseId = ExpenseId(last.id)
      from = CircleMemberId(last.paidBy)
      owed = CreateOwedAmountInput(expenseId, from, from, Amount(10))
      key = OwedAmountKey(expenseId, from, from)
      expected <- owedAmountsRepo.create(owed)
      _ <- owedAmountsRepo.delete(key)
      obtained <- owedAmountsRepo.get(key)
    yield assert(obtained.isLeft)

  test("You should be able to update an owed amount"):
    val session = sessionPool()
    val expenseRepo = ExpenseRepository.fromSession(session)
    val owedAmountsRepo = OwedAmountRepository.fromSession(session)
    for
      last <- createExpenses(session)
      expenseId = ExpenseId(last.id)
      from = CircleMemberId(last.paidBy)
      owed = CreateOwedAmountInput(expenseId, from, from, Amount(10))
      key = OwedAmountKey(expenseId, from, from)
      result <- owedAmountsRepo.create(owed)
      write = OwedAmountWriteMapper(result.id, None, None, Some(20))
      _ <- owedAmountsRepo.update(write)
      obtained <- owedAmountsRepo.get(key).rethrow
    yield assertEquals(obtained.amount, 20f)

  test("You should be able to get all owed amounts for an expense"):
    val session = sessionPool()
    val expenseRepo = ExpenseRepository.fromSession(session)
    val owedAmountsRepo = OwedAmountRepository.fromSession(session)
    for
      last <- createExpenses(session)
      expenseId = ExpenseId(last.id)
      from = CircleMemberId(last.paidBy)
      owed = CreateOwedAmountInput(expenseId, from, from, Amount(10))
      owed2 = CreateOwedAmountInput(expenseId, from, from, Amount(20))
      _ <- owedAmountsRepo.create(owed)
      _ <- owedAmountsRepo.create(owed2)
      obtained <- owedAmountsRepo.fromExpense(expenseId)
    yield assertEquals(obtained.length, 2)

  test("You should be able to get all owed amounts by a circle member"):
    val session = sessionPool()
    val expenseRepo = ExpenseRepository.fromSession(session)
    val owedAmountsRepo = OwedAmountRepository.fromSession(session)
    for
      last <- createExpenses(session)
      expenseId = ExpenseId(last.id)
      from = CircleMemberId(last.paidBy)
      owed = CreateOwedAmountInput(expenseId, from, from, Amount(10))
      owed2 = CreateOwedAmountInput(expenseId, from, from, Amount(20))
      _ <- owedAmountsRepo.create(owed)
      _ <- owedAmountsRepo.create(owed2)
      obtained <- owedAmountsRepo.fromCircleMemberFrom(from)
    yield assertEquals(obtained.length, 2)

  test("You should be able to get all owed amount details for an expense"):
    val session = sessionPool()
    val expenseRepo = ExpenseRepository.fromSession(session)
    val owedAmountsRepo = OwedAmountRepository.fromSession(session)
    for
      last <- createExpenses(session)
      expenseId = ExpenseId(last.id)
      from = CircleMemberId(last.paidBy)
      owed = CreateOwedAmountInput(expenseId, from, from, Amount(10))
      owed2 = CreateOwedAmountInput(expenseId, from, from, Amount(20))
      _ <- owedAmountsRepo.create(owed)
      _ <- owedAmountsRepo.create(owed2)
      obtained <- owedAmountsRepo.detailFromExpense(expenseId)
    yield assertEquals(obtained.length, 2)
