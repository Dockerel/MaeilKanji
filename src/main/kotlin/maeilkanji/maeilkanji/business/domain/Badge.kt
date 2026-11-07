package maeilkanji.maeilkanji.business.domain

enum class BadgeCategory(val label: String) {
    STREAK("연속 학습"),
    STUDY_DAYS("누적 학습"),
    QUIZ("퀴즈"),
}

/** 회원의 누적 학습 기록. 배지 획득 판정에 쓴다. */
data class LearningStats(
    val maxStreak: Int = 0,
    val studyDays: Int = 0,
    val quizSessions: Int = 0,
    val perfectSessions: Int = 0,
    val levelsPlayed: Set<KanjiLevel> = emptySet(),
)

/**
 * 프로필에 표시되는 업적 배지. 조건을 만족하면 자동으로 얻는다.
 * enum 이름은 DB(member.featured_badge)에 저장되므로 바꾸지 않는다.
 */
enum class Badge(
    val emoji: String,
    val title: String,
    val description: String,
    val category: BadgeCategory,
    private val condition: (LearningStats) -> Boolean,
) {
    STREAK_3("🌱", "새싹", "3일 연속 학습", BadgeCategory.STREAK, { it.maxStreak >= 3 }),
    STREAK_7("📅", "일주일 개근", "7일 연속 학습", BadgeCategory.STREAK, { it.maxStreak >= 7 }),
    STREAK_14("✨", "2주 완주", "14일 연속 학습", BadgeCategory.STREAK, { it.maxStreak >= 14 }),
    STREAK_30("🌙", "한 달의 기적", "30일 연속 학습", BadgeCategory.STREAK, { it.maxStreak >= 30 }),
    STREAK_50("🎋", "반백 일", "50일 연속 학습", BadgeCategory.STREAK, { it.maxStreak >= 50 }),
    STREAK_100("💯", "백일 수행", "100일 연속 학습", BadgeCategory.STREAK, { it.maxStreak >= 100 }),
    STREAK_200("🗻", "후지산 등정", "200일 연속 학습", BadgeCategory.STREAK, { it.maxStreak >= 200 }),
    STREAK_365("👑", "1년 개근", "365일 연속 학습", BadgeCategory.STREAK, { it.maxStreak >= 365 }),

    DAYS_10("🌿", "잔디 심기", "누적 10일 학습", BadgeCategory.STUDY_DAYS, { it.studyDays >= 10 }),
    DAYS_50("🌳", "작은 숲", "누적 50일 학습", BadgeCategory.STUDY_DAYS, { it.studyDays >= 50 }),
    DAYS_100("🌲", "울창한 숲", "누적 100일 학습", BadgeCategory.STUDY_DAYS, { it.studyDays >= 100 }),
    DAYS_300("🏯", "한자 장인", "누적 300일 학습", BadgeCategory.STUDY_DAYS, { it.studyDays >= 300 }),

    QUIZ_FIRST("✏️", "첫 도전", "퀴즈 첫 완료", BadgeCategory.QUIZ, { it.quizSessions >= 1 }),
    QUIZ_10("📝", "퀴즈 단골", "퀴즈 10회 완료", BadgeCategory.QUIZ, { it.quizSessions >= 10 }),
    QUIZ_50("📚", "퀴즈 마니아", "퀴즈 50회 완료", BadgeCategory.QUIZ, { it.quizSessions >= 50 }),
    QUIZ_100("🎓", "퀴즈 박사", "퀴즈 100회 완료", BadgeCategory.QUIZ, { it.quizSessions >= 100 }),
    PERFECT_1("🎯", "첫 만점", "퀴즈 만점 1회", BadgeCategory.QUIZ, { it.perfectSessions >= 1 }),
    PERFECT_10("🏹", "명사수", "퀴즈 만점 10회", BadgeCategory.QUIZ, { it.perfectSessions >= 10 }),
    PERFECT_30("🏆", "만점왕", "퀴즈 만점 30회", BadgeCategory.QUIZ, { it.perfectSessions >= 30 }),
    ALL_LEVELS("🗺️", "레벨 탐험가", "초급·중급·고급 퀴즈 모두 완료", BadgeCategory.QUIZ, {
        it.levelsPlayed.containsAll(KanjiLevel.values().toList())
    }),
    ;

    fun isEarned(stats: LearningStats): Boolean = condition(stats)

    companion object {
        fun earned(stats: LearningStats): List<Badge> = values().filter { it.isEarned(stats) }

        /** before 에서 after 로 바뀌면서 새로 얻은 배지 */
        fun newlyEarned(before: LearningStats, after: LearningStats): List<Badge> =
            values().filter { !it.isEarned(before) && it.isEarned(after) }

        fun fromName(name: String?): Badge? = values().firstOrNull { it.name == name }
    }
}
