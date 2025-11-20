package kr.hhplus.be.server.reservation.service

import kr.hhplus.be.server.reservation.service.port.RedisReservationOperations
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisReservationOperationsImpl(
    private val redisTemplate: StringRedisTemplate,
) : RedisReservationOperations {

    override fun saveTempReservation(
        seatListKey: String,
        reserveKey: String,
        seatNumber: Int,
        timeoutSeconds: Long
    ) {
        redisTemplate.executePipelined { connection ->
            // 좌석 정보 저장 (TTL 포함)
            connection.setEx(
                seatListKey.toByteArray(),
                timeoutSeconds,
                seatNumber.toString().toByteArray()
            )
            // 날짜별 예약 좌석 Set에 추가
            connection.sAdd(
                reserveKey.toByteArray(),
                seatNumber.toString().toByteArray()
            )
            null
        }
    }

    override fun getTempReservedSeats(reserveKey: String): List<Int> {
        val numbers = redisTemplate.opsForSet().members(reserveKey)
        return numbers?.map { it.toInt() } ?: emptyList()
    }

    override fun deleteReservation(seatListKey: String): Boolean {
        return redisTemplate.delete(seatListKey)
    }

    override fun isReservationExists(seatListKey: String): Boolean {
        return redisTemplate.hasKey(seatListKey)
    }

    override fun removeFromReserveSet(reserveKey: String, seatNumber: Int) {
        redisTemplate.opsForSet().remove(reserveKey, seatNumber.toString())
    }
}
