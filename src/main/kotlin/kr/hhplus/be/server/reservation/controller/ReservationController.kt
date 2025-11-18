package kr.hhplus.be.server.reservation.controller

import kr.hhplus.be.server.reservation.dto.ReservationRequest
import kr.hhplus.be.server.reservation.service.ReservationService
import org.apache.catalina.User
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/reservation")
class ReservationController(
    private val reservationService: ReservationService,

) {

    @PostMapping
    fun make(
        @AuthenticationPrincipal user: User,
        @RequestBody req: ReservationMakeRequest
    ): ResponseEntity<*> {
        reservationService.make(
            ReservationRequest(
                req.date,
                user.username,
                req.seatNumber
            )
        )

        return ResponseEntity.ok(
            mapOf(
                "date" to req.date,
                "seatNumber" to req.seatNumber
            )
        )
    }

    @GetMapping("/date")
    fun getAvailableSeatNumbers(
        @RequestParam date: LocalDate
    ): ResponseEntity<*> {
        val list = reservationService.getAvailableSeat(date)
        return ResponseEntity.ok(
            mapOf(
                "date" to date,
                "seats" to list
            )
        )
    }

    @PostMapping("/pay/{id}")
    fun payTempReservation(
        @AuthenticationPrincipal user: User,
        @PathVariable("id") id: Long
    ): ResponseEntity<*> {
        val reservation = reservationService.payReservation(id, user.username)
        return ResponseEntity.ok(
            mapOf(
                "date" to reservation.date,
                "seat" to reservation.seatNumber
            )
        )
    }

}

data class ReservationMakeRequest(
    val date: LocalDate,
    val seatNumber: Int
)
