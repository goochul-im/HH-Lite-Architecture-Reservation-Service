package kr.hhplus.be.server.application.wating

import kr.hhplus.be.server.common.jwt.JwtProvider
import kr.hhplus.be.server.auth.UserStatus
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
}
