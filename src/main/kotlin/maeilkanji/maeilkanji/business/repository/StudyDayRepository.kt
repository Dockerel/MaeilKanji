package maeilkanji.maeilkanji.business.repository

import java.time.LocalDate
import java.util.*

interface StudyDayRepository {

    /** 이미 기록된 날이면 아무것도 하지 않는다. */
    fun saveIfAbsent(memberId: UUID, date: LocalDate)

    fun count(memberId: UUID): Int

    fun findDates(memberId: UUID, from: LocalDate): Set<LocalDate>

    /** 회원별 누적 학습일. 기록이 없는 회원은 빠진다. */
    fun countAllByMember(): Map<UUID, Int>

}
