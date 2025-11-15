package maeilkanji.maeilkanji.presentation.controller.api.request

import maeilkanji.maeilkanji.business.service.request.FeedbackServiceRequest

data class FeedbackRequest(
    val memberId: String,
    val reason: String,
    val comment: String,
) {
    fun toServiceRequest(): FeedbackServiceRequest = FeedbackServiceRequest(this.memberId, this.reason, this.comment)
}
