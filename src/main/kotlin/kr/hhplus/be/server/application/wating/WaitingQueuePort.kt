package kr.hhplus.be.server.application.wating

import java.time.LocalDate

interface WaitingQueuePort {

    fun getUsedNumberByDate(date: LocalDate) : List<Int>
    fun saveNumber(userId : String,num: Int, date: LocalDate)

}
