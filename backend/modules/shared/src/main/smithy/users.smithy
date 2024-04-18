$version: "2"

namespace exsplit.spec

use alloy#simpleRestJson
use alloy.common#emailFormat

@simpleRestJson
service UserService {
    version: "1.0.0"
    operations: [
        Login
        Register
        Refresh
    ]
    errors: [
        ValidationError
    ]
}

@http(method: "POST", uri: "/api/auth/register")
operation Register {
    input: UserCredentials

    output := {
        @required
        userId: String

        @required
        refreshToken: RefreshToken

        @required
        accessToken: AccessToken
    }

    errors: [
        RegistrationError
    ]
}

@http(method: "POST", uri: "/api/auth/login")
operation Login {
    input: UserCredentials

    output := {
        @required
        accessToken: AccessToken

        @required
        refreshToken: RefreshToken
    }
}

@http(method: "POST", uri: "/api/auth/refresh")
operation Refresh {
    input := {
        @required
        refreshToken: RefreshToken
    }

    output := {
        @required
        accessToken: AccessToken
    }

    errors: [
        AuthError
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

string UserId

structure UserCredentials {
    @required
    email: Email

    @required
    password: Password
}
