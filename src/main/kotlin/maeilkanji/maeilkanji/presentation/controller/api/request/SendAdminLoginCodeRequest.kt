package maeilkanji.maeilkanji.presentation.controller.api.request

import maeilkanji.maeilkanji.business.service.request.SendAdminLoginCodeServiceRequest

data class SendAdminLoginCodeRequest(
    val email: String,
) {
    fun toServiceRequest(): SendAdminLoginCodeServiceRequest {
        return SendAdminLoginCodeServiceRequest(email)
    }
}
