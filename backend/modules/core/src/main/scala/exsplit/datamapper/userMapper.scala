package exsplit.datamapper.user

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
import exsplit.database._

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
  * password text not null,
  * email text not null,
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

/** A trait representing a UserMapper, which is responsible for mapping user
  * data between different representations.
  *
  * @tparam F
  *   The effect type used in the mapping operations.
  */
trait UserMapper[F[_]]
    extends DataMapper[
      F,
      (String, String, String),
      UserReadMapper,
      UserWriteMapper,
      String
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

/** A companion object for the `UserMapper` trait. This object contains the
  * implementation of the `UserMapper` trait.
  */
object UserMapper:
  /*
   * Creates a new `UserMapper` instance from the given `Session`.
   * Each query is run inside an independent session.
   *
   * @param pool
   *   The session pool to be used for database operations.
   * @return
   *   The created `UserMapper` instance.
   */
  def fromSession[F[_]: Cancel: Parallel](
      pool: AppSessionPool[F]
  ): UserMapper[F] =
    new UserMapper[F]:

      def get(id: String): F[Either[NotFoundError, UserReadMapper]] =
        pool
          .option(userFromId, id)
          .map(_.toRight(NotFoundError(s"User $id not found.")))

      def delete(id: String): F[Unit] =
        pool.exec(deleteUserCommand, id).void

      def create(input: (String, String, String)): F[UserReadMapper] =
        pool.unique(createUserCommand, input)

      def findUserById(
          userId: UserId
      ): F[Either[NotFoundError, UserReadMapper]] =
        get(userId.value)

      def findUserByEmail(
          email: Email
      ): F[Either[NotFoundError, UserReadMapper]] =
        pool
          .option(userFromEmail, email.value)
          .map(_.toRight(NotFoundError(s"User with email $email not found.")))

      def createUser(id: UUID, email: Email, password: String): F[Unit] =
        create((id.toString, email.value, password)).void

      def update(user: UserWriteMapper): F[Unit] =
        List(
          user.email.map: mail =>
            pool.exec(updateEmail, (mail, user.id)),
          user.password.map: pass =>
            pool.exec(updatePassword, (pass, user.id))
        ).flatten.parSequence.void

      def updateUser(user: UserWriteMapper): F[Unit] =
        update(user).void

      def deleteUser(userId: UserId): F[Unit] =
        delete(userId.value).void

  private val userFromEmail: Query[String, UserReadMapper] =
    sql"""
      SELECT id, email, password
      FROM users
      WHERE email = $text
    """
      .query(text *: text *: text)
      .to[UserReadMapper]

  private val userFromId: Query[String, UserReadMapper] =
    sql"""
      SELECT id, email, password
      FROM users
      WHERE id = $text
    """
      .query(text *: text *: text)
      .to[UserReadMapper]

  private val createUserCommand
      : Query[(String, String, String), UserReadMapper] =
    sql"""
      INSERT INTO users (id, email, password)
      VALUES ($text, $text, $text)
      RETURNING id, email, password
    """
      .query(text *: text *: text)
      .to[UserReadMapper]

  private val updateEmail: Command[(String, String)] =
    sql"""
      UPDATE users
      SET email = $text
      WHERE id = $text
    """.command

  private val updatePassword: Command[(String, String)] =
    sql"""
      UPDATE users
      SET password = $text
      WHERE id = $text
    """.command

  private val deleteUserCommand: Command[String] =
    sql"""
      DELETE FROM users
      WHERE id = $text
    """.command
