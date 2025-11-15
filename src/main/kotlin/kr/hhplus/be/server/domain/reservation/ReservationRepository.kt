package kr.hhplus.be.server.domain.reservation

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface ReservationRepository : JpaRepository<Reservation, Long> {

    @Query("select r from Reservation r " +
            "where r.seatNumber = :seat and " +
            "r.date = :date and " +
            "r.status = 'RESERVE'")
    fun getAvailableSeatNumber(@Param("seat") seatNumber: Int, @Param("date") date: LocalDate)

}
