package maeilkanji.maeilkanji.business.service.request

data class VerifyAdminLoginCodeServiceRequest(
    val email: String,
    val code: String,
)
