package kr.hhplus.be.server.application.point.service

import kr.hhplus.be.server.auth.AuthService
import org.springframework.stereotype.Service

@Service
class PointService(
    private val authService: AuthService
) {

    fun charge(id: String, chargePoint: Int) : Int {
        val member = authService.getById(id)
        member.chargePoint(chargePoint)
        return member.point
    }

    fun inquiry(id: String): Int {
        val member = authService.getById(id)
        return member.point
    }

}
