package kr.hhplus.be.server.domain.reservation.service

import kr.hhplus.be.server.reservation.dto.ReservationRequest
import kr.hhplus.be.server.auth.AuthService
import kr.hhplus.be.server.member.Member
import kr.hhplus.be.server.reservation.domain.Reservation
import kr.hhplus.be.server.reservation.domain.ReservationRepository
import kr.hhplus.be.server.reservation.domain.ReservationStatus
import kr.hhplus.be.server.reservation.service.ReservationService
import kr.hhplus.be.server.reservation.service.TempReservationAdaptor
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.*
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class ReservationServiceTest {

    @Mock
    private lateinit var reservationRepository: ReservationRepository

    @Mock
    private lateinit var tempReservationAdaptor: TempReservationAdaptor

    @Mock
    private lateinit var authService: AuthService

    @InjectMocks
    private lateinit var reservationService: ReservationService

    @Test
    @DisplayName("예약 생성 테스트")
    fun make_reservation_test() {
        // Given
        val memberId = "testmember"
        val date = LocalDate.now()
        val seatNumber = 5
        val request = ReservationRequest(date, memberId, seatNumber)

        val member = Member(id = memberId, username = "Test User", password = "testpassword" )
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
        verify(tempReservationAdaptor).save(date, reservation.id!!, seatNumber)
    }
}
