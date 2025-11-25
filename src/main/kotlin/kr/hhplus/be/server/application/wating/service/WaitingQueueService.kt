package kr.hhplus.be.server.application.wating.service

import kr.hhplus.be.server.application.wating.port.WaitingQueuePort
import kr.hhplus.be.server.common.jwt.JwtProvider
import kr.hhplus.be.server.auth.UserStatus
import kr.hhplus.be.server.exception.WaitingQueueException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class WaitingQueueService(
    private val waitingQueuePort: WaitingQueuePort,
    private val jwtProvider: JwtProvider,
) {
    fun enter(userId: String): String {
        // 1. 대기열에 사용자 추가
        waitingQueuePort.add(userId)

        // 2. 대기열 토큰 발급 (WAIT 상태)
        return jwtProvider.createWaitingToken(userId, UserStatus.WAIT)
    }

    fun getMyRank(userId: String) : Long{
        return waitingQueuePort.getMyRank(userId) ?: throw WaitingQueueException("대기열에 존재하지 않습니다")
    }

    fun isValidWaitingToken(userId: String): Boolean {
        if (!waitingQueuePort.isEnteringKey(userId))
            throw WaitingQueueException("접속되어있는 대기열 토큰이 아닙니다")
        return true
    }

    @Scheduled(fixedRate = 10000)
    fun enterQueue() {
        waitingQueuePort.enteringQueue()
    }
}
