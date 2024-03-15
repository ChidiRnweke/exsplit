$version: "2"

namespace exsplit.spec

use alloy#simpleRestJson
use alloy#uuidFormat
use alloy.common#emailFormat

@simpleRestJson
service UserService {
    version: "1.0.0"
    operations: [
        Login
        Register
        Refresh
    ]
}

@http(method: "POST", uri: "/auth/register")
operation Register {
    input: UserCredentials

    errors: [
        RegistrationError
    ]
}

@http(method: "POST", uri: "/auth/login")
operation Login {
    input: UserCredentials

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

@http(method: "POST", uri: "/auth/refresh")
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

structure PublicUserData {
    @required
    id: UserId

    @required
    email: Email
}

structure UserCredentials {
    @required
    email: Email

    @required
    password: Password
}
