package kr.hhplus.be.server.common

import kr.hhplus.be.server.domain.member.Member
import kr.hhplus.be.server.domain.member.MemberRepository
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class DataInitializer(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder
) : ApplicationListener<ContextRefreshedEvent> {

    override fun onApplicationEvent(event: ContextRefreshedEvent) {

        if (memberRepository.count() <= 0) {
            memberRepository.save(Member(
                username = "gooch123",
                password = passwordEncoder.encode("goo6485")
            ))
        }
    }

}


