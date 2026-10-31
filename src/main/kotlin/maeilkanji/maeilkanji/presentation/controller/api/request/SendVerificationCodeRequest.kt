package maeilkanji.maeilkanji.presentation.controller.api.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import maeilkanji.maeilkanji.business.service.request.SendVerificationCodeServiceRequest

data class SendVerificationCodeRequest(
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "이메일 형태는 필수입니다.")
    val email: String,
) {
    fun toServiceRequest(): SendVerificationCodeServiceRequest = SendVerificationCodeServiceRequest(this.email)
}
