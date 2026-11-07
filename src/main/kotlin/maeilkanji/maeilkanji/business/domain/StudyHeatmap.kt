package maeilkanji.maeilkanji.business.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** 하루 동안의 학습 활동. 퀴즈를 여러 번 풀었으면 가장 높은 점수 비율을 쓴다. */
data class DailyActivity(
    val visited: Boolean = false,
    // 그날 푼 퀴즈의 최고 정답 비율 (0.0 ~ 1.0). 안 풀었으면 null
    val bestQuizRatio: Double? = null,
) {
    /**
     * 잔디 색 단계 (0 ~ 4)
     * 0: 기록 없음 / 1: 학습 페이지 방문 / 2: 퀴즈 완료 / 3: 퀴즈 60% 이상 / 4: 퀴즈 만점
     */
    val level: Int
        get() = when {
            bestQuizRatio != null && bestQuizRatio >= 1.0 -> 4
            bestQuizRatio != null && bestQuizRatio >= 0.6 -> 3
            bestQuizRatio != null -> 2
            visited -> 1
            else -> 0
        }

    fun merge(other: DailyActivity): DailyActivity = DailyActivity(
        visited = visited || other.visited,
        bestQuizRatio = listOfNotNull(bestQuizRatio, other.bestQuizRatio).maxOrNull(),
    )
}

data class HeatmapCell(
    val date: LocalDate,
    val level: Int,
    // 오늘 이후 날짜. 화면에서 비워 둔다.
    val future: Boolean,
    val today: Boolean = false,
)

/** 한 주(일~토) 열. monthLabel 은 그 주에 새 달이 시작될 때만 값이 있다. 예: "11월" */
data class HeatmapWeek(
    val monthLabel: String?,
    val days: List<HeatmapCell>,
)

/**
 * GitHub 잔디처럼 최근 1년(53주)의 학습 기록을 주 단위 열로 만든다.
 * 열은 일요일부터 토요일까지 7칸이고, 마지막 열은 오늘이 포함된 주다.
 */
data class StudyHeatmap(
    val weeks: List<HeatmapWeek>,
) {
    val activeDays: Int get() = weeks.sumOf { week -> week.days.count { it.level > 0 } }

    companion object {
        const val WEEK_COUNT = 53

        fun of(activities: Map<LocalDate, DailyActivity>, today: LocalDate, weekCount: Int = WEEK_COUNT): StudyHeatmap {
            val lastSunday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
            val firstSunday = lastSunday.minusWeeks((weekCount - 1).toLong())

            val weeks = (0 until weekCount).map { w ->
                val weekStart = firstSunday.plusWeeks(w.toLong())
                val days = (0 until 7).map { d ->
                    val date = weekStart.plusDays(d.toLong())
                    HeatmapCell(
                        date = date,
                        level = if (date.isAfter(today)) 0 else activities[date]?.level ?: 0,
                        future = date.isAfter(today),
                        today = date == today,
                    )
                }
                // 이 주 안에 1일이 있으면 달 이름을 붙인다. 첫 열은 다음 달 이름과 겹치지 않을 때(3주 이상 여유)만 붙인다.
                val monthStart = days.firstOrNull { it.date.dayOfMonth == 1 }?.date
                    ?: if (w == 0 && weekStart.plusWeeks(3).monthValue == weekStart.monthValue) weekStart else null
                HeatmapWeek(monthStart?.let { "${it.monthValue}월" }, days)
            }
            return StudyHeatmap(weeks)
        }
    }
}
