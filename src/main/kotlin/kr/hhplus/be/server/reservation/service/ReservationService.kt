package kr.hhplus.be.server.reservation.service

import jakarta.transaction.Transactional
import kr.hhplus.be.server.member.infrastructure.MemberEntity
import kr.hhplus.be.server.member.port.MemberRepository
import kr.hhplus.be.server.outbox.domain.AggregateType
import kr.hhplus.be.server.outbox.domain.EventType
import kr.hhplus.be.server.outbox.domain.OutboxMessage
import kr.hhplus.be.server.outbox.domain.OutboxStatus
import kr.hhplus.be.server.outbox.port.OutboxRepository
import kr.hhplus.be.server.reservation.domain.Reservation
import kr.hhplus.be.server.reservation.infrastructure.ReservationEntity
import kr.hhplus.be.server.reservation.infrastructure.ReservationStatus
import kr.hhplus.be.server.reservation.dto.ReservationRequest
import kr.hhplus.be.server.reservation.dto.TempReservationPayload
import kr.hhplus.be.server.reservation.port.ReservationRepository
import kr.hhplus.be.server.reservation.port.TempReservationPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ReservationService(
    private val reservationRepository: ReservationRepository,
    private val tempReservationService: TempReservationPort,
    private val memberRepository: MemberRepository,
    @param:Value("\${reservation.price}")
    private val price: Int,
    private val outboxRepository: OutboxRepository
) {

    @Transactional
    fun make(dto: ReservationRequest) : Reservation {

        if (!getAvailableSeat(dto.date).contains(dto.seatNumber)) {
            throw RuntimeException("이미 예약되어있는 좌석입니다.")
        }

        val reservation = Reservation(
            date = dto.date,
            seatNumber = dto.seatNumber,
            status = ReservationStatus.PENDING,
            reserver = memberRepository.findById(dto.memberId)
        )

        val save : Reservation = reservationRepository.save(reservation)

        val outboxMessage = OutboxMessage(
            aggregateType = AggregateType.TEMP_RESERVATION,
            eventType = EventType.INSERT,
            payload = TempReservationPayload(save.id!!, save.date, save.seatNumber).toMap(),
            status = OutboxStatus.PENDING
        )

        outboxRepository.save(outboxMessage)
        return save
    }

    fun getAvailableSeat(date: LocalDate): List<Int> {
        val seatInPersistent = reservationRepository.getReservedSeatNumber(date).toSet()
        val seatInTemp = tempReservationService.getTempReservation(date).toSet()

        val availableSeat = (1..50).filter { !seatInPersistent.contains(it) && !seatInTemp.contains(it) }

        return availableSeat
    }

    @Transactional
    fun payReservation(reservationId: Long, userId: String) : Reservation {
        if (!tempReservationService.isValidReservation(reservationId)) {
            throw RuntimeException("예약된 자리가 아닙니다.")
        }
        var reserver = memberRepository.findById(userId)
        var reservation =
            reservationRepository.findReservationByIdAndReserver(reservationId, reserver.id!!)
        if (reservation.status != ReservationStatus.PENDING) {
            throw RuntimeException("예약한 자리가 아닙니다.")
        }

        reserver.usePoint(price)
        reserver = memberRepository.save(reserver)
        reservation.reserver = reserver

        reservation.status = ReservationStatus.RESERVE
        reservation = reservationRepository.save(reservation)
        tempReservationService.delete(reservationId)

        return reservation
    }

}
