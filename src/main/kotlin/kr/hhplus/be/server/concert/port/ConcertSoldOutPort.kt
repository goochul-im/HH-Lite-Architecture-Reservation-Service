package kr.hhplus.be.server.concert.port

import kr.hhplus.be.server.concert.dto.ConcertRanking

interface ConcertSoldOutPort {

    fun markSoldOut(concertId: Long, timestamp: Long)

    fun isSoldOut(concertId: Long): Boolean

    fun getTopN(n: Int): List<ConcertRanking>

    fun getRank(concertId: Long): ConcertRanking
}
