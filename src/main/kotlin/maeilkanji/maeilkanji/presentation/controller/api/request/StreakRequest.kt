package maeilkanji.maeilkanji.presentation.controller.api.request

import jakarta.validation.constraints.NotBlank

data class StreakRequest(
    @field:NotBlank(message = "멤버 Id는 필수입니다.")
    val memberId: String,
)
