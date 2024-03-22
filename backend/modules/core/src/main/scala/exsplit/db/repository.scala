package exsplit.db

import cats.effect._
import cats.syntax.all._
import cats.data._
import cats._
import exsplit.config._
import skunk._
import skunk.implicits._
import skunk.codec.all._
import natchez.Trace.Implicits.noop
import fs2.io.net.Network
import natchez._
import scala.annotation.targetName
import fs2._

trait SessionPoolBootstrap[F[_]: Temporal: natchez.Trace: std.Console: Network](
    repositoryConfig: RepositoryConfig
):
  val sessionPool = Session.pooled[F](
    host = repositoryConfig.host,
    user = repositoryConfig.user,
    password = Some(repositoryConfig.password),
    database = repositoryConfig.database,
    max = repositoryConfig.max
  )

trait SkunkRepository[F[_]: Concurrent](session: Resource[F, Session[F]]):

  def executeQuery[B](query: Query[Void, B]): F[List[B]] =
    session.use(_.execute(query))

  def executeQuery[A, B](query: PreparedQuery[F, A, B], param: A): F[List[B]] =
    query
      .stream(param, 64)
      .compile
      .toList
