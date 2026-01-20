package kr.hhplus.be.server.auth.security

object SecurityConstant {
    val PUBLIC_URIS = arrayOf(
        "/api/auth/**",
        "/api/concerts/**",
        "/api/ranking/**",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
    )

    val NO_WAITING_TOKEN_PATH = arrayOf(
        "/api/pay/**",
        "/api/point/**",
        "/api/wait/enter",
    )
}
