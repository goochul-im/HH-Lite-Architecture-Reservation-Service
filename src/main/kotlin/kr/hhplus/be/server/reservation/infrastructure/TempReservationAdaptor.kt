package kr.hhplus.be.server.reservation.infrastructure

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import kr.hhplus.be.server.reservation.infrastructure.TempReservationConstant
import kr.hhplus.be.server.reservation.port.TempReservationPort
import kr.hhplus.be.server.reservation.infrastructure.RedisReservationOperations
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class TempReservationAdaptor(
    private val redisOperations: RedisReservationOperations,
    private val reservationJpaRepository: ReservationJpaRepository,
) : TempReservationPort {

    @Value("\${reservation.pending-timeout-seconds}")
    private val pendingTimeoutSeconds: Long = 300L

    override fun save(date: LocalDate, reservationId: Long, seatNumber: Int) {
        val seatListKey = getSeatListKey(reservationId)
        val reserveKey = getReservedKey(date)

        redisOperations.saveTempReservation(
            seatListKey = seatListKey,
            reserveKey = reserveKey,
            seatNumber = seatNumber,
            timeoutSeconds = pendingTimeoutSeconds
        )
    }

    override fun getTempReservation(date: LocalDate): List<Int> {
        val reservationKey = getReservedKey(date)
        return redisOperations.getTempReservedSeats(reservationKey)
    }

    @Transactional
    override fun cleanupExpiredReservation(reservationId: Long) {
        // 1. RDB에서 예약 정보 조회
        val reservation = reservationJpaRepository.findById(reservationId).orElseThrow {
            throw EntityNotFoundException("${reservationId}를 가진 예약 정보를 조회할 수 없습니다.")
        }

        // 2. Redis Set에서 좌석 제거
        val reserveKey = getReservedKey(reservation.date)
        redisOperations.removeFromReserveSet(reserveKey, reservation.seatNumber)

        // 3. RDB 예약 상태 변경
        reservation.status = ReservationStatus.CANCEL
        reservationJpaRepository.save(reservation)
    }

    override fun delete(reservationId: Long) {
        val seatListKey = getSeatListKey(reservationId)
        redisOperations.deleteReservation(seatListKey)
    }

    override fun isValidReservation(reservationId: Long): Boolean {
        val seatListKey = getSeatListKey(reservationId)
        return redisOperations.isReservationExists(seatListKey)
    }

    private fun getReservedKey(date: LocalDate): String =
        "${TempReservationConstant.CHECK_SEAT}$date"

    private fun getSeatListKey(reservationId: Long): String =
        "${TempReservationConstant.TEMP_RESERVATIONS}$reservationId"
}
