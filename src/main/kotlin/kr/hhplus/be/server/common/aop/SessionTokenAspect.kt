package kr.hhplus.be.server.common.aop

import kr.hhplus.be.server.common.annotation.LoginCheck
import kr.hhplus.be.server.common.exception.SessionTokenNotFoundException
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Aspect
@Component
class SessionTokenAspect {

    /**
     * @LoginCheck 어노테이션이 붙은 메소드 실행 전에 실행
     * @throws SessionTokenNotFoundException
     * 세션 토큰이 없으면 예외 발생
     */
    @Before("@annotation(check)")
    fun checkToken(check: LoginCheck) {

        val request = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request

        val session = request.session

        val tokenName = check.tokenName

        val token = session.getAttribute(tokenName) ?: throw SessionTokenNotFoundException("세션이 필요합니다")
    }

}
