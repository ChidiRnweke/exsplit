package exsplit.auth

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
import exsplit.auth._
import java.util.UUID

/** A trait representing a user repository. This trait provides methods for
  * finding and creating users.
  *
  * @tparam F
  *   the effect type, representing the context in which the repository operates
  */
trait UserRepository[F[_]]:
  /** Finds the user credentials based on the email.
    *
    * @param email
    *   the email of the user
    * @return
    *   an effect that yields either a `NotFoundError` or the user with the
    *   given email
    */

  def findUserById(userId: UserId): F[Either[NotFoundError, User]]

  /** Finds the user based on the email.
    *
    * @param email
    *   the email of the user
    * @return
    *   an effect that yields either a `NotFoundError` or the user with the
    *   given email
    */
  def findUserByEmail(email: Email): F[Either[NotFoundError, User]]

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

/** Trait representing a password validator. Provides methods for hashing and
  * checking passwords.
  *
  * @tparam F
  *   the effect type for the password validation operations
  */

/** A class that prepares queries and commands related to user operations. The
  * queries and commands are prepared in the context of a database session.
  * Doing this is effectful, so the class is parameterized by the effect type
  * `F`.
  *
  * @param session
  *   The database session to execute the queries and commands.
  */
case class UserQueryPreparer[F[_]](session: Session[F]):

  /** Prepared query to find a user by their ID.
    */
  val findUserByIdQuery: F[PreparedQuery[F, String, User]] =
    val query = sql"""
      SELECT u.id, u.email, u.password
      FROM users u
      WHERE u.id = $varchar
    """.query(varchar *: varchar *: varchar).to[User]
    session.prepare(query)

  /** Prepared query to find a user by their email.
    */
  val findUserByEmailQuery: F[PreparedQuery[F, String, User]] =
    val query = sql"""
      SELECT u.id, u.email, u.password
      FROM users u
      WHERE u.email = $varchar
    """.query(varchar *: varchar *: varchar).to[User]
    session.prepare(query)

  /** Prepared command to create a new user.
    */
  val createUserQuery: F[PreparedCommand[F, (String, String, String)]] =
    val query = sql"""
      INSERT INTO users (id, email, password)
      VALUES ($varchar, $varchar, $varchar)
    """.command
    session.prepare(query)

object UserRepository:
  /** Creates a service for interacting with the user repository. The service
    * provides methods for finding and creating users. The creation of the
    * service itself is effectful, so the method is parameterized by the effect
    * type `F`. The reason for this is that the service needs to prepare queries
    * and commands in the context of a database session.
    *
    * @param session
    *   The database session.
    * @return
    *   The user repository.
    *
    * @tparam F
    *   the effect type for the user repository operations in which the service
    *   itself is created in as well.
    *
    * @see
    *   https://www.postgresql.org/docs/current/sql-createprocedure.html
    */
  def createService[F[_]: MonadThrow](
      session: Session[F]
  ): F[UserRepository[F]] =
    val queryPreparer = UserQueryPreparer(session)
    for
      findUserByIdQuery <- queryPreparer.findUserByIdQuery
      findUserByEmailQuery <- queryPreparer.findUserByEmailQuery
      createUserQuery <- queryPreparer.createUserQuery
      userRepository = new UserRepository[F]:

        /** Finds a user by their ID.
          *
          * @param userId
          *   The ID of the user.
          * @return
          *   Either the user or a NotFoundError if the user is not found.
          */
        def findUserById(userId: UserId): F[Either[NotFoundError, User]] =
          findUserByIdQuery
            .option(userId.value)
            .map(_.toRight(NotFoundError("User not found.")))

        /** Finds a user by their email.
          *
          * @param email
          *   The email of the user.
          * @return
          *   Either the user or a NotFoundError if the user is not found.
          */
        def findUserByEmail(email: Email): F[Either[NotFoundError, User]] =
          findUserByEmailQuery
            .option(email.value)
            .map(_.toRight(NotFoundError("User not found.")))

        /** Creates a new user.
          *
          * @param id
          *   The ID of the user.
          * @param email
          *   The email of the user.
          * @param password
          *   The password of the user.
          * @return
          *   A unit value indicating success.
          */
        def createUser(id: UUID, email: Email, password: String): F[Unit] =
          createUserQuery.execute((id.toString, email.value, password)).void
    yield userRepository
