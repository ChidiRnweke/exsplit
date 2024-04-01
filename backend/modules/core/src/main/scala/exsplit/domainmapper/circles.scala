package exsplit.domainmapper

import exsplit.spec._
import exsplit.datamapper.circles._
import cats.effect._
import cats.syntax.all._
import cats._

/** Extension methods for mapping circles between different representations.
  */
object CirclesOps:

  extension [F[_]: MonadThrow](circlesMapper: CirclesMapper[F])
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
      for circleRead <- circlesMapper.get(id).rethrow
      yield circleRead.toCircleOut

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
      for circleRead <- circlesMapper.create(input)
      yield circleRead.toCircleOut

  extension (circleRead: CircleReadMapper)
    /** Extension method for `CircleReadMapper` that converts a
      * `CircleReadMapper` object to a `CircleOut` object. The former is the
      * representation of a circle in the database, while the latter is the
      * representation of a circle in the application.
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
      * representation of a list of circles in the domain and not in the
      * database.
      * @param userCircleRead
      *   The `UserCirclesMapper` instance.
      */
    def getCirclesOut(userId: UserId): F[CirclesOut] =
      for userCirclesRead <- userCircleRead.listPrimaries(userId)
      yield userCirclesRead.toCirclesOut

  /** Extension method for `List[CircleReadMapper]` that converts a list of
    * `CircleReadMapper` instances to `CircleOut` instances.
    * @param userCircles
    *   The list of `CircleReadMapper` instances.
    */
  extension (userCircles: List[CircleReadMapper])
    def toCirclesOut: CirclesOut =
      CirclesOut(userCircles.map(_.toCircleOut))

  /** Extension methods for mapping circle members between different
    * representations.
    */
object CircleMemberOps:

  extension [F[_]: MonadThrow](circleMemberMapper: CircleMemberMapper[F])
    /** Extension method for `CircleMemberMapper` that retrieves a
      * `CircleMemberOut` object by its ID. The former is the representation of
      * a circle member in the database, while the latter is the representation
      * of a circle member in the application.
      *
      * @param circleMemberMapper
      *   The `CircleMemberMapper` instance.
      * @param id
      *   The ID of the circle member to retrieve.
      * @return
      *   A `CircleMemberOut` object wrapped in the effect type `F`.
      */
    def getCircleMemberOut(id: CircleMemberId): F[CircleMemberOut] =
      for circleMemberRead <- circleMemberMapper.get(id).rethrow
      yield circleMemberRead.toCircleMemberOut

    def addCircleMember(
        userId: UserId,
        displayName: String,
        circleId: CircleId
    ): F[CircleMemberOut] =
      val input = AddUserToCircleInput(userId, displayName, circleId)
      for circleMemberRead <- circleMemberMapper.create(input)
      yield circleMemberRead.toCircleMemberOut

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
      for circleMembersRead <- circleToMembersMapper.listChildren(circleId)
      yield circleMembersRead.toCircleMemberOuts

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