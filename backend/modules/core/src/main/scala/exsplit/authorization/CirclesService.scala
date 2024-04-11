package exsplit.authorization

import cats.MonadThrow
import exsplit.authorization.extractors._
import exsplit.spec._
import exsplit.circles._
import cats.effect._
import cats.syntax.all._
import cats._
import skunk.Session
import exsplit.datamapper.user.UserMapper
import exsplit.datamapper.circles._

object CirclesWithAuthEntryPoint:
  def fromSession[F[_]: Concurrent](
      userInfo: F[Email],
      session: Session[F]
  ): F[CirclesService[F]] =
    for
      service <- CirclesEntryPoint.fromSession(session)
      userMapper <- UserMapper.fromSession(session)
      circlesRepo <- CirclesRepository.fromSession(session)
      membersRepo <- CircleMembersRepository.fromSession(session)
      circlesAuth = CirclesAuth(circlesRepo, userMapper)
      userAuth = UserAuth(userMapper)
      circleMemberAuth = CircleMemberAuth(userMapper, membersRepo)
    yield CirclesServiceWithAuth(
      userInfo,
      circlesAuth,
      circleMemberAuth,
      userAuth,
      service
    )

case class CirclesServiceWithAuth[F[_]: MonadThrow](
    userInfo: F[Email],
    circlesAuth: CirclesAuth[F],
    circleMemberAuth: CircleMemberAuth[F],
    userAuth: UserAuth[F],
    serviceImpl: CirclesService[F]
) extends CirclesService[F]:

  def listCircleMembers(circleId: CircleId): F[MembersListOut] =
    for
      _ <- circlesAuth.authCheck(userInfo, circleId)
      res <- serviceImpl.listCircleMembers(circleId)
    yield res

  def removeMemberFromCircle(
      circleId: CircleId,
      memberId: CircleMemberId
  ): F[Unit] =
    for _ <- circleMemberAuth.authCheck(userInfo, memberId)
    yield serviceImpl.removeMemberFromCircle(circleId, memberId)

  def getCircle(circleId: CircleId): F[GetCircleOutput] =
    for
      _ <- circlesAuth.authCheck(userInfo, circleId)
      res <- serviceImpl.getCircle(circleId)
    yield res

  def addUserToCircle(
      userId: UserId,
      displayName: String,
      circleId: CircleId
  ): F[Unit] =
    for
      _ <- userAuth.authCheck(userInfo, userId)
      _ <- circlesAuth.authCheck(userInfo, circleId)
    yield serviceImpl.addUserToCircle(userId, displayName, circleId)

  def changeDisplayName(
      circleId: CircleId,
      memberId: CircleMemberId,
      displayName: String
  ): F[Unit] =
    for _ <- circleMemberAuth.authCheck(userInfo, memberId)
    yield serviceImpl.changeDisplayName(circleId, memberId, displayName)

  def createCircle(
      userId: UserId,
      displayName: String,
      circleName: String,
      description: Option[String]
  ): F[CreateCircleOutput] =
    for
      _ <- userAuth.authCheck(userInfo, userId)
      res <- serviceImpl.createCircle(
        userId,
        displayName,
        circleName,
        description
      )
    yield res

  def deleteCircle(circleId: CircleId): F[Unit] =
    for _ <- circlesAuth.authCheck(userInfo, circleId)
    yield serviceImpl.deleteCircle(circleId)

  def listCirclesForUser(userId: UserId): F[CirclesOut] =
    for
      _ <- userAuth.authCheck(userInfo, userId)
      res <- serviceImpl.listCirclesForUser(userId)
    yield res

  def updateCircle(
      circleId: CircleId,
      circleName: Option[String],
      description: Option[String]
  ): F[Unit] =
    for _ <- circlesAuth.authCheck(userInfo, circleId)
    yield serviceImpl.updateCircle(circleId, circleName, description)
