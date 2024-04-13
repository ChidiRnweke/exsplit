package exsplit.database

import cats.effect._
import cats.syntax.all._
import cats.data._
import cats._
import skunk._
import skunk.implicits._
import skunk.codec.all._
import natchez.Trace.Implicits.noop
import fs2.io.net.Network
import natchez._
import scala.annotation.targetName
import fs2._
import exsplit.config._

/** Type alias for the connection type. This type alias represents a partially
  * applied type constructor that takes an effect type F and returns a
  * connection type.
  *
  * A connection is understood as a resource that manages the lifecycle of a
  * session pool.
  */
type Connection = [F[_]] =>> Resource[F, Resource[F, Session[F]]]

/** Represents a session pool for the application. This type alias represents a
  * partially applied type constructor that takes an effect type F and returns a
  * resource that manages the lifecycle of a session pool.
  *
  * A session pool is a resource that manages the lifecycle of a pool of
  * database sessions. The pool is used to produce sessions that can be used to
  * interact with the database.
  */
type AppSessionPool = [F[_]] =>> Resource[F, Session[F]]

/** Convenience alias for the `MonadCancel` type class.
  */
type Cancel = [F[_]] =>> MonadCancel[F, Throwable]

extension [F[_]: Cancel](pool: AppSessionPool[F])

  /** Executes a command directly on a session pool. A session is acquired from
    * the pool after which the command is prepared and executed.
    */
  def exec[A](command: Command[A], data: A): F[Unit] =
    pool
      .use: session =>
        session.prepare(command).flatMap(_.execute(data).void)

  /** A session is acquired from the pool after which the query is prepared and
    * executed. The result is expected to be unique.
    *
    * @param query
    *   The query to execute.
    * @param data
    *   The data to pass to the query.
    * @return
    *   The unique result of the query.
    */
  def unique[Q, A](query: Query[Q, A], data: Q): F[A] =
    pool
      .use: session =>
        session.prepare(query).flatMap(_.unique(data))

  /** A session is acquired from the pool after which the query is prepared and
    * executed. The result is optional.
    *
    * @param query
    *   The query to execute.
    * @param data
    *   The data to pass to the query.
    * @return
    *   An optional result of the query.
    */
  def option[Q, A](query: Query[Q, A], data: Q): F[Option[A]] =
    pool
      .use: session =>
        session.prepare(query).flatMap(_.option(data))

extension [F[_]: Concurrent](pool: AppSessionPool[F])
  /** A session is acquired from the pool after which the query is prepared and
    * executed. The results are streamed as a list.
    *
    * @param query
    *   The query to execute.
    * @param data
    *   The data to pass to the query.
    * @return
    *   A list of results from the query.
    */
  def stream[Q, A](query: Query[Q, A], data: Q): F[List[A]] =
    pool
      .use: session =>
        session.prepare(query).flatMap(_.stream(data, 32).compile.toList)

object SessionPool:
  /** Creates a connection pool to a PostgreSQL database.
    *
    * @param repositoryConfig
    *   The configuration for the PostgreSQL database.
    * @return
    *   A connection pool to the PostgreSQL database. The returned connection
    *   pool is a resource that manages the lifecycle of a session pool.
    */
  def makePool[F[_]: Temporal: natchez.Trace: std.Console: Network](
      repositoryConfig: PostgresConfig
  ): Connection[F] =
    Session.pooled[F](
      host = repositoryConfig.host,
      user = repositoryConfig.user,
      password = Some(repositoryConfig.password),
      database = repositoryConfig.database,
      max = repositoryConfig.max
    )
