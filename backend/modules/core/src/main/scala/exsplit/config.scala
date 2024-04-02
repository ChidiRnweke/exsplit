package exsplit.config

import cats.Functor
import pureconfig._
import pureconfig.generic.derivation.default._
import scala.reflect.ClassTag
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

case class AuthConfig(
    secretKey: String
) derives ConfigReader

case class PostgresConfig(
    host: String,
    user: String,
    password: String,
    database: String,
    max: Int
) derives ConfigReader

case class MigrationsConfig(
    url: String,
    user: String,
    password: String,
    migrationsTable: String,
    migrationsLocations: List[String]
) derives ConfigReader

def readConfig[T](
    resourcePath: String
)(using ConfigReader[T], ClassTag[T]): T =
  ConfigSource.resources(resourcePath).loadOrThrow[T]

object SessionPool:
  def makePool[F[_]: Temporal: natchez.Trace: std.Console: Network](
      repositoryConfig: PostgresConfig
  ): Resource[F, Resource[F, Session[F]]] =
    Session.pooled[F](
      host = repositoryConfig.host,
      user = repositoryConfig.user,
      password = Some(repositoryConfig.password),
      database = repositoryConfig.database,
      max = repositoryConfig.max
    )
