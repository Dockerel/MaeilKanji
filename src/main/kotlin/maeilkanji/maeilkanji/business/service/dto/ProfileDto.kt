package maeilkanji.maeilkanji.business.service.dto

import maeilkanji.maeilkanji.business.domain.*

data class BadgeStatusDto(
    val badge: Badge,
    val earned: Boolean,
)

/** 프로필 페이지에 보여줄 값 모음 */
data class ProfileDto(
    // 미설정이면 null. 화면에는 displayName 을 쓴다.
    val nickname: String?,
    val bio: String?,
    val avatar: ProfileAvatar,
    val theme: ProfileTheme,
    val featuredBadge: Badge?,
    val currentStreak: Int,
    val maxStreak: Int,
    val studyDays: Int,
    val quizSessions: Int,
    val perfectSessions: Int,
    // 퀴즈 평균 정답률(%). 푼 퀴즈가 없으면 null
    val averageQuizRate: Int?,
    val heatmap: StudyHeatmap,
    val badges: List<BadgeStatusDto>,
    // 랭킹 순위. 순위표에 없으면(값이 0) null
    val ranks: Map<RankingType, Int?>,
    // 다음에 열릴 테마. 모두 열렸으면 null
    val nextTheme: ProfileTheme?,
) {
    val displayName: String get() = nickname ?: ANONYMOUS_NAME
    val earnedBadgeCount: Int get() = badges.count { it.earned }
    val currentStreakRank: Int? get() = ranks[RankingType.CURRENT_STREAK]
    val maxStreakRank: Int? get() = ranks[RankingType.MAX_STREAK]
    val studyDaysRank: Int? get() = ranks[RankingType.STUDY_DAYS]

    /** 다음 테마까지 남은 최장 스트릭 일수 */
    val daysToNextTheme: Int? get() = nextTheme?.let { it.requiredMaxStreak - maxStreak }

    companion object {
        const val ANONYMOUS_NAME = "익명의 학습자"
    }
}
