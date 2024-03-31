package exsplit.circles

import exsplit.spec._
import cats.effect._
import cats.syntax.all._
import cats._
import cats.data._
import exsplit.auth._
import exsplit.circles._
import exsplit.datamapper.user._
import exsplit.datamapper.circles._
import skunk.Session
import exsplit.domainmapper.CirclesOps._
import exsplit.domainmapper.CircleMemberOps._

object CirclesEntryPoint:
  def createService[F[_]: MonadThrow](
      circleRepository: CirclesMapper[F],
      circleMemberRepository: CircleMemberMapper[F],
      circleToMembersRepository: CircleToMembersMapper[F],
      userCirclesMapper: UserCirclesMapper[F],
      userRepository: UserMapper[F]
  ): CirclesServiceImpl[F] =
    CirclesServiceImpl(
      circleRepository,
      circleMemberRepository,
      circleToMembersRepository,
      userCirclesMapper,
      userRepository
    )

  def fromSession[F[_]: Concurrent: Parallel](
      session: Session[F]
  ): F[CirclesService[F]] =
    for
      circlesMapper <- CirclesMapper.fromSession(session)
      circleMemberMapper <- CircleMemberMapper.fromSession(session)
      circleToMembersMapper <- CircleToMembersMapper.fromSession(session)
      userCirclesMapper <- UserCirclesMapper.fromSession(session)
      userMapper <- UserMapper.fromSession(session)
    yield CirclesServiceImpl(
      circlesMapper,
      circleMemberMapper,
      circleToMembersMapper,
      userCirclesMapper,
      userMapper
    )

def withValidCircle[F[_]: MonadThrow, A](
    circleId: CircleId,
    circleRepository: CirclesMapper[F]
)(action: CircleOut => F[A]): F[A] =
  for
    circleOut <- circleRepository.getCircleOut(circleId)
    result <- action(circleOut)
  yield result

def withValidCircleMember[F[_]: MonadThrow, A](
    memberId: CircleMemberId,
    circleMemberRepository: CircleMemberMapper[F]
)(action: CircleMemberOut => F[A]): F[A] =
  for
    member <- circleMemberRepository.getCircleMemberOut(memberId)
    result <- action(member)
  yield result

case class CirclesServiceImpl[F[_]: MonadThrow](
    circleRepository: CirclesMapper[F],
    circleMemberRepository: CircleMemberMapper[F],
    circleToMembersRepository: CircleToMembersMapper[F],
    userCirclesMapper: UserCirclesMapper[F],
    userRepo: UserMapper[F]
) extends CirclesService[F]:

  def listCirclesForUser(userId: UserId): F[ListCirclesForUserOutput] =
    withValidUser(userId, userRepo): user =>
      for circles <- userCirclesMapper.getCirclesOut(userId)
      yield ListCirclesForUserOutput(circles)

  def removeMemberFromCircle(
      circleId: CircleId,
      member: CircleMemberId
  ): F[Unit] =
    withValidCircle(circleId, circleRepository): _ =>
      withValidCircleMember(member, circleMemberRepository): _ =>
        for
          _ <- circleMemberRepository.delete(member)
          // TODO: the user is not allowed to have outstanding debts in the circle
          members <- circleToMembersRepository.getCircleMembersOuts(circleId)
          _ <- handleEmptyCircle(circleId, members)
        yield ()

  def getCircle(circleId: CircleId): F[GetCircleOutput] =
    circleRepository.getCircleOut(circleId).map(GetCircleOutput(_))

  def changeDisplayName(
      circleId: CircleId,
      memberId: CircleMemberId,
      displayName: String
  ): F[Unit] =
    withValidCircleMember(memberId, circleMemberRepository): member =>
      withValidCircle(circleId, circleRepository): circle =>
        val write = CircleMemberWriteMapper(memberId.value, displayName)
        circleMemberRepository.update(write)

  def createCircle(
      userId: UserId,
      displayName: String,
      circleName: String,
      description: Option[String]
  ): F[CreateCircleOutput] =
    withValidUser(userId, userRepo): user =>
      circleRepository
        .createCircle(userId, displayName, circleName, description)
        .map(CreateCircleOutput(_))

  def addUserToCircle(
      user: UserId,
      displayName: String,
      circleId: CircleId
  ): F[Unit] =
    withValidUser(user, userRepo): validUser =>
      withValidCircle(circleId, circleRepository): circle =>
        circleMemberRepository.addCircleMember(user, displayName, circleId).void

  def deleteCircle(circleId: CircleId): F[Unit] =
    // TODO: the circle is not allowed to have outstanding debts
    circleRepository.delete(circleId)

  def listCircleMembers(circleId: CircleId): F[ListCircleMembersOutput] =
    withValidCircle(circleId, circleRepository): circle =>
      for
        members <- circleToMembersRepository.getCircleMembersOuts(circleId)
        membersListOut = MembersListOut(members)
      yield ListCircleMembersOutput(membersListOut)

  def updateCircle(
      circleId: CircleId,
      name: Option[String],
      description: Option[String]
  ): F[Unit] =
    withValidCircle(circleId, circleRepository): circle =>
      val write = CircleWriteMapper(circleId.value, name, description)
      circleRepository.update(write)

  private def handleEmptyCircle(
      circleId: CircleId,
      members: List[CircleMemberOut]
  ): F[Unit] =
    members match
      case Nil => circleRepository.delete(circleId)
      case _   => ().pure[F]
