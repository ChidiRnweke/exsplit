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
  def fromSession[F[_]: Concurrent](
      session: Session[F]
  ): F[CirclesService[F]] =
    for
      circlesRepo <- CirclesRepository.fromSession(session)
      circleMembersRepo <- CircleMembersRepository.fromSession(session)
      userRepo <- UserMapper.fromSession(session)
    yield CirclesServiceImpl(circlesRepo, circleMembersRepo, userRepo)

def withValidCircle[F[_]: MonadThrow, A](
    circleId: CircleId,
    circleRepository: CirclesRepository[F]
)(action: CircleOut => F[A]): F[A] =
  for
    circleOut <- circleRepository.main.getCircleOut(circleId)
    result <- action(circleOut)
  yield result

def withValidCircleMember[F[_]: MonadThrow, A](
    memberId: CircleMemberId,
    circleMemberRepository: CircleMembersRepository[F]
)(action: CircleMemberOut => F[A]): F[A] =
  for
    member <- circleMemberRepository.main.getCircleMemberOut(memberId)
    result <- action(member)
  yield result

case class CirclesServiceImpl[F[_]: MonadThrow](
    circlesRepo: CirclesRepository[F],
    circleMembersRepo: CircleMembersRepository[F],
    userRepo: UserMapper[F]
) extends CirclesService[F]:

  def listCirclesForUser(userId: UserId): F[CirclesOut] =
    withValidUser(userId, userRepo): user =>
      for circles <- circlesRepo.byUser.getCirclesOut(userId)
      yield circles

  def removeMemberFromCircle(
      circleId: CircleId,
      member: CircleMemberId
  ): F[Unit] =
    withValidCircle(circleId, circlesRepo): _ =>
      withValidCircleMember(member, circleMembersRepo): _ =>
        for
          _ <- circleMembersRepo.main.delete(member)
          // TODO: the user is not allowed to have outstanding debts in the circle
          members <- circleMembersRepo.byCircle.getCircleMembersOuts(circleId)
          _ <- handleEmptyCircle(circleId, members)
        yield ()

  def getCircle(circleId: CircleId): F[GetCircleOutput] =
    circlesRepo.main.getCircleOut(circleId).map(GetCircleOutput(_))

  def changeDisplayName(
      circleId: CircleId,
      memberId: CircleMemberId,
      displayName: String
  ): F[Unit] =
    withValidCircleMember(memberId, circleMembersRepo): member =>
      withValidCircle(circleId, circlesRepo): circle =>
        val write = CircleMemberWriteMapper(memberId.value, displayName)
        circleMembersRepo.main.update(write)

  def createCircle(
      userId: UserId,
      displayName: String,
      circleName: String,
      description: Option[String]
  ): F[CreateCircleOutput] =
    withValidUser(userId, userRepo): user =>
      circlesRepo.main
        .createCircle(userId, displayName, circleName, description)
        .map(CreateCircleOutput(_))

  def addUserToCircle(
      user: UserId,
      displayName: String,
      circleId: CircleId
  ): F[Unit] =
    withValidUser(user, userRepo): validUser =>
      withValidCircle(circleId, circlesRepo): circle =>
        circleMembersRepo.main.addCircleMember(user, displayName, circleId).void

  def deleteCircle(circleId: CircleId): F[Unit] =
    // TODO: the circle is not allowed to have outstanding debts
    circlesRepo.main.delete(circleId)

  def listCircleMembers(circleId: CircleId): F[MembersListOut] =
    withValidCircle(circleId, circlesRepo): circle =>
      for members <- circleMembersRepo.byCircle.getCircleMembersOuts(circleId)
      yield MembersListOut(members)

  def updateCircle(
      circleId: CircleId,
      name: Option[String],
      description: Option[String]
  ): F[Unit] =
    withValidCircle(circleId, circlesRepo): circle =>
      val write = CircleWriteMapper(circleId.value, name, description)
      circlesRepo.main.update(write)

  private def handleEmptyCircle(
      circleId: CircleId,
      members: List[CircleMemberOut]
  ): F[Unit] =
    members match
      case Nil => circlesRepo.main.delete(circleId)
      case _   => ().pure[F]
