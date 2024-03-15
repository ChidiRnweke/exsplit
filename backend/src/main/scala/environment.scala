package Environment
import cats.Functor

trait AuthConfig[F[_]: Functor]:
  val secretKey: F[String]

trait RepositoryConfig[F[_]: Functor]:
  val dbUrl: F[String]
  val dbUser: F[String]
  val dbPassword: F[String]
  val dbDriver: F[String]
