package exsplit.circles

import exsplit.spec._
import cats.effect._
import cats.syntax.all._
import cats._
import cats.data._
import exsplit.auth._
import exsplit.circles._

object CirclesEntryPoint:
  def createService[F[_]: MonadThrow](
      circleRepository: CirclesRepository[F],
      userRepository: UserRepository[F]
  ): CirclesServiceImpl[F] =
    CirclesServiceImpl(circleRepository, userRepository)

def withValidCircle[F[_]: MonadThrow, A](
    circleId: CircleId,
    circleRepository: CirclesRepository[F]
)(action: CircleOut => F[A]): F[A] =
  for
    circle <- circleRepository.findCircleById(circleId).rethrow
    result <- action(circle)
  yield result

def withValidCircleMember[F[_]: MonadThrow, A](
    memberId: CircleMemberId,
    circleRepository: CirclesRepository[F]
)(action: CircleMemberOut => F[A]): F[A] =
  for
    member <- circleRepository.getCircleMemberById(memberId).rethrow
    result <- action(member)
  yield result

case class CirclesServiceImpl[F[_]: MonadThrow](
    circleRepository: CirclesRepository[F],
    userRepo: UserRepository[F]
) extends CirclesService[F]:

  def listCirclesForUser(userId: UserId): F[ListCirclesForUserOutput] =
    withValidUser(userId, userRepo): user =>
      for circlesOut <-
          circleRepository.getCirclesForUser(user).map(CirclesOut(_))
      yield ListCirclesForUserOutput(circlesOut)

  def removeMemberFromCircle(
      circleId: CircleId,
      member: CircleMemberId
  ): F[Unit] =
    withValidCircle(circleId, circleRepository): circle =>
      withValidCircleMember(member, circleRepository): member =>
        for
          _ <- circleRepository.removeUserFromCircle(circle, member)
          // TODO: the user is not allowed to have outstanding debts in the circle
          members <- circleRepository.listCircleMembers(circle)
          _ <- handleEmptyCircle(circleId, members)
        yield ()

  def getCircle(circleId: CircleId): F[GetCircleOutput] =
    circleRepository.findCircleById(circleId).rethrow.map(GetCircleOutput(_))

  def changeDisplayName(
      circleId: CircleId,
      memberId: CircleMemberId,
      displayName: String
  ): F[Unit] =
    withValidCircleMember(memberId, circleRepository): member =>
      withValidCircle(circleId, circleRepository): circle =>
        circleRepository.changeDisplayName(member, circle, displayName)

  def createCircle(
      userId: UserId,
      displayName: String,
      circleName: String,
      description: Option[String]
  ): F[CreateCircleOutput] =
    withValidUser(userId, userRepo): user =>
      circleRepository
        .createCircle(user, displayName, circleName, description)
        .map(CreateCircleOutput(_))

  def addUserToCircle(
      user: UserId,
      displayName: String,
      circleId: CircleId
  ): F[Unit] =
    withValidUser(user, userRepo): user =>
      withValidCircle(circleId, circleRepository): circle =>
        circleRepository.addUserToCircle(user, displayName, circle)

  def deleteCircle(circleId: CircleId): F[Unit] =
    // TODO: the circle is not allowed to have outstanding debts
    circleRepository.deleteCircle(circleId)

  def listCircleMembers(circleId: CircleId): F[ListCircleMembersOutput] =
    withValidCircle(circleId, circleRepository): circle =>
      for
        members <- circleRepository.listCircleMembers(circle)
        membersListOut = MembersListOut(members)
      yield ListCircleMembersOutput(membersListOut)

  def updateCircle(
      circleId: CircleId,
      name: Option[String],
      description: Option[String]
  ): F[Unit] =
    withValidCircle(circleId, circleRepository): circle =>
      circleRepository.updateCircle(circle, name, description)

  private def handleEmptyCircle(
      circleId: CircleId,
      members: List[CircleMemberOut]
  ): F[Unit] =
    members match
      case Nil => circleRepository.deleteCircle(circleId)
      case _   => ().pure[F]
