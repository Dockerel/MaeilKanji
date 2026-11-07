package maeilkanji.maeilkanji.business.domain

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * 연속 학습(스트릭) 계산 규칙. 날짜(한국 시간) 단위로 판정한다.
 * - 첫 접속: 1
 * - 같은 날 재접속: 유지
 * - 마지막 접속이 전날: +1
 * - 하루 이상 건너뜀: 1부터 다시 시작
 */
object StreakPolicy {

    fun next(currentStreak: Int, lastAccessedAt: LocalDateTime?, now: LocalDateTime): Int {
        if (lastAccessedAt == null) return 1

        val dayGap = ChronoUnit.DAYS.between(lastAccessedAt.toLocalDate(), now.toLocalDate())
        return when {
            dayGap <= 0L -> maxOf(currentStreak, 1)
            dayGap == 1L -> currentStreak + 1
            else -> 1
        }
    }
}
