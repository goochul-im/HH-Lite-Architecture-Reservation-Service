package kr.hhplus.be.server.common.port

import io.jsonwebtoken.lang.Supplier

interface DistributeLockManager {

    fun<T> runWithLock(lockKey: String, task: Supplier<T>) : T

}
