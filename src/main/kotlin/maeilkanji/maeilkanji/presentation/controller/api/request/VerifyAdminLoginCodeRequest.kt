package maeilkanji.maeilkanji.presentation.controller.api.request

import maeilkanji.maeilkanji.business.service.request.VerifyAdminLoginCodeServiceRequest

data class VerifyAdminLoginCodeRequest(
    val email: String,
    val code: String,
) {
    fun toServiceRequest(): VerifyAdminLoginCodeServiceRequest {
        return VerifyAdminLoginCodeServiceRequest(email, code)
    }
}
