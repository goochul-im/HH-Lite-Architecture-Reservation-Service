package kr.hhplus.be.server.reservation.infrastructure

import kr.hhplus.be.server.exception.ResourceNotFoundException
import kr.hhplus.be.server.member.domain.Member
import kr.hhplus.be.server.member.infrastructure.MemberEntity
import kr.hhplus.be.server.reservation.domain.Reservation
import org.apache.commons.compress.harmony.pack200.IntList
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.*
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
    fun `날짜를 통해 예약된 좌석 번호들을 가져올 수 있다`(){
        //given
        val testDate = LocalDate.of(2025, 11, 25)
        val seats = listOf(1, 2, 3, 4, 5, 6, 10)
        given(reservationJpaRepository.getReservedSeatNumber(testDate)).willReturn(seats)
        
        //when
        val result = reservationRepositoryImpl.getReservedSeatNumber(testDate)

        //then
        assertThat(result).containsExactlyInAnyOrder(1,2,3,4,5,6,10)
    }

    @Test
    fun `예약 id와 예약자 id를 통해 알맞은 예약을 가져올 수 있다`(){
        //given
        val testDate = LocalDate.of(2025, 11, 25)
        val member = Member(
            "testUser",
            100,
            "testUsername",
            "testPassword"
        )

        val reservation = Reservation(
            10L,
            testDate,
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
        assertThat(result.date).isEqualTo(testDate)
        assertThat(result.seatNumber).isEqualTo(10)
        assertThat(result.status).isEqualTo(ReservationStatus.RESERVE)
        assertThat(result.reserver).isNotNull
        assertThat(result.reserver!!.id).isEqualTo("testUser")
        assertThat(result.reserver!!.point).isEqualTo(100)
        assertThat(result.reserver!!.username).isEqualTo("testUsername")
        assertThat(result.reserver!!.password).isEqualTo("testPassword")
    }

    @Test
    fun `예약 id와 예약자 id로 예약을 가져올 때 예약이 없으면 예외를 던진다`(){
        //given
        val testDate = LocalDate.of(2025, 11, 25)
        val member = Member(
            "testUser",
            100,
            "testUsername",
            "testPassword"
        )

        given(reservationJpaRepository.findReservationEntityByIdAndReserver_Id(10L, "testUser"))
            .willReturn(null)

        //when &then
        assertThatThrownBy { reservationRepositoryImpl.findReservationByIdAndReserver(10L, "testUser") }
            .isInstanceOf(ResourceNotFoundException::class.java)

    }

}
