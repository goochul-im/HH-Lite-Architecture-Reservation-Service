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
import kr.hhplus.be.server.reservation.infrastructure.ReservationEntity
import kr.hhplus.be.server.reservation.infrastructure.ReservationStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.jdbc.Sql
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@Sql(scripts = ["/test-index-setup.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
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

    @Autowired
    private lateinit var tempReservationPort: TempReservationPort

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

    @Test
    fun `중복 결제 요청 시 포인트가 한 번만 차감되어야 한다`() {
        // given
        val threadCount = 5
        val executorService = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)

        // 유저 준비 (포인트 10000원)
        var member = memberRepository.save(Member(username = "payTester", password = "pw"))
        member.chargePoint(10000)
        member = memberRepository.save(member)
        val initialPoint = member.point

        // 예약 준비
        val date = LocalDate.of(2025, 12, 26)
        val seatNumber = 15
        
        val reservationEntity = ReservationEntity(
            date = date,
            seatNumber = seatNumber,
            status = ReservationStatus.PENDING,
            reserver = kr.hhplus.be.server.member.infrastructure.MemberEntity.from(member)
        )
        val savedReservation = reservationJpaRepository.save(reservationEntity)
        val reservationId = savedReservation.id!!

        // Redis 임시 예약 상태 설정 (결제 검증 통과용)
        tempReservationPort.save(date, reservationId, seatNumber)

        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // when
        for (i in 0 until threadCount) {
            executorService.submit {
                try {
                    reservationService.payReservation(reservationId, member.id!!)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    println("Payment failed: ${e.message}")
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()

        // then
        val finalMember = memberRepository.findById(member.id!!)
        
        println("시도 횟수: $threadCount")
        println("성공 횟수: ${successCount.get()}")
        println("실패 횟수: ${failCount.get()}")
        println("초기 포인트: $initialPoint")
        println("최종 포인트: ${finalMember.point}")
        
        assertThat(successCount.get()).isEqualTo(1)
        assertThat(finalMember.point).isEqualTo(0)
    }
}
