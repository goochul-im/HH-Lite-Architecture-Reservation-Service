package kr.hhplus.be.server.reservation.infrastructure

import kr.hhplus.be.server.exception.ResourceNotFoundException
import kr.hhplus.be.server.reservation.domain.Reservation
import kr.hhplus.be.server.reservation.port.ReservationRepository
import org.springframework.stereotype.Repository

@Repository
class ReservationRepositoryImpl(
    private val repository: ReservationJpaRepository
) : ReservationRepository {

    override fun save(reservation: Reservation): Reservation {
        return repository.save(ReservationEntity.from(reservation)).toDomain()
    }

    override fun getReservedSeatNumbers(concertId: Long): List<Int> {
        return repository.getReservedSeatNumbers(concertId)
    }

    override fun findReservationByIdAndReserver(id: Long, reserverId: String): Reservation {
        return repository.findReservationEntityByIdAndReserver_Id(id, reserverId)
            ?.toDomain()
            ?: throw ResourceNotFoundException("예약 id $id ,예약자 id $reserverId 찾기 실패")
    }
}
