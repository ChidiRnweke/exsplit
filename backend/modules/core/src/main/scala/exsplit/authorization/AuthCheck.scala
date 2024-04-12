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

  def checkAuthorization[F[_]: MonadThrow, A](
      email: F[Email],
      a: A,
      circleFromUser: UserCircles[F],
      extractorA: A => NotFoundCircleId[F]
  ): F[Unit] =
    (extractorA(a), circleFromUser(email)).mapN:
      authCheck(_, _).map: res =>
        if res then () else forbiddenError.raiseError

  private def authCheck(
      circleA: Either[NotFoundError, String],
      userCircles: List[String]
  ): Either[ForbiddenError, Boolean] =
    circleA
      .map(a => userCircles.contains(a))
      .leftMap(_ => forbiddenError)

object UserCircleExtractor:
  def apply[F[_]: MonadThrow](
      circlesRepo: CirclesRepository[F],
      userRepo: UserMapper[F]
  ): UserCircles[F] =
    (email: F[Email]) => userToCircleIds(email, userRepo, circlesRepo)

  private def userToCircleIds[F[_]: MonadThrow](
      getEmail: F[Email],
      userRepo: UserMapper[F],
      circlesRepo: CirclesRepository[F]
  ): F[List[String]] =
    for
      email <- getEmail
      userEither <- userRepo.findUserByEmail(email)
      user <- userEither
        .leftMap(_ => forbiddenError)
        .liftTo[F]
      circles <- circlesRepo.listPrimaries(UserId(user.id))
    yield circles.map(_.id)
