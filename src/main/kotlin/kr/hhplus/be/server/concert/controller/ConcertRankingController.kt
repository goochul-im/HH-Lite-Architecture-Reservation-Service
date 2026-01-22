package kr.hhplus.be.server.concert.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.concert.dto.ConcertRanking
import kr.hhplus.be.server.concert.dto.ConcertRankingResponse
import kr.hhplus.be.server.concert.port.ConcertRankingPort
import kr.hhplus.be.server.concert.service.ConcertRankingService
import kr.hhplus.be.server.reservation.dto.AvailableSeatsResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "콘서트 랭킹", description = "콘서트 랭킹 조회")
@RestController
@RequestMapping("/api/ranking/concert")
class ConcertRankingController(
    private val concertService: ConcertRankingPort
) {

    @Operation(summary = "콘서트 랭킹 조회", description = "콘서트 매진 랭킹을 조회합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공",
        content = [
            Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = ConcertRankingResponse::class))
            )
        ])
    @GetMapping
    fun getRanking(
        @RequestParam(required = false, defaultValue = "10") topN: Int
    ): ResponseEntity<List<ConcertRankingResponse>> {
        return ResponseEntity.ok(concertService.getRanking(topN))
    }

}
