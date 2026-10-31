package maeilkanji.maeilkanji.business.service.request

data class ChangeLevelServiceRequest(
    val memberId: String,
    val level: String,
)
