package exsplit.domainmapper

import exsplit.spec._
import exsplit.datamapper.circles._
import cats.effect._
import cats.syntax.all._
import cats._

extension [F[_]: MonadThrow](circlesMapper: CirclesRepository[F])
  /** Extension method for `CirclesMapper` that retrieves a `CircleOut` object
    * by its ID.
    *
    * @param circlesMapper
    *   The `CirclesMapper` instance.
    * @param id
    *   The ID of the circle to retrieve.
    * @return
    *   A `CircleOut` object wrapped in the effect type `F`.
    */
  def getCircleOut(id: CircleId): F[CircleOut] =
    circlesMapper.get(id).rethrow.map(_.toCircleOut)

  /** Creates a new circle within the domain type `CircleOut` instead of the
    * database representation `CircleReadMapper`.
    *
    * @param userId
    *   The ID of the user creating the circle.
    * @param displayName
    *   The display name of the circle.
    * @param circleName
    *   The name of the circle.
    * @param description
    *   An optional description of the circle.
    * @return
    *   A `CircleOut` object wrapped in the effect type `F`.
    */
  def createCircle(
      userId: UserId,
      displayName: String,
      circleName: String,
      description: Option[String]
  ): F[CircleOut] =
    val input =
      CreateCircleInput(userId, displayName, circleName, description)
    circlesMapper.create(input).map(_.toCircleOut)

extension [F[_]: MonadThrow, A](repo: CirclesRepository[F])
  def withValidCircle(circleId: CircleId)(action: CircleOut => F[A]): F[A] =
    for
      circleOut <- repo.getCircleOut(circleId)
      result <- action(circleOut)
    yield result

extension (circleRead: CircleReadMapper)
  /** Extension method for `CircleReadMapper` that converts a `CircleReadMapper`
    * object to a `CircleOut` object. The former is the representation of a
    * circle in the database, while the latter is the representation of a circle
    * in the application.
    *
    * @param circleRead
    *   The `CircleReadMapper` object to convert.
    * @return
    *   A `CircleOut` object.
    */
  def toCircleOut: CircleOut =
    CircleOut(circleRead.id, circleRead.name, circleRead.description)

extension [F[_]: MonadThrow](userCircleRead: UserCirclesMapper[F])
  /** Extension method for `UserCirclesMapper` that retrieves the circles
    * associated with a user. It returns a `CirclesOut` object, which is the
    * representation of a list of circles in the domain and not in the database.
    * @param userCircleRead
    *   The `UserCirclesMapper` instance.
    */
  def getCirclesOut(userId: UserId): F[CirclesOut] =
    userCircleRead.listPrimaries(userId).map(_.toCirclesOut)

/** Extension method for `List[CircleReadMapper]` that converts a list of
  * `CircleReadMapper` instances to `CircleOut` instances.
  * @param userCircles
  *   The list of `CircleReadMapper` instances.
  */
extension (userCircles: List[CircleReadMapper])
  def toCirclesOut: CirclesOut = CirclesOut(userCircles.map(_.toCircleOut))

/** Extension methods for mapping circle members between different
  * representations.
  */

extension [F[_]: MonadThrow](circleMemberMapper: CircleMembersRepository[F])
  /** Extension method for `CircleMembersRepository` that retrieves a
    * `CircleMemberOut` object by its ID. The former is the representation of a
    * circle member in the database, while the latter is the representation of a
    * circle member in the application.
    *
    * @param CircleMembersRepository
    *   The `CircleMembersRepository` instance.
    * @param id
    *   The ID of the circle member to retrieve.
    * @return
    *   A `CircleMemberOut` object wrapped in the effect type `F`.
    */
  def getCircleMemberOut(id: CircleMemberId): F[CircleMemberOut] =
    circleMemberMapper.get(id).rethrow.map(_.toCircleMemberOut)

  def addCircleMember(
      userId: UserId,
      displayName: String,
      circleId: CircleId
  ): F[CircleMemberOut] =
    val input = AddUserToCircleInput(userId, displayName, circleId)
    circleMemberMapper.create(input).map(_.toCircleMemberOut)

extension [F[_]: MonadThrow, A](repo: CircleMembersRepository[F])
  def withValidCircleMember(
      memberId: CircleMemberId
  )(action: CircleMemberOut => F[A]): F[A] =
    for
      member <- repo.getCircleMemberOut(memberId)
      result <- action(member)
    yield result

extension (circleMemberRead: CircleMemberReadMapper)
  /** Extension method for `CircleMemberReadMapper` that converts a
    * `CircleMemberReadMapper` object to a `CircleMemberOut` object.
    *
    * @param circleMemberRead
    *   The `CircleMemberReadMapper` object to convert.
    * @return
    *   A `CircleMemberOut` object.
    */
  def toCircleMemberOut: CircleMemberOut =
    CircleMemberOut(circleMemberRead.id, circleMemberRead.displayName)

extension [F[_]: MonadThrow](circleToMembersMapper: CircleToMembersMapper[F])
  /** Retrieves a list of `CircleMemberOut` objects for a given circle ID.
    *
    * @param circleToMembersMapper
    *   The `CircleToMembersMapper` instance.
    * @param circleId
    *   The ID of the circle.
    * @return
    *   A list of `CircleMemberOut` objects wrapped in the effect type `F`.
    */
  def getCircleMembersOuts(circleId: CircleId): F[List[CircleMemberOut]] =
    circleToMembersMapper.listChildren(circleId).map(_.toCircleMemberOuts)

extension (circleMembers: List[CircleMemberReadMapper])
  /** Extension method for a list of `CircleMemberReadMapper` objects that
    * converts them to a list of `CircleMemberOut` objects.
    *
    * @param circleMembers
    *   The list of `CircleMemberReadMapper` objects to convert.
    * @return
    *   A list of `CircleMemberOut` objects.
    */
  def toCircleMemberOuts: List[CircleMemberOut] =
    circleMembers.map(_.toCircleMemberOut)
