package kr.hhplus.be.server.application.point

import kr.hhplus.be.server.auth.AuthService
import org.springframework.stereotype.Service

@Service
class PointService(
    private val authService: AuthService
) {

    fun pay(id: String, usePoint: Int) {
        val member = authService.getById(id)
        member.usePoint(usePoint)
    }

    fun charge(id: String, chargePoint: Int) {
        val member = authService.getById(id)
        member.chargePoint(chargePoint)
    }

}
