package kr.hhplus.be.server.integration

import kr.hhplus.be.server.concert.domain.Concert
import kr.hhplus.be.server.concert.port.ConcertRepository
import kr.hhplus.be.server.member.domain.Member
import kr.hhplus.be.server.member.port.MemberRepository
import kr.hhplus.be.server.outbox.domain.AggregateType
import kr.hhplus.be.server.outbox.domain.EventType
import kr.hhplus.be.server.outbox.domain.OutboxStatus
import kr.hhplus.be.server.outbox.port.OutboxRepository
import kr.hhplus.be.server.reservation.dto.ReservationRequest
import kr.hhplus.be.server.reservation.infrastructure.RedisReservationOperations
import kr.hhplus.be.server.reservation.domain.ReservationStatus
import kr.hhplus.be.server.reservation.port.ReservationRepository
import kr.hhplus.be.server.reservation.service.ReservationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReservationServiceIntegrationTest {

    @Autowired
    private lateinit var reservationService: ReservationService

    @Autowired
    private lateinit var reservationRepository: ReservationRepository

    @Autowired
    private lateinit var concertRepository: ConcertRepository

    @Autowired
    private lateinit var outboxRepository: OutboxRepository

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    lateinit var redisReservationOperations: RedisReservationOperations

    @BeforeEach
    @AfterEach
    fun redisCleanup() {
        redisReservationOperations.cleanUp()
    }

    @Test
    fun `예약 생성 시 예약정보와 아웃박스 메시지가 함께 저장된다`() {
        // Given
        val member = memberRepository.save(Member(username = "테스터", password = "password"))
        val concert = concertRepository.save(Concert(
            name = "테스트 콘서트",
            date = LocalDate.of(2025, 12, 25),
            totalSeats = 50
        ))
        val seatNumber = 10
        val request = ReservationRequest(
            concertId = concert.id!!,
            memberId = member.id!!,
            seatNumber = seatNumber
        )

        // When
        val result = reservationService.make(request)

        // Then
        // 1. 예약 정보 검증
        val savedReservation = reservationRepository.findReservationByIdAndReserver(result.id!!, member.id!!)
        assertThat(savedReservation).isNotNull
        assertThat(savedReservation.concert.id).isEqualTo(concert.id)
        assertThat(savedReservation.seatNumber).isEqualTo(seatNumber)
        assertThat(savedReservation.status).isEqualTo(ReservationStatus.PENDING)

        // 2. 아웃박스 메시지 검증
        val pendingMessages = outboxRepository.getPendingList()
        assertThat(pendingMessages).isNotEmpty

        val targetMessage = pendingMessages.find {
            it.aggregateType == AggregateType.TEMP_RESERVATION &&
                it.eventType == EventType.INSERT &&
                it.status == OutboxStatus.PENDING
        }

        assertThat(targetMessage).isNotNull
        assertThat(targetMessage?.status).isEqualTo(OutboxStatus.PENDING)
        assertThat(targetMessage?.payload).containsEntry("seatNumber", seatNumber)
    }
}

