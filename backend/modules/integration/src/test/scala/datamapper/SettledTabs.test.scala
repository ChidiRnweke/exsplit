import cats.effect._
import cats.syntax.all._
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
import exsplit.datamapper.settledTabs._
import java.util.Date
import smithy4s.Timestamp
import exsplit.database.AppSessionPool

class SettledTabsSuite extends DatabaseSuite:
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
    val userRepo = UserMapper.fromSession(session)
    for user <- userRepo.createUser(Email("test@test.com"), "password")
    yield UserId(user.id)

  /** Helper function to create a settled tab in the database. It creates three
    * settled tabs in the database. This test can be reused for other tests that
    * require settled tabs in the database.
    * @param session
    *   The session to use for the database connection.
    */
  def createTabs(session: AppSessionPool[IO]): IO[SettledTabReadMapper] =
    val amount = Amount(10)
    val settledTabRepo = SettledTabRepository.fromSession(session)
    for
      setupData <- setup(session)
      (id, from, to) = setupData
      createInput = SettleExpenseListInput(id, from, to, amount)
      _ <- settledTabRepo.create(createInput)
      _ <- settledTabRepo.create(createInput)
      settledTab <- settledTabRepo.create(createInput)
    yield settledTab

  test("You should be able to create a settled tab in the database"):
    val session = sessionPool()
    for settledTab <- createTabs(session).attempt
    yield assertEquals(settledTab.isRight, true)

  test("You should be able to read a settled tab from the database"):
    val session = sessionPool()
    val settledTabRepo = SettledTabRepository.fromSession(session)
    for
      expected <- createTabs(session)
      obtained <- settledTabRepo.get(expected.id).rethrow
    yield assertEquals(obtained, expected)

  test("You should be able to delete a settled tab from the database"):
    val session = sessionPool()
    val settledTabRepo = SettledTabRepository.fromSession(session)
    for
      expected <- createTabs(session)
      _ <- settledTabRepo.delete(expected.id)
      obtained <- settledTabRepo.get(expected.id)
    yield assertEquals(obtained.isLeft, true)

  test("You should be able to update a settled tab in the database"):
    val session = sessionPool()
    val settledTabRepo = SettledTabRepository.fromSession(session)
    for
      created <- createTabs(session)
      write = SettledTabWriteMapper(
        created.id,
        Some(created.toMember),
        Some(created.fromMember),
        None
      ) // the people are swapped
      _ <- settledTabRepo.update(write)
      read <- settledTabRepo.get(created.id).rethrow
      expected = (read.toMember, read.fromMember)
      obtained = (created.fromMember, created.toMember)
    yield assertEquals(expected, obtained)

  test("You should be able to get all settled tabs for an expense list"):
    val session = sessionPool()
    val settledTabRepo = SettledTabRepository.fromSession(session)
    for
      created <- createTabs(session)
      id = ExpenseListId(created.expenseListId)
      obtained <- settledTabRepo.fromExpenseList(id)
    yield assertEquals(obtained.length, 3)

  test("You should be able to get all settled tabs for a from member"):
    val session = sessionPool()
    val settledTabRepo = SettledTabRepository.fromSession(session)
    for
      created <- createTabs(session)
      id = CircleMemberId(created.fromMember)
      obtained <- settledTabRepo.byFromMembers(id)
    yield assertEquals(obtained.length, 3)

  test("You should be able to get all settled tabs for a to member"):
    val session = sessionPool()
    val settledTabRepo = SettledTabRepository.fromSession(session)
    for
      created <- createTabs(session)
      id = CircleMemberId(created.toMember)
      obtained <- settledTabRepo.byToMembers(id)
    yield assertEquals(obtained.length, 3)
