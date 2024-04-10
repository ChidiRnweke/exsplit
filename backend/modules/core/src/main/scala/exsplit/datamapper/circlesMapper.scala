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
    with CircleToMembersMapper[F]
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
  def fromSession[F[_]: Concurrent](
      session: Session[F]
  ): F[CirclesRepository[F]] =
    for
      mainMapper <- CirclesMapper.fromSession(session)
      userCircles <- UserCirclesMapper.fromSession(session)
    yield new CirclesRepository[F]:

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
      session: Session[F]
  ): F[CircleMembersRepository[F]] =
    for
      mainMapper <- CircleMemberMapper.fromSession(session)
      circleToMembers <- CircleToMembersMapper.fromSession(session)
    yield new CircleMembersRepository[F]:

      export mainMapper._
      export circleToMembers.{listChildren, listChildren as byCircleId}

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
      session: Session[F]
  ): F[UserCirclesMapper[F]] =
    for listCirclesForUserQuery <- session.prepare(listCirclesForUserQuery)
    yield new UserCirclesMapper[F]:

      /** Retrieves a list of primary circles for a given user.
        *
        * @param userId
        *   The ID of the user.
        * @return
        *   A list of primary circles.
        */
      def listPrimaries(userId: UserId): F[List[CircleReadMapper]] =
        listCirclesForUserQuery.stream(userId.value, 1024).compile.toList

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
      session: Session[F]
  ): F[CircleToMembersMapper[F]] =
    for listCircleMembersQuery <- session.prepare(listCircleMembersQuery)
    yield new CircleToMembersMapper[F]:

      def listChildren(circleId: CircleId): F[List[CircleMemberReadMapper]] =
        listCircleMembersQuery.stream(circleId.value, 1024).compile.toList

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
  def fromSession[F[_]: Monad](session: Session[F]): F[CircleMemberMapper[F]] =
    for
      findCircleMember <- session.prepare(findCircleMemberByIdQuery)
      addCircleMember <- session.prepare(addCircleMemberQuery)
      updateCircleMember <- session.prepare(updateCircleMemberCommand)
      deleteCircleMember <- session.prepare(deleteCircleMemberCommand)
    yield new CircleMemberMapper:
      def get(
          id: CircleMemberId
      ): F[Either[NotFoundError, CircleMemberReadMapper]] =
        findCircleMember
          .option(id.value)
          .map:
            case Some(value) => Right(value)
            case None =>
              Left(NotFoundError(s"Circle member with ID = $id not found."))

      def create(input: AddUserToCircleInput): F[CircleMemberReadMapper] =
        addCircleMember.unique(input)

      def update(circleMember: CircleMemberWriteMapper): F[Unit] =
        updateCircleMember
          .execute(circleMember)
          .void

      def delete(circleMemberId: CircleMemberId): F[Unit] =
        deleteCircleMember.execute(circleMemberId.value).void

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
  def fromSession[F[_]: Monad](
      session: Session[F]
  ): F[CirclesMapper[F]] =
    for
      findCircleByIdQuery <- session.prepare(findCircleByIdQuery)
      updateCircleQuery <- session.prepare(updateCircleQuery)
      updateCircleNameCommand <- session.prepare(updateCircleNameCommand)
      updateCircleDescQuery <- session.prepare(updateCircleQueryDescription)
      createCircleQuery <- session.prepare(createCircleQuery)
      deleteCircleQuery <- session.prepare(deleteCircleQuery)
    yield new CirclesMapper[F]:

      def get(circleId: CircleId): F[Either[NotFoundError, CircleReadMapper]] =
        findCircleByIdQuery
          .option(circleId.value)
          .map:
            case Some(value) => Right(value)
            case None =>
              Left(NotFoundError(s"Circle with ID = $circleId not found."))

      def create(input: CreateCircleInput): F[CircleReadMapper] =
        createCircleQuery
          .unique(input.circleName, input.description.getOrElse(""))

      def update(circle: CircleWriteMapper): F[Unit] =
        val actions = List(
          circle.name.map(name =>
            updateCircleNameCommand.execute(name, circle.id)
          ),
          circle.description.map(desc =>
            updateCircleDescQuery.execute(desc, circle.id)
          )
        ).flatten

        actions.sequence.void

      def delete(circleId: CircleId): F[Unit] =
        deleteCircleQuery.execute(circleId.value).void

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

  private val updateCircleQueryDescription: Command[(String, String)] =
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
