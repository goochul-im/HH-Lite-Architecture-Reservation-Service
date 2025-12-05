package kr.hhplus.be.server.outbox.scheduler

import kr.hhplus.be.server.outbox.domain.OutboxMessage
import kr.hhplus.be.server.outbox.exception.OutboxException
import kr.hhplus.be.server.outbox.port.OutboxHandler
import kr.hhplus.be.server.outbox.port.OutboxRepository
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OutboxScheduler(
    private val outboxRepository: OutboxRepository,
    private val handlers: List<OutboxHandler>
) {

    private val log = KotlinLogging.logger { }

    @Transactional
    @Scheduled(fixedRate = 1000)
    fun schedule() {
        val pendingList = outboxRepository.getPendingList()
        pendingList.forEach { outbox ->
            try {
                val handler = handlers.find { it.canHandle(outbox.aggregateType) }
                handler?.handle(outbox) ?:
                run {
                    log.error { "아웃박스 스케줄러에서 적절한 핸들러를 찾지 못했습니다." }
//                    throw OutboxException("no handler found for aggregateType = ${outbox.aggregateType}")
                }
            } catch (e: Exception) {
                log.error { "메세지 처리 중 오류 발생 : id = ${outbox.id}, message = ${e.message}" }
            }
        }
    }

}
