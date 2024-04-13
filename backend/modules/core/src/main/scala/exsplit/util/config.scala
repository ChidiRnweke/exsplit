package exsplit.config

import pureconfig._
import pureconfig.generic.derivation.default._
import scala.reflect.ClassTag

case class AppConfig(
    auth: AuthConfig,
    postgres: PostgresConfig,
    migrations: MigrationsConfig
) derives ConfigReader

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
