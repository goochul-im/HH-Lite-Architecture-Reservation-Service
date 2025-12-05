package kr.hhplus.be.server.member.infrastructure

import kr.hhplus.be.server.member.port.EncodeService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class EncodeServiceImpl(
    private val passwordEncoder: PasswordEncoder
) : EncodeService {

    override fun encode(target: String): String {
        return passwordEncoder.encode(target)
    }

}
