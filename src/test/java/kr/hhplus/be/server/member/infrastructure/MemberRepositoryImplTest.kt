package kr.hhplus.be.server.member.infrastructure

import kr.hhplus.be.server.exception.ResourceNotFoundException
import kr.hhplus.be.server.member.domain.Member
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class MemberRepositoryImplTest {

    @Mock
    lateinit var memberJpaRepository: MemberJpaRepository

    @InjectMocks
    lateinit var memberRepositoryImpl: MemberRepositoryImpl

    @Test
    fun `username으로 Member를 찾을 수 있다`(){
        //given
        val findUsername = "testusername"
        val memberEntity = MemberEntity(
            "testuser",
            100,
            findUsername,
            "testpassword"
        )

        given(memberJpaRepository.findByUsername(findUsername)).willReturn(memberEntity)

        //when
        val result = memberRepositoryImpl.findByUsername(findUsername)

        //then
        assertThat(result.id).isEqualTo("testuser")
        assertThat(result.point).isEqualTo(100)
        assertThat(result.username).isEqualTo("testusername")
        assertThat(result.password).isEqualTo("testpassword")
    }

    @Test
    fun `id로 Member를 찾을 수 있다`(){
        //given
        val findId = "testuser"
        val memberEntity = MemberEntity(
            findId,
            100,
            "testusername",
            "testpassword"
        )

        given(memberJpaRepository.findById(findId)).willReturn(Optional.of(memberEntity))

        //when
        val result = memberRepositoryImpl.findById(findId)

        //then
        assertThat(result.id).isEqualTo("testuser")
        assertThat(result.point).isEqualTo(100)
        assertThat(result.username).isEqualTo("testusername")
        assertThat(result.password).isEqualTo("testpassword")
    }

    @Test
    fun `username으로 Member를 찾지 못하면 예외를 던진다`(){
        //given
        val findUsername = "testusername"

        given(memberJpaRepository.findByUsername(findUsername)).willReturn(null)

        //when & then
        assertThatThrownBy{ memberRepositoryImpl.findByUsername(findUsername) }.isInstanceOf(
            ResourceNotFoundException::class.java
        )
    }

    @Test
    fun `id로 Member를 찾지 못하면 예외를 던진다`(){
        //given
        val findId = "testuser"

        given(memberJpaRepository.findById(findId)).willReturn(Optional.empty())

        //when & then
        assertThatThrownBy{ memberRepositoryImpl.findById(findId) }.isInstanceOf(
            ResourceNotFoundException::class.java
        )
    }

    @Test
    fun `새로운 회원이 생성될 수 있다`(){
        //given
        val member = Member(username = "testUsername", password = "testPassword")
        given(memberJpaRepository.countByUsername("testUsername")).willReturn(0)
        given(memberJpaRepository.save(any<MemberEntity>())).willAnswer { it.arguments[0] }

        //when
        val result = memberRepositoryImpl.signUp(member)

        //then
        assertThat(result.username).isEqualTo("testUsername")
        assertThat(result.password).isEqualTo("testPassword")
        assertThat(result.point).isEqualTo(0)
    }

}
