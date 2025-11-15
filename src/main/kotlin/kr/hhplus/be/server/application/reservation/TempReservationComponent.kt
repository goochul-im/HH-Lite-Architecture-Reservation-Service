package kr.hhplus.be.server.application.reservation

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import kr.hhplus.be.server.application.reservation.TempReservationConstant.CHECK_SEAT
import kr.hhplus.be.server.application.reservation.TempReservationConstant.TEMP_RESERVATIONS
import kr.hhplus.be.server.domain.reservation.ReservationRepository
import kr.hhplus.be.server.domain.reservation.ReservationStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate

@Component
class TempReservationComponent (
    private val redisTemplate: RedisTemplate<String, Any>,
    private val reservationRepository: ReservationRepository,
) {

    @Value("\${reservation.pending-timeout-seconds}")
    private val pendingTimeoutSeconds: Long = 300L
    private val PENDING_TIMEOUT: Duration
        get() = Duration.ofSeconds(pendingTimeoutSeconds)

    fun save(date: LocalDate, reservationId: Long, seatNumber: Int) {

        val seatListKey = "$TEMP_RESERVATIONS$reservationId" // 임시 예약된 좌석 전체, 만료되면 리스너에서 감지
        val reserveKey = "$CHECK_SEAT$date" // 현재 날짜에 임시 예약된 좌석 번호들 체크하는 용도

        redisTemplate.execute{ // 레디스 트랜잭션
            it.multi()

            redisTemplate.opsForValue().set(seatListKey, seatNumber)
            redisTemplate.opsForSet().add(reserveKey, seatNumber)

            redisTemplate.expire(seatListKey, PENDING_TIMEOUT)

            it.exec()
        }

    }

    @Transactional
    fun cleanupExpiredReservation(reservationId: Long) {

        // 1. RDB에서 예약 정보 조회
        val reservation = reservationRepository.findById(reservationId).orElseThrow {
            throw EntityNotFoundException("${reservationId}를 가진 예약 정보를 조회할 수 없습니다.")
        }

        // 2. Redis Set에서 좌석 제거 (seats:reserved:날짜 에서 좌석 번호 제거)
        val reserveKey = "$CHECK_SEAT${reservation.date}"
        redisTemplate.opsForSet().remove(reserveKey, reservation.seatNumber)

        // 3. RDB 예약 상태 변경 및 저장
        reservation.status = ReservationStatus.CANCEL
        reservationRepository.save(reservation)
    }

}

