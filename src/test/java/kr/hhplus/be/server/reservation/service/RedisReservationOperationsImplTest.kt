package kr.hhplus.be.server.reservation.service

import kr.hhplus.be.server.TestcontainersConfiguration
import kr.hhplus.be.server.reservation.infrastructure.RedisReservationOperations
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.StringRedisTemplate

@SpringBootTest
@Import(TestcontainersConfiguration::class)
class RedisReservationOperationsImplTest {

    @Autowired
    private lateinit var redisReservationOperations: RedisReservationOperations

    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    @BeforeEach
    fun setUp() {
        redisTemplate.connectionFactory?.connection?.flushAll()
    }

    @Test
    fun `임시 예약을 저장하고 해당 좌석이 예약 목록에 존재하는지 확인할 수 있다`() {
        // given
        val seatListKey = "seat_list_key:1"
        val reserveKey = "reserve_key:20250101"
        val seatNumber = 10
        val timeoutSeconds = 60L

        // when
        redisReservationOperations.saveTempReservation(seatListKey, reserveKey, seatNumber, timeoutSeconds)

        // then
        val savedSeats = redisReservationOperations.getTempReservedSeats(reserveKey)
        assertThat(savedSeats).contains(seatNumber)

        val isExists = redisReservationOperations.isReservationExists(seatListKey)
        assertThat(isExists).isTrue()
    }

    @Test
    fun `예약 정보를 삭제하면 존재하지 않음을 확인할 수 있다`() {
        // given
        val seatListKey = "seat_list_key:2"
        val reserveKey = "reserve_key:20250101"
        val seatNumber = 20
        val timeoutSeconds = 60L

        redisReservationOperations.saveTempReservation(seatListKey, reserveKey, seatNumber, timeoutSeconds)

        // when
        val deleted = redisReservationOperations.deleteReservation(seatListKey)

        // then
        assertThat(deleted).isTrue()
        val isExists = redisReservationOperations.isReservationExists(seatListKey)
        assertThat(isExists).isFalse()
    }

    @Test
    fun `특정 예약 키에서 좌석 번호를 제거할 수 있다`() {
        // given
        val seatListKey = "seat_list_key:3"
        val reserveKey = "reserve_key:20250101"
        val seatNumber = 30
        val timeoutSeconds = 60L

        redisReservationOperations.saveTempReservation(seatListKey, reserveKey, seatNumber, timeoutSeconds)

        // when
        redisReservationOperations.removeFromReserveSet(reserveKey, seatNumber)

        // then
        val reservedSeats = redisReservationOperations.getTempReservedSeats(reserveKey)
        assertThat(reservedSeats).doesNotContain(seatNumber)
    }

    @Test
    fun `만료 시간이 지나면 예약 정보가 사라지는지 확인 (TTL 설정 검증)`() {
        // given
        val seatListKey = "seat_list_key:ttl"
        val reserveKey = "reserve_key:ttl"
        val seatNumber = 99
        val timeoutSeconds = 1L // 1 second

        // when
        redisReservationOperations.saveTempReservation(seatListKey, reserveKey, seatNumber, timeoutSeconds)

        // then
        // Check TTL is set (approximate)
        val ttl = redisTemplate.getExpire(seatListKey)
        assertThat(ttl).isGreaterThan(0)
        assertThat(ttl).isLessThanOrEqualTo(timeoutSeconds)
    }
}
