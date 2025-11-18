package kr.hhplus.be.server.application.wating.port

interface WaitingQueuePort {

    fun add(userId: String)
    fun getMyRank(userId: String) : Long?
    fun isEnteringKey(userId: String) : Boolean
    fun deleteToken(userId:String)
    fun renewalTokenTTL(userId:String)

    fun enteringQueue()
}
