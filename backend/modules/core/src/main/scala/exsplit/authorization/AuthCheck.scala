package exsplit.authorization

import exsplit.spec._
import cats.effect._
import cats.syntax.all._
import cats._
import exsplit.auth._
import exsplit.datamapper.circles.CirclesRepository
import exsplit.datamapper.user.UserMapper

/** Represents an error when a user is not authorized to access a resource. This
  * error is kept as a global variable such that it can be reused in multiple
  * places. It is important to keep this error message consistent across the
  * application.
  */
val forbiddenError = ForbiddenError("Access Denied.")

/** Provides a type alias for a function that retrieves the circles of a user
  * given the user's email. This is used for authorization, the email in the
  * claim is used to retrieve the circles of the user.
  */
type UserCircles = [F[_]] =>> F[Email] => F[List[String]]

/** Represents a type alias for a function that retrieves a circle ID from a
  * resource. The function is parameterized by the effect type F.
  */
type NotFoundCircleId = [F[_]] =>> F[Either[NotFoundError, String]]

/** Utility object that checks if a user is authorized to access a resource.
  */
object AuthCheck:
  /** The main authorization check function. This function checks if the user is
    * authorized to access a resource. It takes the circle ID of the resource
    * and the circles of the user. If the user is not authorized, a forbidden
    * error is raised.
    *
    * Each error is mapped to a forbidden error. This is done to keep the error
    * message consistent across the application. Smithy4s takes this error and
    * converts it to a 403 status code.
    *
    * @param email
    *   The email of the user, found in the JWT claim.
    * @param a
    *   The resource to be accessed.
    * @param circleFromUser
    *   A function that retrieves the circles of a user given the user's email.
    * @param extractorA
    *   A function that retrieves the circle ID of a resource.
    * @return
    *   A unit if the user is authorized, otherwise a forbidden error is raised.
    */
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

/** A companion object used to construct a specific instance of a function that
  * retrieves the circles of a user given the user's email. This is used for
  * authorization, the email in the claim is used to retrieve the circles of the
  * user.
  */
object UserCircleExtractor:
  /** Constructs a specific instance of a function that retrieves the circles of
    * a user given the user's email. This is used for authorization, the email
    * in the claim is used to retrieve the circles of the user.
    *
    * @param circlesRepo
    *   The repository that contains the circles of the user.
    * @param userRepo
    *   The repository that contains the user.
    * @return
    *   A function that retrieves the circles of a user given the user's email.
    */
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
