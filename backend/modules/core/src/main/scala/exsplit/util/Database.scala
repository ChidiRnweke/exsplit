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

type Connection = [F[_]] =>> Resource[F, Resource[F, Session[F]]]
type AppSessionPool = [F[_]] =>> Resource[F, Session[F]]
type Cancel = [F[_]] =>> MonadCancel[F, Throwable]

extension [F[_]: Cancel](pool: AppSessionPool[F])
  def exec[A](command: Command[A], data: A): F[Unit] =
    pool
      .use: session =>
        session.prepare(command).flatMap(_.execute(data).void)

  def unique[Q, A](query: Query[Q, A], data: Q): F[A] =
    pool
      .use: session =>
        session.prepare(query).flatMap(_.unique(data))

  def option[Q, A](query: Query[Q, A], data: Q): F[Option[A]] =
    pool
      .use: session =>
        session.prepare(query).flatMap(_.option(data))

extension [F[_]: Concurrent](pool: AppSessionPool[F])
  def stream[Q, A](query: Query[Q, A], data: Q): F[List[A]] =
    pool
      .use: session =>
        session.prepare(query).flatMap(_.stream(data, 32).compile.toList)

object SessionPool:
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
