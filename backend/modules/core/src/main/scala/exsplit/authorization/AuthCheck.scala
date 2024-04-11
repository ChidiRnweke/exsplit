package exsplit.authorization

import exsplit.spec._
import cats.effect._
import cats.syntax.all._
import cats._
import exsplit.auth._
import exsplit.datamapper.circles.CirclesRepository
import exsplit.datamapper.user.UserMapper

val forbiddenError = ForbiddenError("Access Denied.")
type UserCircles = [F[_]] =>> F[Email] => F[List[String]]
type NotFoundCircleId = [F[_]] =>> F[Either[NotFoundError, String]]

object AuthCheck:

  def checkAuthorization[F[_], A](
      email: F[Email],
      a: A,
      circleFromUser: UserCircles[F],
      extractorA: A => NotFoundCircleId[F]
  )(using f: MonadThrow[F]): F[Unit] =
    for
      circleA <- extractorA(a)
      userCircles <- circleFromUser(email)
      res <- f.fromEither(authCheck(circleA, userCircles))
      _ <- if res then f.unit else f.raiseError(forbiddenError)
    yield ()

  private def authCheck(
      circleA: Either[NotFoundError, String],
      userCircles: List[String]
  ): Either[ForbiddenError, Boolean] =
    val res =
      for a <- circleA
      yield userCircles.contains(a)
    res.leftMap(_ => forbiddenError)

object UserCircleExtractor:
  def apply[F[_]: MonadThrow](
      circlesRepo: CirclesRepository[F],
      userRepo: UserMapper[F]
  ): UserCircles[F] =
    (email: F[Email]) => userToCircleIds(email, userRepo, circlesRepo)

  private def userToCircleIds[F[_]: MonadThrow](
      email: F[Email],
      userRepo: UserMapper[F],
      circlesRepo: CirclesRepository[F]
  ): F[List[String]] =
    for
      email_ <- email
      userEither <- userRepo.findUserByEmail(email_)
      user <- userEither
        .leftMap(_ => forbiddenError)
        .liftTo[F]
      circles <- circlesRepo.listPrimaries(UserId(user.id))
    yield circles.map(_.id)
