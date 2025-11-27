package kr.hhplus.be.server.application.wating.service

import kr.hhplus.be.server.application.wating.service.port.WaitingQueuePort
import kr.hhplus.be.server.auth.UserStatus
import kr.hhplus.be.server.common.jwt.JwtProvider
import kr.hhplus.be.server.exception.WaitingQueueException
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@ExtendWith(MockitoExtension::class)
class WaitingQueueServiceTest {

    @Mock
    lateinit var waitingQueuePort: WaitingQueuePort

    @Mock
    lateinit var jwtProvider: JwtProvider

    @InjectMocks
    lateinit var service: WaitingQueueService

    @Test
    fun `대기열 진입 시 포트에 추가하고 WAIT 토큰을 발급해야 한다`() {
        // given
        val userId = "user1"
        val expectedToken = "token_wait"

        // Mocking 동작 정의
        given(jwtProvider.createWaitingToken(userId, UserStatus.WAIT)).willReturn(expectedToken)

        // when
        val result = service.enter(userId)

        // then
        assertEquals(expectedToken, result)

        verify(waitingQueuePort, times(1)).add(userId)
    }

    @Test
    fun `유효하지 않은 대기열 키라면 예외를 던진다`(){
        //given
        val userId = "user1"
        given(waitingQueuePort.isEnteringKey(userId)).willReturn(false)

        //when & then
        assertThatThrownBy{
            service.isValidWaitingToken(userId)
        }.isInstanceOf(WaitingQueueException::class.java)
    }

    @Test
    fun `스케줄러 작동 시 활성 큐 진입 로직을 실행한다`(){
        //when
        service.enterQueue()

        //then
        verify(waitingQueuePort, times(1)).enteringQueue()
    }


}
