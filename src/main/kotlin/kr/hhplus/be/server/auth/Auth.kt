package kr.hhplus.be.server.auth

data class Tokens(
    val accessToken: String,
    val waitingToken: String? = null,
)

enum class UserStatus {
    WAIT,
    ACTIVE,
}
