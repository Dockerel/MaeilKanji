package maeilkanji.maeilkanji.business.service.response

data class SubmitQuizSessionResponse(
    val results: List<Boolean>,
    val score: Int,
    val questionCount: Int,
    val participantCount: Int,
    val averageScore: Double?,
    val topPercent: Int?,
    val questionCorrectRates: List<Int?>,
)
