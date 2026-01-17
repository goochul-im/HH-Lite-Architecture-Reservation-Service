package kr.hhplus.be.server.reservation.infrastructure

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import kr.hhplus.be.server.reservation.port.TempReservationPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component

@Component
class TempReservationAdaptor(
    private val redisOperations: RedisReservationOperations,
    private val reservationJpaRepository: ReservationJpaRepository,
    private val cacheManager: CacheManager
) : TempReservationPort {

    @Value("\${reservation.pending-timeout-seconds}")
    private val pendingTimeoutSeconds: Long = 300L

    override fun save(concertId: Long, reservationId: Long, seatNumber: Int) {
        val seatListKey = getSeatListKey(reservationId)
        val reserveKey = getReservedKey(concertId)

        redisOperations.saveTempReservation(
            seatListKey = seatListKey,
            reserveKey = reserveKey,
            seatNumber = seatNumber,
            timeoutSeconds = pendingTimeoutSeconds
        )
    }

    override fun getTempReservation(concertId: Long): List<Int> {
        val reservationKey = getReservedKey(concertId)
        return redisOperations.getTempReservedSeats(reservationKey)
    }

    @Transactional
    override fun cleanupExpiredReservation(reservationId: Long) {
        val reservation = reservationJpaRepository.findById(reservationId).orElseThrow {
            throw EntityNotFoundException("${reservationId}를 가진 예약 정보를 조회할 수 없습니다.")
        }

        val concertId = reservation.concert.id!!
        val reserveKey = getReservedKey(concertId)
        redisOperations.removeFromReserveSet(reserveKey, reservation.seatNumber)

        val seatListKey = "${TempReservationConstant.TEMP_RESERVATIONS}$reservationId"
        redisOperations.deleteReservation(seatListKey)

        reservation.status = ReservationStatus.CANCEL
        reservationJpaRepository.save(reservation)

        cacheManager.getCache("availableSeats")?.evict(concertId)
    }

    override fun delete(reservationId: Long) {
        val seatListKey = getSeatListKey(reservationId)
        redisOperations.deleteReservation(seatListKey)
    }

    override fun isValidReservation(reservationId: Long): Boolean {
        val seatListKey = getSeatListKey(reservationId)
        return redisOperations.isReservationExists(seatListKey)
    }

    private fun getReservedKey(concertId: Long): String =
        "${TempReservationConstant.CHECK_SEAT}$concertId"

    private fun getSeatListKey(reservationId: Long): String =
        "${TempReservationConstant.TEMP_RESERVATIONS}$reservationId"
}
