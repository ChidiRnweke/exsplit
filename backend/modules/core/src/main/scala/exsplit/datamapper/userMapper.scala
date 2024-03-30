package exsplit.datamapper.user

import exsplit.db._
import skunk._
import skunk.implicits._
import skunk.codec.all._
import natchez.Trace.Implicits.noop
import exsplit.spec._
import cats.effect._
import cats.syntax.all._
import cats._
import cats.data._
import java.util.UUID
import exsplit.datamapper._

/** Represents a user read mapper. This class is a one to one mapping of the
  * user table in the database without the creation and update timestamps.
  *
  * @param id
  *   The unique identifier of the user.
  * @param email
  *   The email address of the user.
  * @param password
  *   The password of the user.
  *
  * ### schema
  * {{{
  *  create table "users" (
  * id text primary key default md5(now()::text || random()::text),
  * password varchar(255) not null,
  * email varchar(255) not null,
  * created_at timestamp not null default current_timestamp,
  * updated_at timestamp not null default current_timestamp
  * );
  * }}}
  */
case class UserReadMapper(id: String, email: String, password: String)

/** Represents a UserWriteMapper, which is used for mapping user data from the
  * database. This is used for updating the user data.
  *
  * @param id
  *   The unique identifier of the user.
  * @param email
  *   The optional email address of the user.
  * @param password
  *   The optional password of the user. The email and password are optional, so
  *   they can be updated independently or together.
  */
case class UserWriteMapper(
    id: String,
    email: Option[String],
    password: Option[String]
)

trait UserMapper[F[_]]
    extends DataMapper[
      F,
      (String, String, String),
      UserReadMapper,
      UserWriteMapper
    ]:
  /** Finds the user credentials based on the email.
    *
    * @param email
    *   the email of the user
    * @return
    *   an effect that yields either a `NotFoundError` or the user with the
    *   given email
    */

  def findUserById(userId: UserId): F[Either[NotFoundError, UserReadMapper]]

  /** Finds the user based on the email.
    *
    * @param email
    *   the email of the user
    * @return
    *   an effect that yields either a `NotFoundError` or the user with the
    *   given email
    */
  def findUserByEmail(email: Email): F[Either[NotFoundError, UserReadMapper]]

  /** Creates a new user with the specified ID, email, and password.
    *
    * @param id
    *   the ID of the user
    * @param email
    *   the email of the user
    * @param password
    *   the password of the user
    * @return
    *   an effect that yields `Unit` when the user is successfully created
    */
  def createUser(id: UUID, email: Email, password: String): F[Unit]

  /** Updates a user in the database. The email and password are optional, so
    * they can be updated independently or together.
    *
    * @param user
    *   The user object containing the updated information.
    * @return
    *   A `Unit` indicating the completion of the update operation.
    */
  def updateUser(user: UserWriteMapper): F[Unit]

  /** Deletes a user from the database. The User is identified by the ID.
    *
    * @param userId
    *   The ID of the user to be deleted.
    * @return
    *   A `Unit` indicating the completion of the delete operation.
    */
  def deleteUser(userId: UserId): F[Unit]

object UserMapper:
  private val userFromEmail: Query[String, UserReadMapper] =
    sql"""
      SELECT id, email, password
      FROM users
      WHERE email = $text
    """
      .query(varchar *: varchar *: varchar)
      .to[UserReadMapper]

  private val userFromId: Query[String, UserReadMapper] =
    sql"""
      SELECT id, email, password
      FROM users
      WHERE id = $text
    """
      .query(varchar *: varchar *: varchar)
      .to[UserReadMapper]

  private val createUser: Query[(String, String, String), UserReadMapper] =
    sql"""
      INSERT INTO users (id, email, password)
      VALUES ($varchar, $varchar, $varchar)
      RETURNING id, email, password
    """
      .query(varchar *: varchar *: varchar)
      .to[UserReadMapper]

  private val updateEmail: Command[(String, String)] =
    sql"""
      UPDATE users
      SET email = $varchar
      WHERE id = $varchar
    """.command

  private val updatePassword: Command[(String, String)] =
    sql"""
      UPDATE users
      SET password = $varchar
      WHERE id = $varchar
    """.command

  private val deleteUser: Command[String] =
    sql"""
      DELETE FROM users
      WHERE id = $varchar
    """.command

  def fromSession[F[_]: Concurrent: Parallel](
      session: Session[F]
  ): F[UserMapper[F]] =
    for
      findUserByIdQuery <- session.prepare(userFromId)
      findUserByEmailQuery <- session.prepare(userFromEmail)
      createUserCommand <- session.prepare(createUser)
      updateEmailCommand <- session.prepare(updateEmail)
      updatePasswordCommand <- session.prepare(updatePassword)
      deleteUserCommand <- session.prepare(deleteUser)
    yield new UserMapper[F]:

      def get(id: String): F[Either[NotFoundError, UserReadMapper]] =
        findUserByIdQuery
          .option(id)
          .map(_.toRight(NotFoundError(s"User with id = $id not found.")))

      def delete(id: String): F[Unit] =
        deleteUserCommand.execute(id).void

      def create(input: (String, String, String)): F[UserReadMapper] =
        createUserCommand.unique(input)

      def findUserById(
          userId: UserId
      ): F[Either[NotFoundError, UserReadMapper]] =
        findUserByEmailQuery
          .option(userId.value)
          .map(_.toRight(NotFoundError(s"User with id = $userId not found.")))

      def findUserByEmail(
          email: Email
      ): F[Either[NotFoundError, UserReadMapper]] =
        findUserByEmailQuery
          .option(email.value)
          .map(
            _.toRight(NotFoundError(s"User with email = $email not found."))
          )

      def createUser(id: UUID, email: Email, password: String): F[Unit] =
        createUserCommand
          .unique(id.toString, email.value, password)
          .void

      def update(user: UserWriteMapper): F[Unit] =
        val actions = List(
          user.email.map(email => updateEmailCommand.execute(user.id, email)),
          user.password.map(password =>
            updatePasswordCommand.execute(user.id, password)
          )
        ).flatten

        actions.parSequence.void

      def updateUser(user: UserWriteMapper): F[Unit] =
        user match
          case UserWriteMapper(id, Some(email), _) =>
            updateEmailCommand.execute(id, email).void

          case UserWriteMapper(id, _, Some(password)) =>
            updatePasswordCommand.execute(id, password).void
          case _ => ().pure[F]

      def deleteUser(userId: UserId): F[Unit] =
        deleteUserCommand.execute(userId.value).void
