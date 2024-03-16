package exsplit.config
import cats.Functor
import pureconfig._
import pureconfig.generic.derivation.default._
import scala.reflect.ClassTag

trait AuthConfig[F[_]: Functor]:
  val secretKey: F[String]

trait RepositoryConfig[F[_]: Functor]:
  val dbUrl: F[String]
  val dbUser: F[String]
  val dbPassword: F[String]
  val dbDriver: F[String]

case class DatabaseConfig(
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
