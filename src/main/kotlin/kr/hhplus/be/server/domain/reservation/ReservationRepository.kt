package kr.hhplus.be.server.domain.reservation

import org.springframework.data.jpa.repository.JpaRepository

interface ReservationRepository : JpaRepository<Reservation, Long>
