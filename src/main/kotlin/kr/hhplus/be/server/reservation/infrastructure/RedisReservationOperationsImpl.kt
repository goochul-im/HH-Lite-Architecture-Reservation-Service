package kr.hhplus.be.server.reservation.infrastructure

import kr.hhplus.be.server.reservation.infrastructure.RedisReservationOperations
import org.redisson.api.RedissonClient
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RedisReservationOperationsImpl(
    private val redissonClient: RedissonClient,
) : RedisReservationOperations {

    override fun saveTempReservation(
        seatListKey: String,
        reserveKey: String,
        seatNumber: Int,
        timeoutSeconds: Long
    ) {
        // Redisson의 Batch 기능을 활용 (Pipeline과 동일한 효과)
        val batch = redissonClient.createBatch()

        // 1. String 데이터 저장 및 TTL 설정
        batch.getBucket<String>(seatListKey)
            .setAsync(seatNumber.toString(), Duration.ofSeconds(timeoutSeconds))

        // 2. Set 데이터에 좌석 번호 추가
        batch.getSet<String>(reserveKey)
            .addAsync(seatNumber.toString())

        // 비동기 명령어들을 한 번에 실행 (Network Round-trip 1회)
        batch.execute()
    }

    override fun getTempReservedSeats(reserveKey: String): List<Int> {
        // Redisson의 Set에서 데이터를 가져옴
        val set = redissonClient.getSet<String>(reserveKey)
        return set.map { it.toInt() }
    }

    override fun deleteReservation(seatListKey: String): Boolean {
        // delete()는 삭제 성공 시 true 반환
        return redissonClient.getBucket<String>(seatListKey).delete()
    }

    override fun isReservationExists(seatListKey: String): Boolean {
        return redissonClient.getBucket<String>(seatListKey).isExists
    }

    override fun removeFromReserveSet(reserveKey: String, seatNumber: Int) {
        redissonClient.getSet<String>(reserveKey).remove(seatNumber.toString())
    }

    override fun cleanUp() {
        redissonClient.keys.flushdb()
    }
}
