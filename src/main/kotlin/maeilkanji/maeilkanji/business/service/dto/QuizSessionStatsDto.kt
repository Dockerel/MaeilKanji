package maeilkanji.maeilkanji.business.service.dto

import kotlin.math.ceil
import kotlin.math.roundToInt

data class QuizSessionStatsDto(
    // 참여 회원별 점수
    val memberScores: List<Int>,
    // 문제 번호(1부터) 순서대로 맞힌 회원 수
    val questionCorrectCounts: List<Int>,
) {
    val participantCount: Int get() = memberScores.size

    /** 평균 점수 (소수 첫째 자리). 참여자가 없으면 null. */
    val averageScore: Double?
        get() = if (memberScores.isEmpty()) null else (memberScores.average() * 10).roundToInt() / 10.0

    /** 문제별 정답률(%, 반올림). 참여자가 없으면 null. */
    val questionCorrectRates: List<Int?>
        get() = questionCorrectCounts.map { count ->
            if (participantCount == 0) null else (count * 100.0 / participantCount).roundToInt()
        }

    /** 이 점수가 상위 몇 %인지 (같은 점수 포함, 올림). 참여자가 없으면 null. */
    fun topPercent(score: Int): Int? {
        if (memberScores.isEmpty()) return null
        val atLeast = memberScores.count { it >= score }
        return ceil(atLeast * 100.0 / participantCount).toInt().coerceIn(1, 100)
    }
}
