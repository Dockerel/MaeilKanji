package maeilkanji.maeilkanji.business.service.dto

data class QuizStatsDto(
    val totalCount: Long,
    val correctCount: Long,
) {
    /** 정답률(%, 반올림). 응답이 없으면 null. */
    val correctRate: Int?
        get() = if (totalCount == 0L) null else Math.round(correctCount * 100.0 / totalCount).toInt()
}
