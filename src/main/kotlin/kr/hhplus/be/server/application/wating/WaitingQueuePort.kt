package kr.hhplus.be.server.application.wating

import java.time.LocalDate

interface WaitingQueuePort {

    fun add(userId: String): Long?
    fun getMyRank(userId: String) : Int
    fun issueWaitingToken(userId: String) : String
    fun validateToken(userId: String) : Boolean
    fun deleteToken(userId:String)
    fun renewalTokenTTL(userId:String)

}
