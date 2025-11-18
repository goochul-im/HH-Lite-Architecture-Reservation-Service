package kr.hhplus.be.server.application.point.service

import kr.hhplus.be.server.auth.AuthService
import org.springframework.stereotype.Service

@Service
class PointService(
    private val authService: AuthService
) {

    fun pay(id: String, usePoint: Int) : Int {
        val member = authService.getById(id)
        member.usePoint(usePoint)
        return member.point
    }

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
