package kr.hhplus.be.server.common.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import kr.hhplus.be.server.auth.UserStatus
import kr.hhplus.be.server.auth.exception.InvalidTokenException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtProvider(
    @Value("\${jwt.secret}") private val secret: String,
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    private val accessTokenExpiration = 1000L * 60 * 60 // 1 hour
    private val waitingTokenExpiration = 1000L * 60 * 10 // 10 minutes

    fun createAccessToken(userId: String): String {
        return createToken(userId, accessTokenExpiration, null)
    }

    fun createWaitingToken(userId: String, status: UserStatus): String {
        val claims = mapOf("status" to status.name)
        return createToken(userId, waitingTokenExpiration, claims)
    }

    private fun createToken(subject: String, expiration: Long, claims: Map<String, Any>?): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)
        val builder = Jwts.builder()
            .subject(subject)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)

        claims?.let {
            builder.claims(it)
        }

        return builder.compact()
    }

    fun validateToken(token: String): Boolean {
        try {
            getClaims(token)
            return true
        } catch (e: Exception) {
            throw InvalidTokenException("Invalid token")
        }
    }

    fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    fun getUserIdFromToken(token: String): String {
        return getClaims(token).subject
    }

    fun getStatusFromToken(token: String): UserStatus {
        val status = getClaims(token)["status"] as? String ?: throw InvalidTokenException("Status not found in token")
        return UserStatus.valueOf(status)
    }
}
