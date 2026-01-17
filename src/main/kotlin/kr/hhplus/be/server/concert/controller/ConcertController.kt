package kr.hhplus.be.server.concert.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.concert.dto.ConcertCreateRequest
import kr.hhplus.be.server.concert.dto.ConcertListResponse
import kr.hhplus.be.server.concert.dto.ConcertResponse
import kr.hhplus.be.server.concert.service.ConcertService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "콘서트", description = "콘서트 조회/등록 관련 API")
@RestController
@RequestMapping("/api/concerts")
class ConcertController(
    private val concertService: ConcertService
) {

    @Operation(summary = "콘서트 등록", description = "새로운 콘서트를 등록합니다.")
    @PostMapping
    fun create(@RequestBody request: ConcertCreateRequest): ResponseEntity<ConcertResponse> {
        val concert = concertService.create(request)
        return ResponseEntity.ok(
            ConcertResponse(
                id = concert.id!!,
                name = concert.name,
                date = concert.date,
                totalSeats = concert.totalSeats
            )
        )
    }

    @Operation(summary = "콘서트 단건 조회", description = "콘서트 ID로 조회합니다.")
    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): ResponseEntity<ConcertResponse> {
        val concert = concertService.findById(id)
        return ResponseEntity.ok(
            ConcertResponse(
                id = concert.id!!,
                name = concert.name,
                date = concert.date,
                totalSeats = concert.totalSeats
            )
        )
    }

    @Operation(summary = "예약 가능 콘서트 목록 조회", description = "오늘 이후 예약 가능한 콘서트 목록을 조회합니다.")
    @GetMapping
    fun findAvailableConcerts(): ResponseEntity<ConcertListResponse> {
        val concerts = concertService.findAvailableConcerts()
        return ResponseEntity.ok(
            ConcertListResponse(
                concerts = concerts.map {
                    ConcertResponse(
                        id = it.id!!,
                        name = it.name,
                        date = it.date,
                        totalSeats = it.totalSeats
                    )
                }
            )
        )
    }
}
