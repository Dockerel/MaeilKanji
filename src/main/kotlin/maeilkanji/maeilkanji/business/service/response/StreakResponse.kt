package maeilkanji.maeilkanji.business.service.response

data class StreakResponse(
    val streak: Int,
    val maxStreak: Int = streak,
    // 이번 접속으로 새로 얻은 배지 (연속·누적 학습)
    val newBadges: List<BadgeResponse> = emptyList(),
)
