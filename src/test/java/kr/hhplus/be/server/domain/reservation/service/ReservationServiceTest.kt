package kr.hhplus.be.server.domain.reservation.service

import kr.hhplus.be.server.reservation.dto.ReservationRequest
import kr.hhplus.be.server.auth.AuthService
import kr.hhplus.be.server.member.Member
import kr.hhplus.be.server.reservation.domain.Reservation
import kr.hhplus.be.server.reservation.domain.ReservationRepository
import kr.hhplus.be.server.reservation.domain.ReservationStatus
import kr.hhplus.be.server.reservation.port.TempReservationPort
import kr.hhplus.be.server.reservation.service.ReservationService
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class ReservationServiceTest {

    @Mock
    private lateinit var reservationRepository: ReservationRepository

    @Mock
    private lateinit var tempReservationService: TempReservationPort

    @Mock
    private lateinit var authService: AuthService

    private lateinit var reservationService: ReservationService

    @BeforeEach
    fun init() {
        reservationService = ReservationService(
            reservationRepository,
            tempReservationService,
            authService,
            1000
        )
    }

    @Test
    @DisplayName("예약 생성 테스트")
    fun make_reservation_test() {
        // Given
        val memberId = "testmember"
        val date = LocalDate.now()
        val seatNumber = 5
        val request = ReservationRequest(date, memberId, seatNumber)

        val member = Member(id = memberId, username = "Test User", password = "testpassword")
        val reservation = Reservation(
            id = 1L,
            date = date,
            seatNumber = seatNumber,
            status = ReservationStatus.PENDING,
            reserver = member
        )

        given(authService.getById(memberId)).willReturn(member)
        given(reservationRepository.save(any())).willReturn(reservation)

        // When
        reservationService.make(request)

        // Then
        verify(authService).getById(memberId)
        verify(reservationRepository).save(any())
        verify(tempReservationService).save(date, reservation.id!!, seatNumber)
    }

    @Test
    fun `선택한 날짜의 예약 가능한 자리를 가져올 수 있다`() {
        //given
        val date = LocalDate.now()
        given(reservationRepository.getReservedSeatnumber(date)).willReturn(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9))
        given(tempReservationService.getTempReservation(date)).willReturn(
            listOf(
                41,
                42,
                43,
                44,
                45,
                46,
                47,
                48,
                49,
                50
            )
        )

        //when
        val result = reservationService.getAvailableSeat(date)

        //then
        val expect = (10..40).toList()
        assertThat(result).containsAll(expect)
    }

    @Test
    fun `결제를 하면 예약의 상태가 바뀐다`() {
        //given
        val reservationId = 30L
        val memberId = "testmemberId"
        val member = Member(id = memberId, username = "Test User", password = "testpassword", point = 10000)
        val reservation = Reservation(
            reservationId,
            LocalDate.of(2025, 11, 18),
            10,
            ReservationStatus.PENDING,
            member
        )

        given(tempReservationService.isValidReservation(reservationId)).willReturn(true)
        given(authService.getById(memberId)).willReturn(member)
        given(reservationRepository.findReservationByIdAndReserver(reservationId, member)).willReturn(reservation)

        //when
        val result = reservationService.payReservation(reservationId, memberId)

        //then
        assertAll(
            { assertThat(result.status).isEqualTo(ReservationStatus.RESERVE) },
            { assertThat(result.id).isEqualTo(reservationId) },
            { assertThat(result.date).isEqualTo(LocalDate.of(2025, 11, 18)) }, // 콤마 추가
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
        val member = Member(id = memberId, username = "Test User", password = "testpassword", point = 500)
        val reservation = Reservation(
            reservationId,
            LocalDate.of(2025, 11, 18),
            10,
            ReservationStatus.PENDING,
            member
        )

        given(tempReservationService.isValidReservation(reservationId)).willReturn(true)
        given(authService.getById(memberId)).willReturn(member)
        given(reservationRepository.findReservationByIdAndReserver(reservationId, member)).willReturn(reservation)

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
        val member = Member(id = memberId, username = "Test User", password = "testpassword", point = 10000)

        given(tempReservationService.isValidReservation(reservationId)).willReturn(true)
        given(authService.getById(memberId)).willReturn(member)

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
        val member = Member(id = memberId, username = "Test User", password = "testpassword", point = 10000)

        given(tempReservationService.isValidReservation(reservationId)).willReturn(true)
        given(authService.getById(memberId)).willReturn(member)
        given(reservationRepository.findReservationByIdAndReserver(reservationId, member)).willReturn(null)

        //when & then
        assertThatThrownBy { reservationService.payReservation(reservationId, memberId) }.isInstanceOf(
            RuntimeException::class.java
        )

    }



}
