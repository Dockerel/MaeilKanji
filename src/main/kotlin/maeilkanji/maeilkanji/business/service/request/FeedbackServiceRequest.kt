package maeilkanji.maeilkanji.business.service.request

data class FeedbackServiceRequest(
    val memberId:String,
    val reason:String,
    val comment:String,
)
