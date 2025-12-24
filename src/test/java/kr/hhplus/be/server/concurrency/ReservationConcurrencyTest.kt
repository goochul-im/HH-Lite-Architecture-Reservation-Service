package kr.hhplus.be.server.concurrency

import kr.hhplus.be.server.TestcontainersConfiguration
import kr.hhplus.be.server.member.domain.Member
import kr.hhplus.be.server.member.port.MemberRepository
import kr.hhplus.be.server.reservation.dto.ReservationRequest
import kr.hhplus.be.server.reservation.infrastructure.RedisReservationOperations
import kr.hhplus.be.server.reservation.infrastructure.ReservationJpaRepository
import kr.hhplus.be.server.reservation.infrastructure.TempReservationAdaptor
import kr.hhplus.be.server.reservation.port.TempReservationPort
import kr.hhplus.be.server.reservation.service.ReservationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@Import(TestcontainersConfiguration::class)
class ReservationConcurrencyTest {

    @Autowired
    private lateinit var reservationService: ReservationService

    @Autowired
    private lateinit var reservationJpaRepository: ReservationJpaRepository

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var redisOperation: RedisReservationOperations

    @BeforeEach
    fun setUp() {
        reservationJpaRepository.deleteAll()
        redisOperation.cleanUp()
    }

    @AfterEach
    fun tearDown() {
        reservationJpaRepository.deleteAll()
        redisOperation.cleanUp()
    }

    @Test
    fun `동시에 30명이 같은 날짜 같은 좌석을 예약하면 1명만 성공해야 한다`() {
        // given
        val threadCount = 30
        val executorService = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)

        val date = LocalDate.of(2025, 12, 25)
        val seatNumber = 10

        val memberIds = (1..threadCount).map {
            val member = Member(
                username = "tester$it",
                password = "testerPassword"
            )
            memberRepository.save(member).id!!
        }

        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // when
        for (i in 0 until threadCount) {
            val memberId = memberIds[i]
            executorService.submit {
                try {
                    reservationService.make(
                        ReservationRequest(
                            date = date,
                            seatNumber = seatNumber,
                            memberId = memberId
                        )
                    )
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        // then
        val reservations = reservationJpaRepository.findAll()

        println("시도 횟수: $threadCount")
        println("성공 횟수: ${successCount.get()}")
        println("실패 횟수: ${failCount.get()}")
        println("DB 저장된 예약 수: ${reservations.size}")

        assertThat(reservations.size).isEqualTo(1)
    }
}
