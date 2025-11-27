package kr.hhplus.be.server.common

import kr.hhplus.be.server.member.infrastructure.MemberEntity
import kr.hhplus.be.server.member.infrastructure.MemberJpaRepository
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class DataInitializer(
    private val memberJpaRepository: MemberJpaRepository,
    private val passwordEncoder: PasswordEncoder
) : ApplicationListener<ContextRefreshedEvent> {

    override fun onApplicationEvent(event: ContextRefreshedEvent) {

        if (memberJpaRepository.count() <= 0) {
            memberJpaRepository.save(MemberEntity(
                username = "gooch123",
                password = passwordEncoder.encode("goo6485")
            ))
        }
    }

}


