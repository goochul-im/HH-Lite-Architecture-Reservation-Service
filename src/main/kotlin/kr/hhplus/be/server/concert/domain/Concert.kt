package kr.hhplus.be.server.concert.domain

import java.time.LocalDate

/**
 * 순수 도메인 Concert 객체
 */
class Concert(
    val id: Long? = null,
    val name: String,
    val date: LocalDate,
    val totalSeats: Int = 50
)
