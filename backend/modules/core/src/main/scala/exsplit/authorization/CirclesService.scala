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
import exsplit.database._

/** Entry point for creating an instance of the CirclesService with
  * authorization. It is a wrapper around the CirclesEntryPoint that adds
  * authorization to the service.
  */
object CirclesWithAuthEntryPoint:
  /** Creates an instance of CirclesService with authorization from the provided
    * session. The session pool is eventually passed down to involved
    * repositories.
    *
    * @param userInfo
    *   The email of the authenticated user. Obtained from the fiber local
    *   through the middleware.
    * @param pool
    *   The AppSessionPool used for database operations.
    * @return
    *   An instance of CirclesService with authorization.
    */
  def fromSession[F[_]: Concurrent: Parallel](
      userInfo: F[Email],
      pool: AppSessionPool[F]
  ): CirclesService[F] =

    val service = CirclesEntryPoint.fromSession(pool)
    val userRepo = UserMapper.fromSession(pool)
    val circlesRepo = CirclesRepository.fromSession(pool)
    val membersRepo = CircleMembersRepository.fromSession(pool)
    makeAuthService(userInfo, service, userRepo, circlesRepo, membersRepo)

  private def makeAuthService[F[_]: MonadThrow: Parallel](
      userInfo: F[Email],
      service: CirclesService[F],
      userMapper: UserMapper[F],
      circlesRepo: CirclesRepository[F],
      membersRepo: CircleMembersRepository[F]
  ): CirclesService[F] =
    val circlesAuth = CirclesAuth(circlesRepo, userMapper)
    val userAuth = UserAuth(userMapper)
    val memberAuth = CircleMemberAuth(userMapper, circlesRepo, membersRepo)
    CirclesServiceWithAuth(userInfo, circlesAuth, memberAuth, userAuth, service)

/** The CirclesService with authorization. It is a wrapper around the
  * CirclesService that adds authorization to the service. Authorization checks
  * are run concurrently where possible. After the checks are successful, the
  * service is run. The eventual interpreter of the service is able to run this
  * as a CirclesService.
  *
  * @param userInfo
  *   The email of the authenticated user. Obtained from the fiber local through
  *   the middleware.
  * @param circlesAuth
  *   The authorization service for circles.
  * @param circleMemberAuth
  *   The authorization service for circle members.
  * @param userAuth
  *   The authorization service for users.
  * @param service
  *   The CirclesService to be wrapped.
  * @tparam F
  *   The effect type.
  */
case class CirclesServiceWithAuth[F[_]: MonadThrow: Parallel](
    userInfo: F[Email],
    circlesAuth: CirclesAuth[F],
    circleMemberAuth: CircleMemberAuth[F],
    userAuth: UserAuth[F],
    service: CirclesService[F]
) extends CirclesService[F]:

  def listCircleMembers(circleId: CircleId): F[MembersListOut] =
    circlesAuth.authCheck(userInfo, circleId) *>
      service.listCircleMembers(circleId)

  def removeMemberFromCircle(
      circleId: CircleId,
      memberId: CircleMemberId
  ): F[Unit] =
    circleMemberAuth.sameCircleMemberId(userInfo, memberId) *>
      service.removeMemberFromCircle(circleId, memberId)

  def getCircle(circleId: CircleId): F[GetCircleOutput] =
    circlesAuth.authCheck(userInfo, circleId) *>
      service.getCircle(circleId)

  def addUserToCircle(
      userId: UserId,
      displayName: String,
      circleId: CircleId
  ): F[Unit] =
    (
      userAuth.authCheck(userInfo, userId),
      circlesAuth.authCheck(userInfo, circleId)
    ).parTupled *>
      service.addUserToCircle(userId, displayName, circleId)

  def changeDisplayName(
      circleId: CircleId,
      memberId: CircleMemberId,
      displayName: String
  ): F[Unit] =
    circleMemberAuth.sameCircleMemberId(userInfo, memberId) *>
      service.changeDisplayName(circleId, memberId, displayName)

  def createCircle(
      userId: UserId,
      displayName: String,
      circleName: String,
      description: Option[String]
  ): F[CreateCircleOutput] =
    userAuth.authCheck(userInfo, userId) *>
      service.createCircle(userId, displayName, circleName, description)

  def deleteCircle(circleId: CircleId): F[Unit] =
    circlesAuth.authCheck(userInfo, circleId) *> service.deleteCircle(circleId)

  def listCirclesForUser(userId: UserId): F[CirclesOut] =
    userAuth.authCheck(userInfo, userId) *> service.listCirclesForUser(userId)

  def updateCircle(
      circleId: CircleId,
      circleName: Option[String],
      description: Option[String]
  ): F[Unit] =
    circlesAuth.authCheck(userInfo, circleId) *>
      service.updateCircle(circleId, circleName, description)
