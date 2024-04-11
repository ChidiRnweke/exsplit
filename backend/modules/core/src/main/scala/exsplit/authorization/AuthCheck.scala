package exsplit.authorization

import exsplit.spec._
import cats.effect._
import cats.syntax.all._
import cats._
import exsplit.auth._
val forbiddenError = ForbiddenError("Access Denied.")

object AuthCheck:

  def checkAuthorization[F[_], A](
      email: F[Email],
      a: A,
      circleFromUser: F[Email] => F[List[String]],
      extractorA: A => F[Either[NotFoundError, String]]
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
