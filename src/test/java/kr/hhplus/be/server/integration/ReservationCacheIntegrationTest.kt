package kr.hhplus.be.server.integration

import kr.hhplus.be.server.TestcontainersConfiguration
import kr.hhplus.be.server.member.infrastructure.MemberEntity
import kr.hhplus.be.server.member.infrastructure.MemberJpaRepository
import kr.hhplus.be.server.reservation.dto.ReservationRequest
import kr.hhplus.be.server.reservation.infrastructure.ReservationJpaRepository
import kr.hhplus.be.server.reservation.infrastructure.TempReservationAdaptor
import kr.hhplus.be.server.reservation.port.SeatFinder
import kr.hhplus.be.server.reservation.service.ReservationService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.RedisTemplate
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
    private lateinit var tempReservationAdaptor: TempReservationAdaptor

    @Autowired
    private lateinit var redissonClient: RedissonClient

    @Autowired
    private lateinit var cacheManager: CacheManager

    private lateinit var testMember: MemberEntity

    @BeforeEach
    fun setUp() {
        reservationJpaRepository.deleteAll()
        memberJpaRepository.deleteAll()
        redissonClient.keys.flushall()
        
        // 캐시 초기화
        cacheManager.getCache("availableSeats")?.clear()

        testMember = memberJpaRepository.save(MemberEntity(username = "CacheTester", password = "pw"))
    }

    @Test
    fun `getAvailableSeat 호출 시 캐시가 적용되어 두 번째 호출부터는 DB 조회가 발생하지 않아야 한다`() {
        val date = LocalDate.now()

        // 1. 첫 번째 호출: Cache Miss -> DB 조회 발생
        seatFinder.getAvailableSeat(date)
        verify(reservationJpaRepository, times(1)).getReservedSeatNumber(date)

        // 2. 두 번째 호출: Cache Hit -> DB 조회 발생 X
        seatFinder.getAvailableSeat(date)
        verify(reservationJpaRepository, times(1)).getReservedSeatNumber(date)
    }

    @Test
    fun `make 호출 시 해당 날짜의 캐시가 제거되어야 한다`() {
        val date = LocalDate.now().plusDays(1)
        val dto = ReservationRequest(date = date, memberId = testMember.id!!, seatNumber = 1)

        // 1. 조회하여 캐시 생성
        seatFinder.getAvailableSeat(date)
        verify(reservationJpaRepository, times(1)).getReservedSeatNumber(date)

        // Mock 리셋
        clearInvocations(reservationJpaRepository)

        // 2. 예약 생성 (내부적으로 Evict 발생)
        reservationService.make(dto)

        // 3. 재조회: Cache Miss -> DB 조회 다시 발생 (총 1회 - make 내부 호출 제외하고, 이번 호출에서 발생했는지 확인이 어려우므로 전체 흐름보다는 상태 변화에 집중하거나, make 내부 호출 포함 여부를 고려해야 함)
        // make() 내부에서도 getAvailableSeat()을 호출하므로, 여기서 DB 조회가 발생함.
        // 따라서 make() 호출 후 다시 getAvailableSeat()을 불렀을 때 '또' DB를 조회하는지(Evict가 되었는지) 확인해야 함.
        
        // make 실행 중 getAvailableSeat 호출됨 -> DB 조회 1회 발생 예상 (왜냐하면 make의 @CacheEvict는 메소드 종료 후 또는 진입 전(beforeInvocation=false 기본)에 동작하지만,
        // 보통은 메소드 실행 '후'에 Evict가 일어남. 따라서 make 내부의 getAvailableSeat은 아직 Evict 전이라 캐시가 있다면 캐시를 탈 것임.
        // 하지만 위에서 getAvailableSeat(date)를 호출해 캐시를 만들어 둠.
        
        // 시나리오 수정:
        // 1. getAvailableSeat -> Cache Hit (DB 1회)
        // 2. make -> 내부 getAvailableSeat (Cache Hit) -> 성공 후 Evict
        // 3. getAvailableSeat -> Cache Miss (DB 조회 발생)
        
        clearInvocations(reservationJpaRepository)
        seatFinder.getAvailableSeat(date)
        // 여기서 DB 조회가 발생해야 함. (Evict가 잘 되었다면)
        verify(reservationJpaRepository, times(1)).getReservedSeatNumber(date)
    }

    @Test
    fun `cleanupExpiredReservation 호출 시 해당 날짜의 캐시가 제거되어야 한다`() {
        val date = LocalDate.now().plusDays(2)
        val dto = ReservationRequest(date = date, memberId = testMember.id!!, seatNumber = 5)

        // 1. 예약 생성 (DB 저장)
        val reservation = reservationService.make(dto)
        
        // 2. 조회하여 캐시 생성
        seatFinder.getAvailableSeat(date)
        
        clearInvocations(reservationJpaRepository)

        // 3. 캐시 Hit 확인
        seatFinder.getAvailableSeat(date)
        verify(reservationJpaRepository, times(0)).getReservedSeatNumber(date)

        // 4. 만료 처리 (수동 Evict 로직 동작)
        tempReservationAdaptor.cleanupExpiredReservation(reservation.id!!)

        // 5. 재조회: Evict 되었으므로 DB 조회 다시 발생 (총 1회)
        seatFinder.getAvailableSeat(date)
        verify(reservationJpaRepository, times(1)).getReservedSeatNumber(date)
    }
}
