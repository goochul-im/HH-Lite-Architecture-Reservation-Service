package kr.hhplus.be.server.application.reservation

import jakarta.transaction.Transactional
import kr.hhplus.be.server.application.reservation.dto.ReservationRequest
import kr.hhplus.be.server.auth.AuthService
import kr.hhplus.be.server.domain.reservation.Reservation
import kr.hhplus.be.server.domain.reservation.ReservationRepository
import kr.hhplus.be.server.domain.reservation.ReservationStatus
import org.springframework.stereotype.Service

@Service
class ReservationService(
    private val reservationRepository: ReservationRepository,
    private val tempReservationComponent: TempReservationComponent,
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
        tempReservationComponent.save(dto.date, saveEntity.id!!, saveEntity.seatNumber)
    }

    private fun isAvailableSeat(number: Int): Boolean {
        TODO()
    }

}
