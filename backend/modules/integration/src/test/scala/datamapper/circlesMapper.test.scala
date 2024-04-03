import cats.effect._
import cats.syntax.all._
import cats.data._
import cats._
import exsplit.config._
import exsplit.integration._
import exsplit.spec._
import exsplit.datamapper.circles._
import exsplit.datamapper.user._
import cats.effect.std.UUIDGen

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

class CircleMembersRepositorySuite extends DatabaseSuite:
  test("You should be able to add a member to a circle"):
    val session = sessionPool()
    val testCircle = CreateCircleInput(UserId("5"), "User", "Test Circle")
    val testUser = (UserId("5"), "User", "User")
    session.use(session =>
      for
        circlesRepo <- CirclesRepository.fromSession(session)
        circleMembersRepo <- CircleMembersRepository.fromSession(session)
        createdCircle <- circlesRepo.main.create(testCircle)
        userRepo <- UserMapper.fromSession(session)
        uuid <- UUIDGen[IO].randomUUID
        _ <- userRepo.createUser(uuid, Email("foo@bar.com"), "password")
        createUser = AddUserToCircleInput(
          UserId(uuid.toString()),
          "User",
          CircleId(createdCircle.id)
        )
        createdMember <- circleMembersRepo.main.create(createUser).attempt
      yield assertEquals(createdMember.isRight, true)
    )

  test("You should be able to add a member to a circle and then read it"):
    val session = sessionPool()
    val testCircle = CreateCircleInput(UserId("6"), "User", "Test Circle")
    val testUser = (UserId("6"), "User", "User")
    session.use(session =>
      for
        circlesRepo <- CirclesRepository.fromSession(session)
        circleMembersRepo <- CircleMembersRepository.fromSession(session)
        createdCircle <- circlesRepo.main.create(testCircle)
        userRepo <- UserMapper.fromSession(session)
        uuid <- UUIDGen[IO].randomUUID
        _ <- userRepo.createUser(uuid, Email("foo@bar.com"), "password")
        createUser = AddUserToCircleInput(
          UserId(uuid.toString()),
          "User",
          CircleId(createdCircle.id)
        )
        member <- circleMembersRepo.main.create(createUser)
        readMember <- circleMembersRepo.main
          .get(CircleMemberId(member.id))
          .rethrow
      yield assertEquals(readMember, member)
    )

  test("You should be able to add a member to a circle and then delete it"):
    val session = sessionPool()
    val testCircle = CreateCircleInput(UserId("7"), "User", "Test Circle")
    session.use(session =>
      for
        circlesRepo <- CirclesRepository.fromSession(session)
        circleMembersRepo <- CircleMembersRepository.fromSession(session)
        createdCircle <- circlesRepo.main.create(testCircle)
        userRepo <- UserMapper.fromSession(session)
        uuid <- UUIDGen[IO].randomUUID
        _ <- userRepo.createUser(uuid, Email("foo@bar.com"), "password")
        createUser = AddUserToCircleInput(
          UserId(uuid.toString()),
          "User",
          CircleId(createdCircle.id)
        )
        member <- circleMembersRepo.main.create(createUser)
        _ <- circleMembersRepo.main.delete(CircleMemberId(member.id))
        readMember <- circleMembersRepo.main
          .get(CircleMemberId(member.id))
      yield assert(readMember.isLeft)
    )

  test("You should be able to add a member to a circle and then update it"):
    val session = sessionPool()
    val testCircle = CreateCircleInput(UserId("8"), "User", "Test Circle")
    session.use(session =>
      for
        circlesRepo <- CirclesRepository.fromSession(session)
        circleMembersRepo <- CircleMembersRepository.fromSession(session)
        createdCircle <- circlesRepo.main.create(testCircle)
        userRepo <- UserMapper.fromSession(session)
        uuid <- UUIDGen[IO].randomUUID
        _ <- userRepo.createUser(uuid, Email("foo@bar.com"), "password")
        createUser = AddUserToCircleInput(
          UserId(uuid.toString()),
          "User",
          CircleId(createdCircle.id)
        )
        member <- circleMembersRepo.main.create(createUser)
        updatedMember = CircleMemberWriteMapper(
          member.id,
          "Updated User"
        )
        _ <- circleMembersRepo.main.update(updatedMember)
        readMember <- circleMembersRepo.main
          .get(CircleMemberId(member.id))
          .rethrow
      yield assertEquals(readMember.displayName, "Updated User")
    )

  test("You should be able to list all members from a circle"):
    val session = sessionPool()
    val testCircle = CreateCircleInput(UserId("9"), "User", "Test Circle")
    val testUser = (UserId("9"), "User", "User")
    session.use(session =>
      for
        circlesRepo <- CirclesRepository.fromSession(session)
        circleMembersRepo <- CircleMembersRepository.fromSession(session)
        createdCircle <- circlesRepo.main.create(testCircle)
        userRepo <- UserMapper.fromSession(session)
        uuid <- UUIDGen[IO].randomUUID
        _ <- userRepo.createUser(uuid, Email("foo@bar.com"), "password")
        createUser = AddUserToCircleInput(
          UserId(uuid.toString()),
          "User",
          CircleId(createdCircle.id)
        )
        _ <- circleMembersRepo.main.create(createUser)
        members <- circleMembersRepo.byCircle.listChildren(
          CircleId(createdCircle.id)
        )
      yield assertEquals(members.length, 1)
    )
