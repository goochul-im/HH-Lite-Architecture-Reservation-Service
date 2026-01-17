package kr.hhplus.be.server.reservation.infrastructure

import kr.hhplus.be.server.concert.domain.Concert
import kr.hhplus.be.server.exception.ResourceNotFoundException
import kr.hhplus.be.server.member.domain.Member
import kr.hhplus.be.server.reservation.domain.Reservation
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class ReservationRepositoryImplTest {

    @Mock
    lateinit var reservationJpaRepository: ReservationJpaRepository

    @InjectMocks
    lateinit var reservationRepositoryImpl: ReservationRepositoryImpl

    @Test
    fun `콘서트 ID를 통해 예약된 좌석 번호들을 가져올 수 있다`() {
        //given
        val concertId = 1L
        val seats = listOf(1, 2, 3, 4, 5, 6, 10)
        given(reservationJpaRepository.getReservedSeatNumbers(concertId)).willReturn(seats)

        //when
        val result = reservationRepositoryImpl.getReservedSeatNumbers(concertId)

        //then
        assertThat(result).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 10)
    }

    @Test
    fun `예약 id와 예약자 id를 통해 알맞은 예약을 가져올 수 있다`() {
        //given
        val testDate = LocalDate.of(2025, 11, 25)
        val concert = Concert(
            id = 1L,
            name = "테스트 콘서트",
            date = testDate,
            totalSeats = 50
        )
        val member = Member(
            "testUser",
            100,
            "testUsername",
            "testPassword"
        )

        val reservation = Reservation(
            10L,
            concert,
            10,
            ReservationStatus.RESERVE,
            member
        )
        val reservationEntity = ReservationEntity.from(reservation)

        given(reservationJpaRepository.findReservationEntityByIdAndReserver_Id(10L, "testUser"))
            .willReturn(reservationEntity)

        //when
        val result = reservationRepositoryImpl.findReservationByIdAndReserver(10L, "testUser")

        //then
        assertThat(result.id).isEqualTo(10L)
        assertThat(result.concert.date).isEqualTo(testDate)
        assertThat(result.seatNumber).isEqualTo(10)
        assertThat(result.status).isEqualTo(ReservationStatus.RESERVE)
        assertThat(result.reserver).isNotNull
        assertThat(result.reserver!!.id).isEqualTo("testUser")
        assertThat(result.reserver!!.point).isEqualTo(100)
        assertThat(result.reserver!!.username).isEqualTo("testUsername")
        assertThat(result.reserver!!.password).isEqualTo("testPassword")
    }

    @Test
    fun `예약 id와 예약자 id로 예약을 가져올 때 예약이 없으면 예외를 던진다`() {
        //given
        given(reservationJpaRepository.findReservationEntityByIdAndReserver_Id(10L, "testUser"))
            .willReturn(null)

        //when &then
        assertThatThrownBy { reservationRepositoryImpl.findReservationByIdAndReserver(10L, "testUser") }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }
}
