package maeilkanji.maeilkanji.presentation.controller.api.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import maeilkanji.maeilkanji.business.service.request.SignupServiceRequest

data class SignupRequest(
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "이메일 형태는 필수입니다.")
    val email: String,
    @field:NotBlank(message = "레벨 선택은 필수입니다.")
    @field:Pattern(
        regexp = "BEGINNER|INTERMEDIATE|ADVANCED",
        message = "레벨은 BEGINNER, INTERMEDIATE, ADVANCED 중 하나여야 합니다."
    )
    val level: String,
) {
    fun toServiceRequest(): SignupServiceRequest = SignupServiceRequest(this.email, this.level)
}
