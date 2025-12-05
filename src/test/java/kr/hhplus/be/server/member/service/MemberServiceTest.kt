package kr.hhplus.be.server.member.service

import kr.hhplus.be.server.member.domain.Member
import kr.hhplus.be.server.member.infrastructure.MemberEntity
import kr.hhplus.be.server.member.port.EncodeService
import kr.hhplus.be.server.member.port.MemberRepository
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@ExtendWith(MockitoExtension::class)
class MemberServiceTest {

    @Mock
    lateinit var memberRepository: MemberRepository

    @Mock
    lateinit var encodeService: EncodeService

    @InjectMocks
    lateinit var service: MemberService

    @Test
    fun `username과 password로 회원가입을 할 수 있고, 비밀번호는 암호화되어야 한다`(){
        // given
        val member = Member(
            username = "testUsername",
            password = "testPassword"
        )
        given(encodeService.encode("testPassword")).willReturn("encodePassword")
        given(memberRepository.signUp(any<Member>())).willReturn(Member(
            "testId",
            username = "testUsername",
            password = "encodePassword"
        ))

        // when
        val result = service.signUp("testUsername", "testPassword")

        // then
        assertThat(result.id).isEqualTo("testId")
        assertThat(result.point).isEqualTo(0)
        assertThat(result.username).isEqualTo("testUsername")
        assertThat(result.password).isEqualTo("encodePassword")
    }

}
