package kr.hhplus.be.server.reservation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.reservation.domain.Reservation
import kr.hhplus.be.server.reservation.dto.AvailableSeatsResponse
import kr.hhplus.be.server.reservation.dto.PayReservationResponse
import kr.hhplus.be.server.reservation.dto.ReservationRequest
import kr.hhplus.be.server.reservation.dto.ReservationResponse
import kr.hhplus.be.server.reservation.port.SeatFinder
import kr.hhplus.be.server.reservation.service.ReservationService
//import org.apache.catalina.User
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.User
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Tag(name = "예약", description = "콘서트 좌석 예약/조회/결제 관련 API")
@RestController
@RequestMapping("/api/reservation")
class ReservationController(
    private val reservationService: ReservationService,
    private val seatFinder: SeatFinder
) {

    @Operation(summary = "콘서트 좌석 예약", description = "특정 날짜의 콘서트 좌석을 예약합니다.")
    @ApiResponse(responseCode = "200", description = "예약 성공", content = [Content(schema = Schema(implementation = ReservationResponse::class))])
    @PostMapping
    fun make(
        @AuthenticationPrincipal user: User,
        @RequestBody req: ReservationMakeRequest
    ): ResponseEntity<ReservationResponse> {
        val reservation : Reservation = reservationService.make(
            ReservationRequest(
                req.date,
                user.username,
                req.seatNumber
            )
        )

        return ResponseEntity.ok(
            ReservationResponse(
                id = reservation.id!!,
                date = reservation.date,
                seatNumber = reservation.seatNumber
            )
        )
    }

    @Operation(summary = "예약 가능 좌석 조회", description = "선택한 날짜에 예약 가능한 좌석 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공", content = [Content(schema = Schema(implementation = AvailableSeatsResponse::class))])
    @GetMapping("/date")
    fun getAvailableSeatNumbers(
        @RequestParam date: LocalDate
    ): ResponseEntity<AvailableSeatsResponse> {
        val list = seatFinder.getAvailableSeat(date)
        return ResponseEntity.ok(
            AvailableSeatsResponse(
                date = date,
                seats = list
            )
        )
    }

    @Operation(summary = "임시 예약 결제", description = "임시 배정된 예약건을 결제 처리합니다.")
    @ApiResponse(responseCode = "200", description = "결제 성공", content = [Content(schema = Schema(implementation = PayReservationResponse::class))])
    @PostMapping("/pay/{id}")
    fun payTempReservation(
        @AuthenticationPrincipal user: User,
        @PathVariable("id") id: Long
    ): ResponseEntity<PayReservationResponse> {
        val reservation = reservationService.payReservation(id, user.username)
        return ResponseEntity.ok(
            PayReservationResponse(
                date = reservation.date,
                seatNumber = reservation.seatNumber
            )
        )
    }

}

@Schema(description = "좌석 예약 요청")
data class ReservationMakeRequest(
    @Schema(description = "예약 날짜", example = "2025-11-26")
    val date: LocalDate,
    @Schema(description = "좌석 번호", example = "12")
    val seatNumber: Int
)

