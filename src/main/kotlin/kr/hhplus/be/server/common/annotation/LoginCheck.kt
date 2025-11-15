package kr.hhplus.be.server.common.annotation

import kr.hhplus.be.server.auth.AuthConstant

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class LoginCheck (
    val tokenName: String = AuthConstant.userToken
)
