package kr.hhplus.be.server.reservation.domain

import kr.hhplus.be.server.concert.domain.Concert
import kr.hhplus.be.server.member.domain.Member

/**
 * 순수 도메인 객체
 */
class Reservation(
    val id: Long? = null,
    val concert: Concert,
    var seatNumber: Int,
    var status: ReservationStatus,
    var reserver: Member?
)

enum class ReservationStatus{

    PENDING, RESERVE, CANCEL

}
