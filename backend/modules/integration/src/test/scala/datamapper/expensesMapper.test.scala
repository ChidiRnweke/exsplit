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

class ExpenseMapperSuite extends DatabaseSuite:
  /** Helper function to setup the database with a user, circle, and expense
    * list.
    * @param session
    *   The session to use for the database connection.
    */
  def setup(
      session: Session[IO]
  ): IO[(ExpenseListId, CircleMemberId, CircleMemberId)] =
    for
      userId <- createUser(session)
      userId2 <- createUser(session)
      input = CreateCircleInput(userId, "User", "Test Circle")
      circlesRepo <- CirclesRepository.fromSession(session)
      circleMembersRepo <- CircleMembersRepository.fromSession(session)
      expenseListRepo <- ExpenseListRepository.fromSession(session)
      circle <- circlesRepo.main.create(input)
      memberInput = AddUserToCircleInput(userId, "User", CircleId(circle.id))
      memberInput2 = AddUserToCircleInput(userId2, "User2", CircleId(circle.id))
      member <- circleMembersRepo.main.create(memberInput)
      member2 <- circleMembersRepo.main.create(memberInput2)
      expListInput = CreateExpenseListInput(CircleId(circle.id), "Test")

      expenseList <- expenseListRepo.main.create(expListInput)
    yield (
      ExpenseListId(expenseList.id),
      CircleMemberId(member.id),
      CircleMemberId(member2.id)
    )

  /** Helper function to create a user in the database.
    * @param session
    *   The session to use for the database connection.
    */
  def createUser(session: Session[IO]): IO[UserId] =
    val uuid = UUIDGen[IO].randomUUID
    for
      nextId <- uuid
      userRepo <- UserMapper.fromSession(session)
      _ <- userRepo.createUser(nextId, Email("test@test.com"), "password")
    yield UserId(nextId.toString())

  /** Helper function to create 3 expenses.
    * @param session
    *   The session to use for the database connection.
    */
  def createExpenses(session: Session[IO]): IO[ExpenseReadMapper] =
    val amount = Amount(10)
    for
      expenseRepo <- ExpenseRepository.fromSession(session)
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

      _ <- expenseRepo.main.create(expenseInput)
      _ <- expenseRepo.main.create(expenseInput)
      result <- expenseRepo.main.create(expenseInput)
    yield result

  test("You should be able to create an expense in the database"):
    val session = sessionPool()
    session.use: session =>
      for result <- createExpenses(session).attempt
      yield assertEquals(result.isRight, true)

  test("You should be able to get an expense from the database"):
    val session = sessionPool()
    session.use: session =>
      for
        expenseRepo <- ExpenseRepository.fromSession(session)
        expected <- createExpenses(session)
        obtained <- expenseRepo.main.get(ExpenseId(expected.id)).rethrow
      yield assertEquals(obtained, expected)

  test("You should be able to update an expense"):
    val session = sessionPool()
    session.use: session =>
      for
        expenseRepo <- ExpenseRepository.fromSession(session)
        result <- createExpenses(session)
        write = ExpenseWriteMapper(
          result.id,
          None,
          Some("updated"),
          Some(50),
          None
        )
        expected = ("updated", 50f)
        _ <- expenseRepo.main.update(write)
        obtained <- expenseRepo.main.get(ExpenseId(result.id)).rethrow
      yield assertEquals((obtained.description, obtained.price), expected)

  test("You should be able to delete an expense"):
    val session = sessionPool()
    session.use: session =>
      for
        expenseRepo <- ExpenseRepository.fromSession(session)
        expected <- createExpenses(session)
        _ <- expenseRepo.main.delete(ExpenseId(expected.id))
        obtained <- expenseRepo.main.get(ExpenseId(expected.id))
      yield assert(obtained.isLeft)

  test("You should be able to read a detailed expense"):
    val session = sessionPool()
    session.use: session =>
      for
        expenseRepo <- ExpenseRepository.fromSession(session)
        expected <- createExpenses(session)
        detail <- expenseRepo.detail.get(ExpenseId(expected.id)).rethrow
      yield assertEquals(detail.paidByName, "User")

  test("You should be able to read the expenses from a circle member"):
    val session = sessionPool()
    session.use: session =>
      for
        expenseRepo <- ExpenseRepository.fromSession(session)
        last <- createExpenses(session)
        expenses <- expenseRepo.byCircleMember.listChildren(
          CircleMemberId(last.paidBy)
        )
      yield assertEquals(expenses.length, 3)

  test("You should be able to read the expenses from a circle member"):
    val session = sessionPool()
    session.use: session =>
      for
        expenseRepo <- ExpenseRepository.fromSession(session)
        last <- createExpenses(session)
        expenses <- expenseRepo.byExpenseList.listChildren(
          ExpenseListId(last.expenseListId)
        )
      yield assertEquals(expenses.length, 3)

  test("You should be able to create an owed amount without errors"):
    val session = sessionPool()
    session.use: session =>
      for
        expenseRepo <- ExpenseRepository.fromSession(session)
        owedAmountsRepo <- OwedAmountRepository.fromSession(session)
        last <- createExpenses(session)
        expenseId = ExpenseId(last.id)
        from = CircleMemberId(last.paidBy)
        owed = CreateOwedAmountInput(expenseId, from, from, Amount(10))
        obtained <- owedAmountsRepo.main.create(owed).attempt
      yield assert(obtained.isRight)

  test("You should be able to create and then read an owed amount"):
    val session = sessionPool()
    session.use: session =>
      for
        expenseRepo <- ExpenseRepository.fromSession(session)
        owedAmountsRepo <- OwedAmountRepository.fromSession(session)
        last <- createExpenses(session)
        expenseId = ExpenseId(last.id)
        from = CircleMemberId(last.paidBy)
        owed = CreateOwedAmountInput(expenseId, from, from, Amount(10))
        expected <- owedAmountsRepo.main.create(owed)
        key = OwedAmountKey(expenseId, from, from)
        obtained <- owedAmountsRepo.main.get(key).rethrow
      yield assertEquals(expected, obtained)

  test("You should be able to delete an owned amount"):
    val session = sessionPool()
    session.use: session =>
      for
        expenseRepo <- ExpenseRepository.fromSession(session)
        owedAmountsRepo <- OwedAmountRepository.fromSession(session)
        last <- createExpenses(session)
        expenseId = ExpenseId(last.id)
        from = CircleMemberId(last.paidBy)
        owed = CreateOwedAmountInput(expenseId, from, from, Amount(10))
        key = OwedAmountKey(expenseId, from, from)
        expected <- owedAmountsRepo.main.create(owed)
        _ <- owedAmountsRepo.main.delete(key)
        obtained <- owedAmountsRepo.main.get(key)
      yield assert(obtained.isLeft)

  test("You should be able to update an owed amount"):
    val session = sessionPool()
    session.use: session =>
      for
        expenseRepo <- ExpenseRepository.fromSession(session)
        owedAmountsRepo <- OwedAmountRepository.fromSession(session)
        last <- createExpenses(session)
        expenseId = ExpenseId(last.id)
        from = CircleMemberId(last.paidBy)
        owed = CreateOwedAmountInput(expenseId, from, from, Amount(10))
        key = OwedAmountKey(expenseId, from, from)
        result <- owedAmountsRepo.main.create(owed)
        write = OwedAmountWriteMapper(result.id, None, None, Some(20))
        _ <- owedAmountsRepo.main.update(write)
        obtained <- owedAmountsRepo.main.get(key).rethrow
      yield assertEquals(obtained.amount, 20f)

  test("You should be able to get all owed amounts for an expense"):
    val session = sessionPool()
    session.use: session =>
      for
        expenseRepo <- ExpenseRepository.fromSession(session)
        owedAmountsRepo <- OwedAmountRepository.fromSession(session)
        last <- createExpenses(session)
        expenseId = ExpenseId(last.id)
        from = CircleMemberId(last.paidBy)
        owed = CreateOwedAmountInput(expenseId, from, from, Amount(10))
        owed2 = CreateOwedAmountInput(expenseId, from, from, Amount(20))
        _ <- owedAmountsRepo.main.create(owed)
        _ <- owedAmountsRepo.main.create(owed2)
        obtained <- owedAmountsRepo.byExpense.listChildren(expenseId)
      yield assertEquals(obtained.length, 2)

  test("You should be able to get all owed amounts by a circle member"):
    val session = sessionPool()
    session.use: session =>
      for
        expenseRepo <- ExpenseRepository.fromSession(session)
        owedAmountsRepo <- OwedAmountRepository.fromSession(session)
        last <- createExpenses(session)
        expenseId = ExpenseId(last.id)
        from = CircleMemberId(last.paidBy)
        owed = CreateOwedAmountInput(expenseId, from, from, Amount(10))
        owed2 = CreateOwedAmountInput(expenseId, from, from, Amount(20))
        _ <- owedAmountsRepo.main.create(owed)
        _ <- owedAmountsRepo.main.create(owed2)
        obtained <- owedAmountsRepo.byCircleMember.listChildren(from)
      yield assertEquals(obtained.length, 2)

  test("You should be able to get all owed amount details for an expense"):
    val session = sessionPool()
    session.use: session =>
      for
        expenseRepo <- ExpenseRepository.fromSession(session)
        owedAmountsRepo <- OwedAmountRepository.fromSession(session)
        last <- createExpenses(session)
        expenseId = ExpenseId(last.id)
        from = CircleMemberId(last.paidBy)
        owed = CreateOwedAmountInput(expenseId, from, from, Amount(10))
        owed2 = CreateOwedAmountInput(expenseId, from, from, Amount(20))
        _ <- owedAmountsRepo.main.create(owed)
        _ <- owedAmountsRepo.main.create(owed2)
        obtained <- owedAmountsRepo.detail.listChildren(expenseId)
      yield assertEquals(obtained.length, 2)
