package kr.hhplus.be.server.common.service

import io.jsonwebtoken.lang.Supplier
import kr.hhplus.be.server.common.port.DistributeLockManager
import kr.hhplus.be.server.exception.LockException
import mu.KotlinLogging
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class DistributeLockManagerImpl(
    private val redissonClient: RedissonClient,
) : DistributeLockManager {

    private val log = KotlinLogging.logger { }

    override fun <T> runWithLock(lockKey: String, task: Supplier<T>): T {
        val lock: RLock = redissonClient.getLock(lockKey)

        try {
            // 획득을 5초 기다리고, 획득 후 2초 지나고 자동으로 해제됨
            val available = lock.tryLock(5, 2, TimeUnit.SECONDS)

            if (!available) {
                log.error("분산 락 획득 실패")
                throw LockException("현재 이용자가 많아 대기 중입니다")
            }

            return task.get()
        } catch (e: InterruptedException) {
            log.error("Lock 획득 중 인터럽트 발생 ", e)
            throw LockException("인터럽트가 발생하여 예약이 취소되었습니다. 처음부터 다시 예약해주세요")
        } finally {
            if (lock.isHeldByCurrentThread) lock.unlock()
        }

    }
}
