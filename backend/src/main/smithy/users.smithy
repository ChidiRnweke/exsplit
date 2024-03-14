$version: "2"

namespace exsplit.spec

use alloy#simpleRestJson
use alloy.common#emailFormat
use alloy#uuidFormat


@simpleRestJson
service UserService {
    version: "1.0.0"
    operations: [Login, Register, Refresh]
}

@http(method: "POST", uri: "/register")
operation Register {
    input := {
        @required
        email: Email

        @required
        password: Password
    }
    errors: [RegistrationError]
}

@http(method: "POST", uri: "/login")
operation Login {
    input := {
        @required
        email: Email

        @required
        password: Password
    }
    output := {
        @required
        access_token: AccessToken
    }
    errors: [ValidationError]
}

@http(method: "POST", uri: "/refresh")
operation Refresh {
    input := {
        @required
        refresh_token: RefreshToken
    }
    output := {
        @required
        access_token: AccessToken
    }
    errors: [ValidationError, AuthError]
}


@emailFormat
string Email

@length(min:8)
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