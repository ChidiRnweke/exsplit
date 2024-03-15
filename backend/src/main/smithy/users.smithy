$version: "2"

namespace exsplit.spec

use alloy#simpleRestJson
use alloy#uuidFormat
use alloy.common#emailFormat

@simpleRestJson
@httpBearerAuth
service UserService {
    version: "1.0.0"
    operations: [
        Login
        Register
        Refresh
    ]
}

@http(method: "POST", uri: "/register")
@auth([])
operation Register {
    input := {
        @required
        email: Email

        @required
        password: Password
    }

    errors: [
        RegistrationError
    ]
}

@http(method: "POST", uri: "/login")
@auth([])
operation Login {
    input := {
        @required
        email: Email

        @required
        password: Password
    }

    output := {
        @required
        accessToken: AccessToken
        @required
        refreshToken: RefreshToken
    }

    errors: [
        ValidationError
    ]
}

@http(method: "POST", uri: "/refresh")
operation Refresh {

    output := {
        @required
        accessToken: AccessToken
    }

    errors: [
        ValidationError,
        AuthError,
        InvalidTokenError
    ]
}

@emailFormat
string Email

@length(min: 8)
string Password

string AccessToken

string RefreshToken

@error("client")
@httpError(400)
structure RegistrationError {
    @required
    message: String
}

@uuidFormat
string UserId

structure User {
    @required
    id: UserId

    @required
    email: Email
}
