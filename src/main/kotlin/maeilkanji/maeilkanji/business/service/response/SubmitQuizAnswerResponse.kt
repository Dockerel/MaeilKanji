package maeilkanji.maeilkanji.business.service.response

data class SubmitQuizAnswerResponse(
    val correct: Boolean,
    val answerIndex: Int,
    val totalCount: Long,
    val correctCount: Long,
    val correctRate: Int?,
)
