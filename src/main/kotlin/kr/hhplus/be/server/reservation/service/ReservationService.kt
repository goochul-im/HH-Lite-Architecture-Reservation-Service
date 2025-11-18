package kr.hhplus.be.server.reservation.service

import jakarta.transaction.Transactional
import kr.hhplus.be.server.auth.AuthService
import kr.hhplus.be.server.reservation.domain.Reservation
import kr.hhplus.be.server.reservation.domain.ReservationRepository
import kr.hhplus.be.server.reservation.domain.ReservationStatus
import kr.hhplus.be.server.reservation.dto.ReservationRequest
import kr.hhplus.be.server.reservation.port.TempReservationPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ReservationService(
    private val reservationRepository: ReservationRepository,
    private val tempReservationService: TempReservationPort,
    private val authService: AuthService,
    @param:Value("\${reservation.price}")
    private val price: Int,
) {

    fun make(dto: ReservationRequest) {

        if (!getAvailableSeat(dto.date).contains(dto.seatNumber)) {
            throw RuntimeException("이미 예약되어있는 좌석입니다.")
        }

        val reservation = Reservation(
            date = dto.date,
            seatNumber = dto.seatNumber,
            status = ReservationStatus.PENDING,
            reserver = authService.getById(dto.memberId)
        )

        val saveEntity = reservationRepository.save(reservation)
        tempReservationService.save(dto.date, saveEntity.id!!, saveEntity.seatNumber)
    }

    fun getAvailableSeat(date: LocalDate): List<Int> {
        val seatInPersistent = reservationRepository.getReservedSeatnumber(date).toSet()
        val seatInTemp = tempReservationService.getTempReservation(date).toSet()

        val availableSeat = (1..50).filter { !seatInPersistent.contains(it) && !seatInTemp.contains(it) }

        return availableSeat
    }

    @Transactional
    fun payReservation(reservationId: Long, userId: String) : Reservation {
        if (!tempReservationService.isValidReservation(reservationId)) {
            throw RuntimeException("예약된 자리가 아닙니다.")
        }
        val reserver = authService.getById(userId)
        val reservation =
            reservationRepository.findReservationByIdAndReserver(reservationId, reserver) ?: throw RuntimeException("예약을 찾을 수 없습니다")
        if (reservation.status != ReservationStatus.PENDING) {
            throw RuntimeException("예약한 자리가 아닙니다.")
        }

        reserver.usePoint(price)
        reservation.status = ReservationStatus.RESERVE
        tempReservationService.delete(reservationId)

        return reservation
    }

}
