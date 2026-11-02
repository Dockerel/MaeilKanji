package maeilkanji.maeilkanji.presentation.controller.api.request

import maeilkanji.maeilkanji.business.service.request.FeedbackServiceRequest

data class FeedbackRequest(
    val memberId: String,
    val reason: String,
    val comment: String,
    // 피드백을 보낸 위치 (예: "초급 #3"). 있으면 관리자가 알아볼 수 있게 코멘트 앞에 붙인다.
    val context: String = "",
) {
    fun toServiceRequest(): FeedbackServiceRequest =
        FeedbackServiceRequest(this.memberId, this.reason, commentWithContext())

    private fun commentWithContext(): String =
        if (context.isBlank()) comment else "[${context.trim()}] $comment"
}
