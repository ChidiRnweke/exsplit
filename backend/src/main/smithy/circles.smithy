$version: "2"

namespace exsplit.spec
use alloy#simpleRestJson
use alloy#uuidFormat

@simpleRestJson
service CirclesService {
    operations: [GetCircles, AddUserToCircle, CreateCircle, UpdateCircle, DeleteCircle, ListCircleMembers]
    errors: [AuthError, ForbiddenError]
}

@readonly
@http(method: "GET", uri: "/users/{user_id}/circles")
operation GetCircles {
    input := {
        @required
        @httpLabel
        user_id: UserId
    }
    output := {
        @required
        circles: Circles
    }
}

@http(method: "POST", uri: "/users/{user_id}/circles")
operation CreateCircle {
    input := {
        @required
        @httpLabel
        user_id: UserId
        @required
        name: String
        description: String
    }
}

@idempotent
@http(method: "PUT", uri: "/users/{user_id}/circles/{circle_id}")
operation UpdateCircle {
    input := {
        @required
        @httpLabel
        user_id: UserId
        @required
        @httpLabel
        circle_id: CircleId
        @required
        name: String
        description: String
    }
}

@idempotent
@http(method: "DELETE", uri: "/users/{user_id}/circles/{circle_id}")
operation DeleteCircle {
    input := {
        @required
        @httpLabel
        user_id: UserId
        @required
        @httpLabel
        circle_id: CircleId
    }

}

@idempotent
@http(method: "PUT", uri: "/circles/{circle_id}/users")
operation AddUserToCircle {
    input := {
        @required
        user_id: UserId
        @required
        display_name: String
        @required
        @httpLabel
        circle_id: CircleId
    }
}

@readonly
@http(method: "GET", uri: "/circles/{circle_id}/users")
operation ListCircleMembers {
    input := {
        @required
        @httpLabel
        circle_id: CircleId
    }
    output := {
        @required
        members: Members
    }
}



list Circles {
    member: Circle
}

@uuidFormat
string CircleId

structure Circle {
    @required
    id: CircleId
    @required
    name: String
    description: String
    @required
    members: Members
}

list Members {
    member: CircleMember
}

structure CircleMember {
    @required
    user_id: String
    @required
    display_name: String
}

