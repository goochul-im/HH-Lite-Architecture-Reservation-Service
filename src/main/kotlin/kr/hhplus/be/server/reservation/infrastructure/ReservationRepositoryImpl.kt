package kr.hhplus.be.server.reservation.infrastructure

import kr.hhplus.be.server.exception.ResourceNotFoundException
import kr.hhplus.be.server.member.domain.Member
import kr.hhplus.be.server.member.infrastructure.MemberEntity
import kr.hhplus.be.server.reservation.domain.Reservation
import kr.hhplus.be.server.reservation.port.ReservationRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class ReservationRepositoryImpl(
    private val repository: ReservationJpaRepository
) : ReservationRepository {

    override fun save(reservation: Reservation): Reservation {
        return repository.save(ReservationEntity.from(reservation)).toDomain()
    }

    override fun getReservedSeatNumber(date: LocalDate) : List<Int> {
        return repository.getReservedSeatNumber(date)
    }

    override fun findReservationByIdAndReserver(id: Long, reserverId: String) : Reservation{
        return repository.findReservationEntityByIdAndReserver_Id(id, reserverId)
            ?.toDomain()
            ?: throw ResourceNotFoundException("예약 id $id ,예약자 id $reserverId 찾기 실패")
    }
}
