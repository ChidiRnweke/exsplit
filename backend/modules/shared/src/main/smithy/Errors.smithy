$version: "2"

namespace exsplit.spec

@error("client")
@httpError(400)
structure ValidationError {
    @required
    message: String
}

@error("client")
@httpError(400)
structure InvalidTokenError {
    @required
    message: String
}

@error("client")
@httpError(404)
structure NotFoundError {
    @required
    message: String
}

@error("client")
@httpError(401)
structure AuthError {
    @required
    message: String
}

@error("client")
@httpError(403)
structure ForbiddenError {}
