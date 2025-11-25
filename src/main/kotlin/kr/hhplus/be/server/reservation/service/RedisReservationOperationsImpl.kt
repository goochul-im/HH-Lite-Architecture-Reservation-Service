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
            val seatListKeyBytes = seatListKey.toByteArray()
            val reserveKeyBytes = reserveKey.toByteArray()
            val seatNumberBytes = seatNumber.toString().toByteArray()

            // 1. setEx 대체: stringCommands().setEx() , String
            connection.stringCommands().setEx(
                seatListKeyBytes,
                timeoutSeconds,
                seatNumberBytes
            )

            // 2. sAdd 대체: setCommands().sAdd() , Set
            connection.setCommands().sAdd(
                reserveKeyBytes,
                seatNumberBytes
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
