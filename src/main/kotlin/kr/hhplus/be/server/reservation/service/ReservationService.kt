package kr.hhplus.be.server.reservation.service

import jakarta.transaction.Transactional
import kr.hhplus.be.server.concert.port.ConcertRankingPort
import kr.hhplus.be.server.concert.port.ConcertRepository
import kr.hhplus.be.server.concert.service.ConcertRankingService
import kr.hhplus.be.server.exception.DuplicateResourceException
import kr.hhplus.be.server.member.port.MemberRepository
import kr.hhplus.be.server.outbox.domain.AggregateType
import kr.hhplus.be.server.outbox.domain.EventType
import kr.hhplus.be.server.outbox.domain.OutboxMessage
import kr.hhplus.be.server.outbox.domain.OutboxStatus
import kr.hhplus.be.server.outbox.port.OutboxRepository
import kr.hhplus.be.server.reservation.domain.Reservation
import kr.hhplus.be.server.reservation.domain.ReservationStatus
import kr.hhplus.be.server.reservation.dto.ReservationRequest
import kr.hhplus.be.server.reservation.dto.TempReservationPayload
import kr.hhplus.be.server.reservation.port.ReservationRepository
import kr.hhplus.be.server.reservation.port.SeatFinder
import kr.hhplus.be.server.reservation.port.TempReservationPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.CacheEvict
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service

@Service
class ReservationService(
    private val reservationRepository: ReservationRepository,
    private val tempReservationService: TempReservationPort,
    private val memberRepository: MemberRepository,
    private val concertRepository: ConcertRepository,
    @param:Value("\${reservation.price}")
    private val price: Int,
    private val outboxRepository: OutboxRepository,
    private val seatFinder: SeatFinder,
    private val concertRankingPort: ConcertRankingPort
) {

    @Transactional
    @CacheEvict(value = ["availableSeats"], key = "#dto.concertId")
    fun make(dto: ReservationRequest): Reservation {
        if (!seatFinder.getAvailableSeats(dto.concertId).contains(dto.seatNumber)) {
            throw DuplicateResourceException("이미 예약되어있는 좌석입니다.")
        }

        val concert = concertRepository.findById(dto.concertId)
        val reservation = Reservation(
            concert = concert,
            seatNumber = dto.seatNumber,
            status = ReservationStatus.PENDING,
            reserver = memberRepository.findById(dto.memberId)
        )

        val saved: Reservation = try {
            reservationRepository.save(reservation)
        } catch (e: DataIntegrityViolationException) {
            throw DuplicateResourceException("이미 예약되어있는 좌석입니다.")
        }

        val outboxMessage = OutboxMessage(
            aggregateType = AggregateType.TEMP_RESERVATION,
            eventType = EventType.INSERT,
            payload = TempReservationPayload(saved.id!!, saved.concert.id!!, saved.seatNumber).toMap(),
            status = OutboxStatus.PENDING
        )

        outboxRepository.save(outboxMessage)
        concertRankingPort.checkAndMarkSoldOut(concert.id!!)
        return saved
    }

    @Transactional
    fun payReservation(reservationId: Long, userId: String): Reservation {
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
        try {
            reserver = memberRepository.saveAndFlush(reserver)
        } catch (e: ObjectOptimisticLockingFailureException) {
            throw DuplicateResourceException("이미 결제된 자리입니다. ${e.message}")
        }
        reservation.reserver = reserver

        reservation.status = ReservationStatus.RESERVE
        reservation = reservationRepository.save(reservation)
        tempReservationService.delete(reservationId)

        return reservation
    }
}
