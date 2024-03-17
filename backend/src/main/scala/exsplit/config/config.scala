package exsplit.config
import cats.Functor
import pureconfig._
import pureconfig.generic.derivation.default._
import scala.reflect.ClassTag
import org.checkerframework.checker.units.qual.m

trait AuthConfig[F[_]: Functor]:
  val secretKey: F[String]

case class RepositoryConfig(
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

def configToClassFromResource[T](
    resourcePath: String
)(using ConfigReader[T], ClassTag[T]): T =
  ConfigSource.resources(resourcePath).loadOrThrow[T]
