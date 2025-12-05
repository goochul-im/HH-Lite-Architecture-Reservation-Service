package kr.hhplus.be.server.auth.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.hhplus.be.server.application.wating.service.port.WaitingQueuePort
import kr.hhplus.be.server.auth.exception.AccessDeniedException
import kr.hhplus.be.server.common.jwt.JwtProvider
import kr.hhplus.be.server.auth.security.SecurityConstant
import mu.KotlinLogging
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider,
    private val userDetailsService: CustomUserDetailsService,
    private val waitingQueuePort: WaitingQueuePort
) : OncePerRequestFilter() {

    private val log = KotlinLogging.logger { }
    private val matcher = AntPathMatcher()


    companion object {
        private const val ACCESS_TOKEN_HEADER = "Authorization"
        private const val WAITING_TOKEN_HEADER = "X-Waiting-Token"
        private const val TOKEN_PREFIX = "Bearer "
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return SecurityConstant.PUBLIC_URIS.any { matcher.match(it, path) }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val accessToken = resolveToken(request, ACCESS_TOKEN_HEADER)
        val waitingToken = resolveToken(request, WAITING_TOKEN_HEADER)
        val path = request.requestURI

        try {
            // 1. Access Token 검증 (대기열 진입 API 포함 모든 인증 필요 API)
            if (accessToken == null || !jwtProvider.validateToken(accessToken)) {
                throw AccessDeniedException("Access token is required or invalid.")
            }
            setAuthentication(accessToken) // 인증 정보 설정

            // 2. Waiting Token 검증
            val requiresActiveWaitingToken = !SecurityConstant.NO_WAITING_TOKEN_PATH.any{ matcher.match(it, path) }

            if (requiresActiveWaitingToken) {
                //현재 접속 리스트에 존재하는지
                val accessUserId = jwtProvider.getUserIdFromToken(accessToken)
                log.info { "accessUserId = $accessUserId, waitingToken = $waitingToken" }
                if (waitingToken == null || !waitingQueuePort.isEnteringKey(accessUserId)) {
                    throw AccessDeniedException("Waiting token is required or invalid.")
                }
                val waitingUserId = jwtProvider.getUserIdFromToken(waitingToken)
                // 두 토큰의 사용자 ID가 일치하는지 확인
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
