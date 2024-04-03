import cats.effect._
import cats.syntax.all._
import cats.data._
import cats._
import exsplit.config._
import exsplit.integration._
import exsplit.spec._
import exsplit.datamapper.circles._

class CirclesRepositorySuite extends DatabaseSuite:
  test("It is possible to write a circle to the database"):
    val session = sessionPool()
    val input = CreateCircleInput(UserId("1"), "User", "Test Circle")
    val result = session.use(session =>
      for
        circlesRepo <- CirclesRepository.fromSession(session)
        result <- circlesRepo.main.create(input)
      yield result match
        case CircleReadMapper(_, "Test Circle", "") => true
        case _                                      => false
    )
    result.assert

  test("It is possible to write and then read a circle from the database"):
    val session = sessionPool()
    val input = CreateCircleInput(UserId("2"), "User", "Test Circle")
    session.use(session =>
      for
        circlesRepo <- CirclesRepository.fromSession(session)
        createdCircle <- circlesRepo.main.create(input)
        circleRead <- circlesRepo.main.get(CircleId(createdCircle.id))
      yield assert(circleRead.isRight)
    )

  test("If you write and then read a circle, they should be the same"):
    val session = sessionPool()
    val input = CreateCircleInput(UserId("3"), "User", "Test Circle")
    session.use(session =>
      for
        circlesRepo <- CirclesRepository.fromSession(session)
        createdCircle <- circlesRepo.main.create(input)
        circleRead <- circlesRepo.main.get(CircleId(createdCircle.id)).rethrow
      yield assertEquals(createdCircle, circleRead)
    )

  test("You should be able to write and then delete a circle"):
    val session = sessionPool()
    val input = CreateCircleInput(UserId("3"), "User", "Test Circle")
    session.use(session =>
      for
        circlesRepo <- CirclesRepository.fromSession(session)
        createdCircle <- circlesRepo.main.create(input)
        _ <- circlesRepo.main.delete(CircleId(createdCircle.id))
        circleRead <- circlesRepo.main.get(CircleId(createdCircle.id))
      yield assert(circleRead.isLeft)
    )

  test("You should be to update a circle's name"):
    val session = sessionPool()
    val input = CreateCircleInput(UserId("3"), "User", "Test Circle")
    session.use(session =>
      for
        circlesRepo <- CirclesRepository.fromSession(session)
        createdCircle <- circlesRepo.main.create(input)
        circleWrite = CircleWriteMapper(
          createdCircle.id,
          Some("Updated Circle"),
          None
        )
        _ <- circlesRepo.main.update(circleWrite)
        circleRead <- circlesRepo.main.get(CircleId(createdCircle.id)).rethrow
      yield assertEquals(circleRead.name, "Updated Circle")
    )

  test("You should be able to update a circle's description"):
    val session = sessionPool()
    val input = CreateCircleInput(UserId("3"), "User", "Test Circle")
    session.use(session =>
      for
        circlesRepo <- CirclesRepository.fromSession(session)
        createdCircle <- circlesRepo.main.create(input)
        circleWrite = CircleWriteMapper(
          createdCircle.id,
          None,
          Some("Updated Description")
        )
        _ <- circlesRepo.main.update(circleWrite)
        circleRead <- circlesRepo.main.get(CircleId(createdCircle.id)).rethrow
      yield assertEquals(circleRead.description, "Updated Description")
    )

  test(
    "You should be able to update a circle's name and description at the same time"
  ):
    val session = sessionPool()
    val input = CreateCircleInput(UserId("3"), "User", "Test Circle")
    val result = session.use(session =>
      for
        circlesRepo <- CirclesRepository.fromSession(session)
        createdCircle <- circlesRepo.main.create(input)
        circleWrite = CircleWriteMapper(
          createdCircle.id,
          Some("Updated Circle"),
          Some("Updated Description")
        )
        _ <- circlesRepo.main.update(circleWrite)
        circleRead <- circlesRepo.main.get(CircleId(createdCircle.id)).rethrow
      yield circleRead
    )
    assertIO(result.map(_.name), "Updated Circle") >> assertIO(
      result.map(_.description),
      "Updated Description"
    )

  test("You should be able to list all from a user"):
    val session = sessionPool()
    val input1 = CreateCircleInput(UserId("4"), "User", "Test Circle")
    val input2 = CreateCircleInput(UserId("4"), "User", "Test Circle 2")
    val result = session.use(session =>
      for
        circlesRepo <- CirclesRepository.fromSession(session)
        _ <- circlesRepo.main.create(input1)
        _ <- circlesRepo.main.create(input2)
        circles <- circlesRepo.byUser.listPrimaries(UserId("4"))
      yield assertEquals(circles.length, 2)
    )
