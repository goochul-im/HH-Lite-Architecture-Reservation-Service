package kr.hhplus.be.server.reservation.service

import kr.hhplus.be.server.auth.AuthService
import kr.hhplus.be.server.reservation.domain.Reservation
import kr.hhplus.be.server.reservation.domain.ReservationRepository
import kr.hhplus.be.server.reservation.domain.ReservationStatus
import kr.hhplus.be.server.reservation.dto.ReservationRequest
import kr.hhplus.be.server.reservation.port.TempReservationPort
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ReservationService(
    private val reservationRepository: ReservationRepository,
    private val tempReservationService: TempReservationPort,
    private val authService: AuthService
) {

    fun make(dto: ReservationRequest) {
        val reservation = Reservation(
            date = dto.date,
            seatNumber = dto.seatNumber,
            status = ReservationStatus.PENDING,
            reserver = authService.getById(dto.memberId)
        )

        val saveEntity = reservationRepository.save(reservation)
        tempReservationService.save(dto.date, saveEntity.id!!, saveEntity.seatNumber)
    }

    private fun isAvailableSeat(date: LocalDate): Boolean {
        val seatInPersistent = reservationRepository.getReservedSeatnumber(date)
        val seatInTemp = tempReservationService
        TODO()
    }

}
