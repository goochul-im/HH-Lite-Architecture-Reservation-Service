package kr.hhplus.be.server.auth.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.hhplus.be.server.auth.UserStatus
import kr.hhplus.be.server.auth.exception.AccessDeniedException
import kr.hhplus.be.server.common.jwt.JwtProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider,
    private val userDetailsService: CustomUserDetailsService,
) : OncePerRequestFilter() {

    companion object {
        private const val ACCESS_TOKEN_HEADER = "Authorization"
        private const val WAITING_TOKEN_HEADER = "X-Waiting-Token"
        private const val TOKEN_PREFIX = "Bearer "
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val accessToken = resolveToken(request, ACCESS_TOKEN_HEADER)
        val waitingToken = resolveToken(request, WAITING_TOKEN_HEADER)
        val path = request.servletPath
        val matcher = AntPathMatcher()

        // 토큰이 필요 없는 경로 (로그인, 회원가입 등)
        if (matcher.match("/api/auth/**", path)) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            // 1. Access Token 검증 (대기열 진입 API 포함 모든 인증 필요 API)
            if (accessToken == null || !jwtProvider.validateToken(accessToken)) {
                throw AccessDeniedException("Access token is required or invalid.")
            }
            setAuthentication(accessToken) // 인증 정보 설정

            // 2. Waiting Token 검증 (대기열 진입 API를 제외한 나머지)
            val requiresActiveWaitingToken = !matcher.match("/api/wait/enter", path)

            if (requiresActiveWaitingToken) {
                if (waitingToken == null || !jwtProvider.validateToken(waitingToken)) {
                    throw AccessDeniedException("Waiting token is required or invalid.")
                }

                // Waiting Token의 상태가 ACTIVE인지 확인
                val status = jwtProvider.getStatusFromToken(waitingToken)
                if (status != UserStatus.ACTIVE) {
                    throw AccessDeniedException("User is not in ACTIVE status.")
                }

                // 두 토큰의 사용자 ID가 일치하는지 확인
                val accessUserId = jwtProvider.getUserIdFromToken(accessToken)
                val waitingUserId = jwtProvider.getUserIdFromToken(waitingToken)
                if (accessUserId != waitingUserId) {
                    throw AccessDeniedException("Token user ID mismatch.")
                }
            }

            filterChain.doFilter(request, response)
        } catch (e: Exception) {
            SecurityContextHolder.clearContext()
            throw e
        }
    }

    private fun resolveToken(request: HttpServletRequest, header: String): String? {
        val bearerToken = request.getHeader(header)
        return if (bearerToken != null && bearerToken.startsWith(TOKEN_PREFIX)) {
            bearerToken.substring(TOKEN_PREFIX.length)
        } else {
            null
        }
    }

    private fun setAuthentication(token: String) {
        val userId = jwtProvider.getUserIdFromToken(token)
        val userDetails = userDetailsService.loadUserById(userId)
        val authentication = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
        SecurityContextHolder.getContext().authentication = authentication
    }
}
