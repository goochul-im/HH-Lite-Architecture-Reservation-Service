package kr.hhplus.be.server.reservation.event.listener

import kr.hhplus.be.server.concert.port.ConcertRankingPort
import kr.hhplus.be.server.external.CreateReservationInfo
import kr.hhplus.be.server.external.DataFlatformPort
import kr.hhplus.be.server.reservation.event.ReservationCreatedEvent
import kr.hhplus.be.server.reservation.event.ReservationExpiredEvent
import kr.hhplus.be.server.reservation.event.ReservationPaidEvent
import kr.hhplus.be.server.reservation.port.TempReservationPort
import mu.KotlinLogging
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ReservationAfterCommitEventListener(
    private val concertRankingPort: ConcertRankingPort,
    private val tempReservationPort: TempReservationPort,
    private val cacheManager: CacheManager,
    private val dataFlatformPort: DataFlatformPort
) {

    private val log = KotlinLogging.logger { }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleReservationCreate(event: ReservationCreatedEvent) {
        dataFlatformPort.transferData(CreateReservationInfo(
            event.reservationId,
            event.concertId,
            event.memberId
        ))
        concertRankingPort.checkAndMarkSoldOut(event.concertId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleReservationPaid(event: ReservationPaidEvent) {
        tempReservationPort.delete(event.reservationId)
    }


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleReservationExpired(event: ReservationExpiredEvent) {
        cacheManager.getCache("availableSeats")?.evict(event.concertId)
    }

}
