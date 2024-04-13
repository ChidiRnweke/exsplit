package exsplit.datamapper.circles

import skunk._
import skunk.implicits._
import skunk.codec.all._
import natchez.Trace.Implicits.noop
import exsplit.spec._
import cats.effect._
import cats.syntax.all._
import cats._
import exsplit.datamapper._
import exsplit.database._

/** Represents a repository for managing circles. This trait contains the main
  * mapper for circles and the user circles mapper. The former has methods for
  * creating, updating, and deleting circles. The latter has methods for listing
  * primary circles for a given user.
  */
trait CirclesRepository[F[_]] extends CirclesMapper[F] with UserCirclesMapper[F]

/** Represents a repository for managing circle members. This trait contains the
  * main mapper for circle members and the circle to members mapper. The former
  * has methods for creating, updating, and deleting circle members. The latter
  * has methods for listing the children of a given parent circle.
  */
trait CircleMembersRepository[F[_]]
    extends CircleMemberMapper[F]
    with CircleToMembersMapper[F]:

  /** Retrieves the circle members for a given user. This means that you'll
    * typically get a list of all the circles that the user is a member of.
    *
    * @param userId
    *   The ID of the user.
    * @return
    *   A list of CircleMemberReadMapper objects.
    */
  def byUserId(userId: UserId): F[List[CircleMemberReadMapper]]
/*
Contains a factory method for creating a CirclesRepository from a session.
 */
object CirclesRepository:
  /** Creates an instance of CirclesRepository from the given session. This
    * method is effectful because it prepares the SQL queries and commands for
    * the underlying mappers.
    *
    * @param session
    *   The session to create the repository from.
    * @return
    *   An instance of CirclesRepository.
    */
  def fromSession[F[_]: Concurrent: Parallel](
      session: AppSessionPool[F]
  ): CirclesRepository[F] =
    val mainMapper = CirclesMapper.fromSession(session)
    val userCircles = UserCirclesMapper.fromSession(session)
    new CirclesRepository[F]:

      export mainMapper._
      export userCircles.{listPrimaries as byUserId}
      export userCircles.{listPrimaries}

/*
Contains a factory method for creating a CircleMembersRepository from a session.
 */
object CircleMembersRepository:
  /** Creates an instance of CircleMembersRepository from the given session.
    * This method is effectful because it prepares the SQL queries and commands
    * for the underlying mappers.
    *
    * @param session
    *   The session to create the repository from.
    * @return
    *   An instance of CircleMembersRepository.
    */
  def fromSession[F[_]: Concurrent](
      session: AppSessionPool[F]
  ): CircleMembersRepository[F] =

    val mainMapper = CircleMemberMapper.fromSession(session)
    val circleToMembers = CircleToMembersMapper.fromSession(session)
    val userToCircleMembers = UserToCircleMembersMapper.fromSession(session)
    new CircleMembersRepository[F]:

      export mainMapper._
      export circleToMembers.{listChildren, listChildren as byCircleId}
      export userToCircleMembers.{listChildren as byUserId}

/** Describes a circle read mapper. This class is a one to one mapping of the
  * circle table in the database without the creation and update timestamps.
  *
  * @param id
  *   The ID of the circle.
  * @param name
  *   The name of the circle.
  * @param description
  *   An optional description of the circle. It is listed as required because it
  *   is read from the database, but it is optional in reality. It will simply
  *   be an empty string if it is not provided.
  */
case class CircleReadMapper(
    id: String,
    name: String,
    description: String
)

/** Represents a CircleWriteMapper, which is used to map data for writing
  * circles. The ID is required, but the name and description are optional
  * because they can be updated independently or together.
  *
  * @param id
  *   The ID of the circle.
  * @param name
  *   An optional name for the circle.
  * @param description
  *   An optional description for the circle.
  */
case class CircleWriteMapper(
    id: String,
    name: Option[String],
    description: Option[String]
)

/** Represents a Circle Member Read Mapper. This class is a one to one mapping
  * of the circle member table in the database without the creation and update
  * timestamps.
  *
  * @param id
  *   The unique identifier of the circle member.
  * @param circleId
  *   The unique identifier of the circle.
  * @param userId
  *   The unique identifier of the user.
  * @param displayName
  *   The display name of the user.
  */
case class CircleMemberReadMapper(
    id: String,
    circleId: String,
    userId: String,
    displayName: String
)

/** Represents a mapper that maps users to circle members.
  *
  * @tparam F
  *   The effect type. It is typically effectful because it requires a database
  *   to fetch the data.
  */
trait UserToCircleMembersMapper[F[_]]
    extends HasMany[F, UserId, CircleMemberReadMapper]:

  /** Retrieves a list of circle members that are children of the specified
    * parent user.
    *
    * @param parent
    *   The ID of the parent user.
    * @return
    *   A list of circle members that are children of the specified parent user.
    */
  def listChildren(parent: UserId): F[List[CircleMemberReadMapper]]

/** Companion object for the `UserToCircleMembersMapper` trait. This object
  * contains a factory method for creating a new instance of the mapper.
  */
object UserToCircleMembersMapper:

  /** Creates a new instance of `UserToCircleMembersMapper` using the provided
    * session. This method is effectful because it prepares the SQL query for
    * the mapper.
    *
    * @param session
    *   The database session.
    * @tparam F
    *   The effect type, representing the context in which the mapping operation
    *   is performed.
    * @return
    *   A new instance of `UserToCircleMembersMapper`.
    */
  def fromSession[F[_]: Concurrent](
      session: AppSessionPool[F]
  ): UserToCircleMembersMapper[F] =
    new UserToCircleMembersMapper[F]:
      def listChildren(parent: UserId): F[List[CircleMemberReadMapper]] =
        session.stream(listCircleMembersQuery, parent.value)

  private val listCircleMembersQuery: Query[String, CircleMemberReadMapper] =
    sql"""
      SELECT cm.id, cm.circle_id, cm.user_id, cm.display_name
      FROM circle_members cm
      WHERE cm.user_id = $text
    """
      .query(text *: text *: text *: text)
      .to[CircleMemberReadMapper]

/** Represents a Circle Member Write Mapper. This class is used to map data for
  * writing circle members. The ID is required, but the display name is required
  * because it is the only field that can be updated.
  *
  * @param id
  *   The ID of the circle member.
  * @param displayName
  *   The display name of the circle member.
  */
case class CircleMemberWriteMapper(id: String, displayName: String)

/** Repository trait for managing circles. This trait defines the basic read and
  * write operations for circles. More complex operations are defined elsewhere.
  *
  * @tparam F
  *   The effect type, representing the context in which the repository
  *   operations are executed.
  */
trait CirclesMapper[F[_]]
    extends DataMapper[
      F,
      CreateCircleInput,
      CircleReadMapper,
      CircleWriteMapper,
      CircleId
    ]:

  /** Finds a circle by its ID.
    * @param CircleId
    *   The ID of the circle to find.
    * @return
    *   An effectful computation that yields either a `NotFoundError` or a
    *   `CircleReadMapper`. The former is returned if the circle is not found.
    */
  def get(
      circleId: CircleId
  ): F[Either[NotFoundError, CircleReadMapper]]

  /** Updates a circle. The circle is identified by its ID. The name and
    * description are optional because they can be updated independently or
    * together.
    * @param circle
    *   The circle to update.
    * @return
    *   An effectful computation that yields `Unit`.
    */
  def update(circle: CircleWriteMapper): F[Unit]

  /** Creates a new circle based on the provided input. The input is a type
    * automatically generated from smithy4s.
    *
    * @param input
    *   The input data for creating the circle.
    * @return
    *   A `CircleReadMapper` representing the created circle.
    */
  def create(input: CreateCircleInput): F[CircleReadMapper]

  /** Deletes a circle by its ID.
    * @param CircleId
    *   The ID of the circle to delete.
    * @return
    *   An effectful computation that yields `Unit`.
    */
  def delete(circleId: CircleId): F[Unit]

/** Repository trait for managing circle members. Defines the basic read and
  * write operations for circle members. More complex operations are defined
  * elsewhere.
  *
  * @tparam F
  *   the effect type, representing the context in which the operations are
  *   executed
  */
trait CircleMemberMapper[F[_]]
    extends DataMapper[
      F,
      AddUserToCircleInput,
      CircleMemberReadMapper,
      CircleMemberWriteMapper,
      CircleMemberId
    ]:
  /** Retrieves a circle member by their ID.
    *
    * @param id
    *   the ID of the circle member to retrieve
    * @return
    *   a `F` effect that resolves to either a `NotFoundError` if the circle
    *   member is not found, or a `CircleMemberReadMapper` if the circle member
    *   is found
    */
  def get(id: CircleMemberId): F[Either[NotFoundError, CircleMemberReadMapper]]

  /** Creates a new circle member.
    *
    * @param input
    *   the input data for creating the circle member
    * @return
    *   a `F` effect that resolves to a `CircleMemberReadMapper` representing
    *   the created circle member
    */
  def create(input: AddUserToCircleInput): F[CircleMemberReadMapper]

  /** Updates an existing circle member.
    *
    * @param circleMember
    *   the updated circle member data
    * @return
    *   a `F` effect that resolves to `Unit` when the update is successful
    */
  def update(circleMember: CircleMemberWriteMapper): F[Unit]

  /** Deletes a circle member by their ID.
    *
    * @param circleMemberId
    *   the ID of the circle member to delete
    * @return
    *   a `F` effect that resolves to `Unit` when the deletion is successful
    */
  def delete(circleMemberId: CircleMemberId): F[Unit]

/** A trait that represents a mapper for mapping circles to their members.
  *
  * @tparam F
  *   the effect type
  */
trait CircleToMembersMapper[F[_]]
    extends HasMany[F, CircleId, CircleMemberReadMapper]:

  /** Lists the children (members) of a given parent circle.
    *
    * @param parent
    *   the parent circle
    * @return
    *   a list of CircleMemberReadMapper instances representing the children
    */
  def listChildren(parent: CircleId): F[List[CircleMemberReadMapper]]

/** Represents a mapper for user circles. Provides methods to interact with the
  * database and retrieve user circles.
  *
  * @tparam F
  *   The effect type, representing the context in which the operations are
  *   executed.
  */
trait UserCirclesMapper[F[_]]
    extends BelongsToThrough[F, UserId, CircleReadMapper, CircleReadMapper]:

  /** Retrieves a list of primary circles for a given user.
    *
    * @param userId
    *   The ID of the user.
    * @return
    *   A list of primary circles.
    */
  def listPrimaries(userId: UserId): F[List[CircleReadMapper]]

/** Companion object for the `UserCirclesMapper` trait. This object contains a
  * factory method for creating a new instance of the mapper.
  */
object UserCirclesMapper:

  /** Creates a new instance of `UserCirclesMapper` from a session. This method
    * is effectful because it prepares the SQL query for the mapper.
    *
    * @param session
    *   The database session.
    * @tparam F
    *   The effect type, representing the context in which the operations are
    *   executed.
    * @return
    *   A new instance of `UserCirclesMapper`.
    */
  def fromSession[F[_]: Concurrent](
      session: AppSessionPool[F]
  ): UserCirclesMapper[F] =
    new UserCirclesMapper[F]:

      /** Retrieves a list of primary circles for a given user.
        *
        * @param userId
        *   The ID of the user.
        * @return
        *   A list of primary circles.
        */
      def listPrimaries(userId: UserId): F[List[CircleReadMapper]] =
        session.stream(listCirclesForUserQuery, userId.value)

  private val listCirclesForUserQuery: Query[String, CircleReadMapper] =
    sql"""
      SELECT c.id, c.name, c.description
      FROM circles c
      JOIN circle_members cm ON c.id = cm.circle_id
      WHERE cm.user_id = $text
    """
      .query(text *: text *: text)
      .to[CircleReadMapper]

object CircleToMembersMapper:
  /** Creates a CircleToMembersMapper instance from the provided session. This
    * method is effectful because it prepares the SQL query for the mapper.
    *
    * @param session
    *   the session to create the mapper from
    * @return
    *   a `F[CircleToMembersMapper[F]]` representing the asynchronous result of
    *   creating the mapper
    */
  def fromSession[F[_]: Concurrent](
      session: AppSessionPool[F]
  ): CircleToMembersMapper[F] =
    new CircleToMembersMapper[F]:
      def listChildren(parent: CircleId): F[List[CircleMemberReadMapper]] =
        session.stream(listCircleMembersQuery, parent.value)

  private val listCircleMembersQuery: Query[String, CircleMemberReadMapper] =
    sql"""
      SELECT cm.id, cm.circle_id, cm.user_id, cm.display_name
      FROM circle_members cm
      WHERE cm.circle_id = $text
    """
      .query(text *: text *: text *: text)
      .to[CircleMemberReadMapper]

/*
Contains a factory method for creating a CircleMemberMapper from a session.
 */
object CircleMemberMapper:

  /** Creates a `CircleMemberMapper` instance from a given `Session`. This
    * factory method is effectful because it prepares the SQL queries and
    * commands for the `CircleMemberMapper`.
    *
    * @param session
    *   The session to create the `CircleMemberMapper` from.
    * @return
    *   A `CircleMemberMapper` instance wrapped in the effect `F`.
    */
  def fromSession[F[_]: Cancel](
      session: AppSessionPool[F]
  ): CircleMemberMapper[F] =
    new CircleMemberMapper[F]:

      def get(
          id: CircleMemberId
      ): F[Either[NotFoundError, CircleMemberReadMapper]] =
        session
          .option(findCircleMemberByIdQuery, id.value)
          .map(_.toRight(NotFoundError(s"Circle member $id not found.")))

      def create(input: AddUserToCircleInput): F[CircleMemberReadMapper] =
        session.unique(addCircleMemberQuery, input)

      def update(circleMember: CircleMemberWriteMapper): F[Unit] =
        session.exec(updateCircleMemberCommand, circleMember)

      def delete(circleMemberId: CircleMemberId): F[Unit] =
        session.exec(deleteCircleMemberCommand, circleMemberId.value)

  private val findCircleMemberByIdQuery: Query[String, CircleMemberReadMapper] =
    sql"""
        SELECT cm.id, cm.circle_id, cm.user_id, cm.display_name
        FROM circle_members cm
        WHERE cm.id = $text
      """
      .query(text *: text *: text *: text)
      .to[CircleMemberReadMapper]

  private val addCircleMemberQuery
      : Query[AddUserToCircleInput, CircleMemberReadMapper] =
    sql"""
        INSERT INTO circle_members (display_name, user_id, circle_id)
        VALUES ($text, $text, $text)
        RETURNING id, circle_id, user_id, display_name
      """
      .query(text *: text *: text *: text)
      .contramap: (input: AddUserToCircleInput) =>
        (input.displayName, input.userId.value, input.circleId.value)
      .to[CircleMemberReadMapper]

  private val deleteCircleMemberCommand: Command[String] =
    sql"""
        DELETE FROM circle_members
        WHERE id = $text
      """.command

  private val updateCircleMemberCommand: Command[CircleMemberWriteMapper] =
    sql"""
          UPDATE circle_members
          SET display_name = $text
          WHERE id = $text
        """.command
      .contramap: (input: CircleMemberWriteMapper) =>
        (input.displayName, input.id)

/** A companion object for the `CirclesMapper` trait. This object contains the
  * implementation of the `CirclesMapper` trait.
  */
object CirclesMapper:

  /** This file contains the implementation of the CirclesMapper class, which is
    * responsible for mapping data between the application and the database for
    * the Circles module. The CirclesMapper class provides methods for
    * interacting with the database to perform CRUD operations on circles.
    *
    * This method is effectful because it prepares the SQL queries and commands
    * for the CirclesMapper.
    *
    * @param session
    *   The database session used for executing queries.
    * @tparam F
    *   The effect type, representing the context in which the operations are
    *   executed.
    */
  def fromSession[F[_]: Cancel: Parallel](
      sessionPool: AppSessionPool[F]
  ): CirclesMapper[F] =
    new CirclesMapper[F]:

      def get(circleId: CircleId): F[Either[NotFoundError, CircleReadMapper]] =
        sessionPool
          .option(findCircleByIdQuery, circleId.value)
          .map(_.toRight(NotFoundError(s"Circle $circleId not found.")))

      def create(input: CreateCircleInput): F[CircleReadMapper] =
        sessionPool.unique(
          createCircleQuery,
          (input.circleName, input.description.getOrElse(""))
        )

      def update(circle: CircleWriteMapper): F[Unit] =
        List(
          circle.name.map: name =>
            sessionPool.exec(updateCircleNameCommand, (name, circle.id)),
          circle.description.map: description =>
            sessionPool.exec(updateDescription, (description, circle.id))
        ).flatten.parSequence.void

      def delete(circleId: CircleId): F[Unit] =
        sessionPool.exec(deleteCircleQuery, (circleId.value))

  private val findCircleByIdQuery: Query[String, CircleReadMapper] =
    sql"""
      SELECT c.id, c.name, c.description
      FROM circles c
      WHERE c.id = $text
    """
      .query(text *: text *: text)
      .to[CircleReadMapper]

  private val updateCircleNameCommand: Command[(String, String)] =
    sql"""
      UPDATE circles
      SET name = $text
      WHERE id = $text
    """.command

  private val updateDescription: Command[(String, String)] =
    sql"""
      UPDATE circles
      SET description = $text
      WHERE id = $text
    """.command

  private val updateCircleQuery: Command[(String, String, String)] =
    sql"""
      UPDATE circles
      SET name = $text, description = $text
      WHERE id = $text
    """.command

  private val createCircleQuery: Query[(String, String), CircleReadMapper] =
    sql"""
      INSERT INTO circles (name, description)
      VALUES ($text, $text)
      RETURNING id, name, description
    """
      .query(text *: text *: text)
      .to[CircleReadMapper]

  private val deleteCircleQuery: Command[String] =
    sql"""
      DELETE FROM circles
      WHERE id = $text
    """.command
