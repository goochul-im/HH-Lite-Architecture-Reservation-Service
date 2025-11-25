package kr.hhplus.be.server.reservation.domain

import kr.hhplus.be.server.member.domain.Member
import kr.hhplus.be.server.member.infrastructure.MemberEntity
import kr.hhplus.be.server.reservation.infrastructure.ReservationStatus
import java.time.LocalDate

/**
 * 순수 도메인 객체
 */
class Reservation(
    val id: Long? = null,
    var date: LocalDate,
    var seatNumber: Int,
    var status: ReservationStatus,
    var reserver: Member?
)
