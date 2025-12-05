package kr.hhplus.be.server.outbox.infrastructure

import jakarta.persistence.LockModeType
import kr.hhplus.be.server.outbox.domain.OutboxMessage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface OutboxJpaRepository : JpaRepository<OutboxEntity, Long> {

    /**
     * 비관적 락을 DB 레코드에 걸고 가져옵니다.
     * 이로 인해 스케줄러가 아웃박스 핸들러보다 빠르게 작동하여 동시성 문제가 발생하는 것을 막습니다
     * PENDING 상태인 OutboxEntity를 생성 순으로 10개 가져옵니다
     * @return 아웃박스 리스트
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE) // 비관적 락 사용
    @Query(
        "select e " +
                "from OutboxEntity e " +
                "where e.status = 'PENDING' " +
                "order by e.createdAt asc " +
                "limit 10 "
    )
    fun getPendingList(): List<OutboxEntity>

}
