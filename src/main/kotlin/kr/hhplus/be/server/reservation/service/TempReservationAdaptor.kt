package kr.hhplus.be.server.reservation.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import kr.hhplus.be.server.reservation.TempReservationConstant
import kr.hhplus.be.server.reservation.domain.ReservationRepository
import kr.hhplus.be.server.reservation.domain.ReservationStatus
import kr.hhplus.be.server.reservation.port.TempReservationPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate

@Component
class TempReservationAdaptor (
    private val redisTemplate: StringRedisTemplate,
    private val reservationRepository: ReservationRepository,
) : TempReservationPort {

    @Value("\${reservation.pending-timeout-seconds}")
    private val pendingTimeoutSeconds: Long = 300L
    private val PENDING_TIMEOUT: Duration
        get() = Duration.ofSeconds(pendingTimeoutSeconds)

    override fun save(date: LocalDate, reservationId: Long, seatNumber: Int) {

        val seatListKey = "${TempReservationConstant.TEMP_RESERVATIONS}$reservationId" // 임시 예약된 좌석 전체, 만료되면 리스너에서 감지
        val reserveKey = "${TempReservationConstant.CHECK_SEAT}$date" // 현재 날짜에 임시 예약된 좌석 번호들 체크하는 용도

        redisTemplate.execute{ // 레디스 트랜잭션
            it.multi()

            redisTemplate.opsForValue().set(seatListKey, seatNumber.toString())
            redisTemplate.opsForSet().add(reserveKey, seatNumber.toString())

            redisTemplate.expire(seatListKey, PENDING_TIMEOUT)

            it.exec()
        }

    }

    override fun getTempReservation(date: LocalDate): List<Int> {
        val reservationKey = "${TempReservationConstant.CHECK_SEAT}$date"

        val numbers = redisTemplate.opsForSet().members(reservationKey)
        return numbers?.map { it.toInt() } ?: emptyList()
    }

    @Transactional
    override fun cleanupExpiredReservation(reservationId: Long) {

        // 1. RDB에서 예약 정보 조회
        val reservation = reservationRepository.findById(reservationId).orElseThrow {
            throw EntityNotFoundException("${reservationId}를 가진 예약 정보를 조회할 수 없습니다.")
        }

        // 2. Redis Set에서 좌석 제거 (seats:reserved:날짜 에서 좌석 번호 제거)
        val reserveKey = "${TempReservationConstant.CHECK_SEAT}${reservation.date}"
        redisTemplate.opsForSet().remove(reserveKey, reservation.seatNumber)

        // 3. RDB 예약 상태 변경 및 저장
        reservation.status = ReservationStatus.CANCEL
        reservationRepository.save(reservation)
    }

}
