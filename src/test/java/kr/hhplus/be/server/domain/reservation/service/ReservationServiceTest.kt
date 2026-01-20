package kr.hhplus.be.server.domain.reservation.service

import kr.hhplus.be.server.concert.domain.Concert
import kr.hhplus.be.server.concert.infrastructure.ConcertEntity
import kr.hhplus.be.server.concert.port.ConcertRankingPort
import kr.hhplus.be.server.concert.port.ConcertRepository
import kr.hhplus.be.server.member.infrastructure.MemberEntity
import kr.hhplus.be.server.member.port.MemberRepository
import kr.hhplus.be.server.outbox.port.OutboxRepository
import kr.hhplus.be.server.reservation.domain.Reservation
import kr.hhplus.be.server.reservation.dto.ReservationRequest
import kr.hhplus.be.server.reservation.infrastructure.ReservationEntity
import kr.hhplus.be.server.reservation.domain.ReservationStatus
import kr.hhplus.be.server.reservation.port.ReservationRepository
import kr.hhplus.be.server.reservation.port.SeatFinder
import kr.hhplus.be.server.reservation.port.TempReservationPort
import kr.hhplus.be.server.reservation.service.ReservationService
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class ReservationServiceTest {

    @Mock
    private lateinit var reservationRepository: ReservationRepository

    @Mock
    private lateinit var tempReservationService: TempReservationPort

    @Mock
    lateinit var memberRepository: MemberRepository

    @Mock
    lateinit var concertRepository: ConcertRepository

    @Mock
    lateinit var outboxRepository: OutboxRepository

    @Mock
    lateinit var seatFinder: SeatFinder

    @Mock
    lateinit var concertRankingPort: ConcertRankingPort

    private lateinit var reservationService: ReservationService

    @BeforeEach
    fun init() {
        reservationService = ReservationService(
            reservationRepository,
            tempReservationService,
            memberRepository,
            concertRepository,
            1000,
            outboxRepository,
            seatFinder,
            concertRankingPort
        )
    }

    @Test
    @DisplayName("예약 생성 테스트")
    fun make_reservation_test() {
        // Given
        val memberId = "testmember"
        val concertId = 1L
        val seatNumber = 5
        val request = ReservationRequest(concertId, memberId, seatNumber)

        val memberEntity = MemberEntity(id = memberId, username = "Test User", password = "testpassword")
        val member = memberEntity.toDomain()
        val concertEntity = ConcertEntity(
            id = concertId,
            name = "테스트 콘서트",
            date = LocalDate.now(),
            totalSeats = 50
        )
        val concert = concertEntity.toDomain()
        val reservationEntity = ReservationEntity(
            id = 1L,
            concert = concertEntity,
            seatNumber = seatNumber,
            status = ReservationStatus.PENDING,
            reserver = memberEntity
        )
        val reservation = reservationEntity.toDomain()

        given(memberRepository.findById(memberId)).willReturn(member)
        given(concertRepository.findById(concertId)).willReturn(concert)
        given(reservationRepository.save(any())).willReturn(reservation)
        given(seatFinder.getAvailableSeats(any())).willReturn(listOf(seatNumber))

        // When
        val result = reservationService.make(request)

        // Then
        assertThat(result.id).isEqualTo(1L)
        assertThat(result.concert.id).isEqualTo(concertId)
        assertThat(result.seatNumber).isEqualTo(5)
        assertThat(result.status).isEqualTo(ReservationStatus.PENDING)
        assertThat(result.reserver).isNotNull
        assertThat(result.reserver!!.id).isEqualTo("testmember")
        assertThat(result.reserver!!.point).isEqualTo(0)
        assertThat(result.reserver!!.username).isEqualTo("Test User")
        assertThat(result.reserver!!.password).isEqualTo("testpassword")

        verify(reservationRepository, times(1)).save(any())
        verify(outboxRepository, times(1)).save(any())
    }

    @Test
    fun `결제를 하면 예약의 상태가 바뀐다`() {
        //given
        val reservationId = 30L
        val memberId = "testmemberId"
        val concertId = 1L
        val memberEntity = MemberEntity(id = memberId, username = "Test User", password = "testpassword", point = 10000)
        val member = memberEntity.toDomain()
        val concertEntity = ConcertEntity(
            id = concertId,
            name = "테스트 콘서트",
            date = LocalDate.of(2025, 11, 18),
            totalSeats = 50
        )
        val concert = concertEntity.toDomain()
        val reservation = Reservation(
            reservationId,
            concert,
            10,
            ReservationStatus.PENDING,
            member
        )

        given(tempReservationService.isValidReservation(reservationId)).willReturn(true)
        given(memberRepository.findById(memberId)).willReturn(member)
        given(reservationRepository.findReservationByIdAndReserver(reservationId, member.id!!)).willReturn(reservation)
        given(reservationRepository.save(any())).willAnswer { it.arguments[0] }
        given(memberRepository.saveAndFlush(any())).willAnswer { it.arguments[0] }

        //when
        val result = reservationService.payReservation(reservationId, memberId)

        //then
        assertAll(
            { assertThat(result.status).isEqualTo(ReservationStatus.RESERVE) },
            { assertThat(result.id).isEqualTo(reservationId) },
            { assertThat(result.concert.date).isEqualTo(LocalDate.of(2025, 11, 18)) },
            { assertThat(result.seatNumber).isEqualTo(10) },
            { assertThat(result.reserver).isNotNull() },
            { assertThat(result.reserver!!.id).isEqualTo(memberId) },
            { assertThat(result.reserver!!.username).isEqualTo("Test User") },
            { assertThat(result.reserver!!.point).isEqualTo(9000) }
        )
    }

    @Test
    fun `포인트 잔액이 모자라면 예외를 던진다`() {
        //given
        val reservationId = 30L
        val memberId = "testmemberId"
        val concertId = 1L
        val memberEntity = MemberEntity(id = memberId, username = "Test User", password = "testpassword", point = 500)
        val member = memberEntity.toDomain()
        val concertEntity = ConcertEntity(
            id = concertId,
            name = "테스트 콘서트",
            date = LocalDate.of(2025, 11, 18),
            totalSeats = 50
        )
        val concert = concertEntity.toDomain()
        val reservation = Reservation(
            reservationId,
            concert,
            10,
            ReservationStatus.PENDING,
            member
        )

        given(tempReservationService.isValidReservation(reservationId)).willReturn(true)
        given(memberRepository.findById(memberId)).willReturn(member)
        given(reservationRepository.findReservationByIdAndReserver(reservationId, member.id!!)).willReturn(reservation)

        //when & then
        assertThatThrownBy { reservationService.payReservation(reservationId, memberId) }.isInstanceOf(
            RuntimeException::class.java
        )
    }

    @Test
    fun `임시 예약되어있지 않으면 예외를 던진다`() {
        //given
        val reservationId = 30L
        val memberId = "testmemberId"

        given(tempReservationService.isValidReservation(reservationId)).willReturn(false)

        //when
        assertThatThrownBy { reservationService.payReservation(reservationId, memberId) }.isInstanceOf(
            RuntimeException::class.java
        )
    }

    @Test
    fun `이미 결제된 자리에 결제를 시도하면 예외를 던진다`() {
        //given
        val reservationId = 30L
        val memberId = "testmemberId"
        val memberEntity = MemberEntity(id = memberId, username = "Test User", password = "testpassword", point = 10000)
        val member = memberEntity.toDomain()

        given(tempReservationService.isValidReservation(reservationId)).willReturn(true)
        given(memberRepository.findById(memberId)).willReturn(member)

        //when & then
        assertThatThrownBy { reservationService.payReservation(reservationId, memberId) }.isInstanceOf(
            RuntimeException::class.java
        )
    }

    @Test
    fun `예약을 찾을 수 없으면 예외를 던진다`() {
        //given
        val reservationId = 30L
        val memberId = "testmemberId"
        val memberEntity = MemberEntity(id = memberId, username = "Test User", password = "testpassword", point = 10000)
        val member = memberEntity.toDomain()

        given(tempReservationService.isValidReservation(reservationId)).willReturn(true)
        given(memberRepository.findById(memberId)).willReturn(member)
        given(reservationRepository.findReservationByIdAndReserver(reservationId, member.id!!)).willReturn(null)

        //when & then
        assertThatThrownBy { reservationService.payReservation(reservationId, memberId) }.isInstanceOf(
            RuntimeException::class.java
        )
    }
}
