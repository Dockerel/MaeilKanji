package maeilkanji.maeilkanji.business.service.request

data class VerifyVerificationCodeServiceRequest(
    val email: String,
    val code: String,
)
