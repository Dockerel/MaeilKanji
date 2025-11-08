package maeilkanji.maeilkanji.presentation.controller.api.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import maeilkanji.maeilkanji.business.service.request.ChangeLevelServiceRequest

data class ChangeLevelRequest(
    @field:NotBlank(message = "멤버 Id는 필수입니다.")
    val memberId: String,
    @field:NotBlank(message = "레벨 선택은 필수입니다.")
    @field:Pattern(
        regexp = "BEGINNER|INTERMEDIATE|ADVANCED",
        message = "레벨은 BEGINNER, INTERMEDIATE, ADVANCED 중 하나여야 합니다."
    )
    val level: String,
) {
    fun toServiceRequest(): ChangeLevelServiceRequest = ChangeLevelServiceRequest(this.memberId, this.level)
}
