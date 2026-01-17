package kr.hhplus.be.server.integration

import kr.hhplus.be.server.TestcontainersConfiguration
import kr.hhplus.be.server.concert.infrastructure.ConcertEntity
import kr.hhplus.be.server.concert.infrastructure.ConcertJpaRepository
import kr.hhplus.be.server.member.infrastructure.MemberEntity
import kr.hhplus.be.server.member.infrastructure.MemberJpaRepository
import kr.hhplus.be.server.reservation.dto.ReservationRequest
import kr.hhplus.be.server.reservation.infrastructure.ReservationJpaRepository
import kr.hhplus.be.server.reservation.infrastructure.TempReservationAdaptor
import kr.hhplus.be.server.reservation.port.SeatFinder
import kr.hhplus.be.server.reservation.service.ReservationService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@SpringBootTest
@Import(TestcontainersConfiguration::class)
@ActiveProfiles("test")
class ReservationCacheIntegrationTest {

    @Autowired
    private lateinit var reservationService: ReservationService

    @Autowired
    private lateinit var seatFinder: SeatFinder

    @SpyBean
    private lateinit var reservationJpaRepository: ReservationJpaRepository

    @Autowired
    private lateinit var memberJpaRepository: MemberJpaRepository

    @Autowired
    private lateinit var concertJpaRepository: ConcertJpaRepository

    @Autowired
    private lateinit var tempReservationAdaptor: TempReservationAdaptor

    @Autowired
    private lateinit var redissonClient: RedissonClient

    @Autowired
    private lateinit var cacheManager: CacheManager

    private lateinit var testMember: MemberEntity
    private lateinit var testConcert: ConcertEntity

    @BeforeEach
    fun setUp() {
        reservationJpaRepository.deleteAll()
        concertJpaRepository.deleteAll()
        memberJpaRepository.deleteAll()
        redissonClient.keys.flushall()

        cacheManager.getCache("availableSeats")?.clear()

        testMember = memberJpaRepository.save(MemberEntity(username = "CacheTester", password = "pw"))
        testConcert = concertJpaRepository.save(
            ConcertEntity(name = "테스트 콘서트", date = LocalDate.now().plusDays(10), totalSeats = 50)
        )
    }

    @Test
    fun `getAvailableSeats 호출 시 캐시가 적용되어 두 번째 호출부터는 DB 조회가 발생하지 않아야 한다`() {
        val concertId = testConcert.id!!

        // 1. 첫 번째 호출: Cache Miss -> DB 조회 발생
        seatFinder.getAvailableSeats(concertId)
        verify(reservationJpaRepository, times(1)).getReservedSeatNumbers(concertId)

        // 2. 두 번째 호출: Cache Hit -> DB 조회 발생 X
        seatFinder.getAvailableSeats(concertId)
        verify(reservationJpaRepository, times(1)).getReservedSeatNumbers(concertId)
    }

    @Test
    fun `make 호출 시 해당 콘서트의 캐시가 제거되어야 한다`() {
        val concert = concertJpaRepository.save(
            ConcertEntity(name = "캐시 테스트 콘서트", date = LocalDate.now().plusDays(1), totalSeats = 50)
        )
        val concertId = concert.id!!
        val dto = ReservationRequest(concertId = concertId, memberId = testMember.id!!, seatNumber = 1)

        // 1. 조회하여 캐시 생성
        seatFinder.getAvailableSeats(concertId)
        verify(reservationJpaRepository, times(1)).getReservedSeatNumbers(concertId)

        clearInvocations(reservationJpaRepository)

        // 2. 예약 생성 (내부적으로 Evict 발생)
        reservationService.make(dto)

        clearInvocations(reservationJpaRepository)
        seatFinder.getAvailableSeats(concertId)
        // 여기서 DB 조회가 발생해야 함. (Evict가 잘 되었다면)
        verify(reservationJpaRepository, times(1)).getReservedSeatNumbers(concertId)
    }

    @Test
    fun `cleanupExpiredReservation 호출 시 해당 콘서트의 캐시가 제거되어야 한다`() {
        val concert = concertJpaRepository.save(
            ConcertEntity(name = "만료 테스트 콘서트", date = LocalDate.now().plusDays(2), totalSeats = 50)
        )
        val concertId = concert.id!!
        val dto = ReservationRequest(concertId = concertId, memberId = testMember.id!!, seatNumber = 5)

        // 1. 예약 생성 (DB 저장)
        val reservation = reservationService.make(dto)

        // 2. 조회하여 캐시 생성
        seatFinder.getAvailableSeats(concertId)

        clearInvocations(reservationJpaRepository)

        // 3. 캐시 Hit 확인
        seatFinder.getAvailableSeats(concertId)
        verify(reservationJpaRepository, times(0)).getReservedSeatNumbers(concertId)

        // 4. 만료 처리 (수동 Evict 로직 동작)
        tempReservationAdaptor.cleanupExpiredReservation(reservation.id!!)

        // 5. 재조회: Evict 되었으므로 DB 조회 다시 발생 (총 1회)
        seatFinder.getAvailableSeats(concertId)
        verify(reservationJpaRepository, times(1)).getReservedSeatNumbers(concertId)
    }
}
