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

case class CirclesServiceImpl[F[_]](
    circleRepository: CirclesRepository[F],
    userRepo: UserRepository[F]
)(using F: MonadThrow[F])
    extends CirclesService[F]:

  def listCirclesForUser(userId: UserId): F[ListCirclesForUserOutput] =
    withValidUser(userId): user =>
      circleRepository.getCirclesForUser(user).map(ListCirclesForUserOutput(_))

  def removeUserFromCircle(circleId: CircleId, userId: UserId): F[Unit] =
    withValidUser(userId): user =>
      withValidCircle(circleId): circle =>
        for
          _ <- circleRepository.removeUserFromCircle(circle, user)
          members <- circleRepository.listCircleMembers(circle)
          _ <- handleEmptyCircle(circleId, members)
        yield ()

  def getCircle(circleId: CircleId): F[GetCircleOutput] =
    circleRepository.findCircleById(circleId).rethrow.map(GetCircleOutput(_))

  def changeDisplayName(
      circleId: CircleId,
      userId: UserId,
      displayName: String
  ): F[Unit] =
    withValidUser(userId): user =>
      withValidCircle(circleId): circle =>
        val member = CircleMember(userId, displayName)
        circleRepository.changeDisplayName(member, circle)

  def createCircle(
      userId: UserId,
      displayName: String,
      circleName: String,
      description: Option[String]
  ): F[Unit] =
    withValidUser(userId): user =>
      val member = CircleMember(userId, displayName)
      circleRepository.createCircle(member, circleName, description)

  def addUserToCircle(
      userId: UserId,
      displayName: String,
      circleId: CircleId
  ): F[Unit] =
    withValidUser(userId): user =>
      withValidCircle(circleId): circle =>
        val member = CircleMember(userId, displayName)
        circleRepository.addUserToCircle(member, circle)

  def deleteCircle(circleId: CircleId): F[Unit] =
    circleRepository.deleteCircle(circleId)

  def listCircleMembers(circleId: CircleId): F[ListCircleMembersOutput] =
    withValidCircle(circleId): circle =>
      for members <- circleRepository.listCircleMembers(circle)
      yield ListCircleMembersOutput(members)

  def updateCircle(
      circleId: CircleId,
      name: Option[String],
      description: Option[String]
  ): F[Unit] =
    withValidCircle(circleId): circle =>
      circleRepository.updateCircle(circle, name, description)

  private def withValidUser[A](userId: UserId)(action: User => F[A]): F[A] =
    for
      user <- userRepo.findUserById(userId).rethrow
      result <- action(user)
    yield result

  private def withValidCircle[A](
      circleId: CircleId
  )(action: CircleOut => F[A]): F[A] =
    for
      circle <- circleRepository.findCircleById(circleId).rethrow
      result <- action(circle)
    yield result

  private def handleEmptyCircle(
      circleId: CircleId,
      members: List[CircleMemberOut]
  ): F[Unit] =
    members match
      case Nil => circleRepository.deleteCircle(circleId)
      case _   => F.unit
