package maeilkanji.maeilkanji.business.domain

enum class RankingType(val label: String, val unit: String) {
    CURRENT_STREAK("현재 스트릭", "일 연속"),
    MAX_STREAK("최장 스트릭", "일 연속"),
    STUDY_DAYS("누적 학습일", "일"),
    ;

    /** URL 쿼리용 소문자 이름. 예: /ranking?type=max_streak */
    val path: String get() = name.lowercase()

    companion object {
        fun fromPath(path: String?): RankingType = values().firstOrNull { it.path == path } ?: CURRENT_STREAK
    }
}
