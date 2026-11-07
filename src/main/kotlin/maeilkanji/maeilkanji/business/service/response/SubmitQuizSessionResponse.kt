package maeilkanji.maeilkanji.business.service.response

data class SubmitQuizSessionResponse(
    val results: List<Boolean>,
    val score: Int,
    val questionCount: Int,
    val participantCount: Int,
    val averageScore: Double?,
    val topPercent: Int?,
    val questionCorrectRates: List<Int?>,
    // 이번 세션 기록으로 새로 얻은 배지 (회원의 첫 세션만)
    val newBadges: List<BadgeResponse> = emptyList(),
)
