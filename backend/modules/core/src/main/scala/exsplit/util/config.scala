package exsplit.config

import pureconfig._
import pureconfig.generic.derivation.default._
import scala.reflect.ClassTag

/** Represents the application configuration. It derives the ConfigReader
  * typeclass to read the configuration from the resource file from PureConfig
  * library.
  *
  * @param auth
  *   The authentication configuration.
  * @param postgres
  *   The PostgreSQL database configuration.
  * @param migrations
  *   The database migrations configuration.
  */
case class AppConfig(
    auth: AuthConfig,
    postgres: PostgresConfig,
    migrations: MigrationsConfig
) derives ConfigReader

/** Represents the authentication configuration. It derives the ConfigReader
  * typeclass to read the configuration from the resource file from PureConfig
  * library.
  *
  * @param secretKey
  *   The secret key used for authentication.
  */
case class AuthConfig(
    secretKey: String
) derives ConfigReader

/** Represents the PostgreSQL database configuration. It derives the
  * ConfigReader typeclass to read the configuration from the resource file from
  * PureConfig
  *
  * @param host
  *   The host of the PostgreSQL database.
  * @param user
  *   The username for connecting to the database.
  * @param password
  *   The password for connecting to the database.
  * @param database
  *   The name of the database.
  * @param max
  *   The maximum number of connections in the connection pool.
  */
case class PostgresConfig(
    host: String,
    user: String,
    password: String,
    database: String,
    max: Int
) derives ConfigReader

/** Represents the database migrations configuration. It derives the
  * ConfigReader typeclass to read the configuration from the resource file from
  * PureConfig library.
  *
  * @param url
  *   The URL of the database.
  * @param user
  *   The username for connecting to the database.
  * @param password
  *   The password for connecting to the database.
  * @param migrationsTable
  *   The name of the table used for tracking migrations.
  * @param migrationsLocations
  *   The locations of the migration scripts.
  */
case class MigrationsConfig(
    url: String,
    user: String,
    password: String,
    migrationsTable: String,
    migrationsLocations: List[String]
) derives ConfigReader

/** Reads the configuration from the specified resource path. If the
  * configuration cannot be read, it throws an exception. This is desirable
  * because the application cannot run without the configuration.
  *
  * @param resourcePath
  *   The path to the configuration resource.
  * @tparam T
  *   The type of the configuration to read.
  * @return
  *   The configuration object.
  */
def readConfig[T](
    resourcePath: String
)(using ConfigReader[T], ClassTag[T]): T =
  ConfigSource.resources(resourcePath).loadOrThrow[T]
