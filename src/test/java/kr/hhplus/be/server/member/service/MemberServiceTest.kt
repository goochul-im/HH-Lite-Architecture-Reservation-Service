package kr.hhplus.be.server.member.service

import kr.hhplus.be.server.member.port.MemberRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@ExtendWith(MockitoExtension::class)
class MemberServiceTest {

    @Mock
    lateinit var memberRepository: MemberRepository

    @InjectMocks
    lateinit var service: MemberService

    @Test
    fun `username과 password로 회원가입을 할 수 있다`(){
        //when
        service.signUp("testUsername", "testPassword")

        //then
        verify(memberRepository, times(1)).save(any())
    }

}
