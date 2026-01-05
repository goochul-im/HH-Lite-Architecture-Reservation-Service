package kr.hhplus.be.server.reservation.service

import kr.hhplus.be.server.common.port.DistributeLockManager
import kr.hhplus.be.server.reservation.domain.Reservation
import kr.hhplus.be.server.reservation.dto.ReservationRequest
import org.springframework.stereotype.Component

@Component
class ReservationFacade(
    private val reservationService: ReservationService,
    private val lockManager: DistributeLockManager
) {

    fun makeWithLock(dto: ReservationRequest): Reservation {
        // 날짜와 좌석번호로 유니크한 키 생성
        val lockKey = "LOCK:RESERVATION:${dto.date}:${dto.seatNumber}"

        return lockManager.runWithLock(lockKey) {
            reservationService.make(dto)
        }
    }

}
