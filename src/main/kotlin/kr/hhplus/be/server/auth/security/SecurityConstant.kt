package kr.hhplus.be.server.auth.security

object SecurityConstant {
    val PUBLIC_URIS = arrayOf(
        "/api/auth/**",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
    )

    val NO_WAITING_TOKEN_PATH = arrayOf(
        "/api/pay/**",
        "/api/wait/enter",
    )
}
