package maeilkanji.maeilkanji.presentation.controller.api.request

import jakarta.validation.constraints.NotBlank
import maeilkanji.maeilkanji.business.service.request.StopMailServiceRequest

data class StopMailRequest(
    @field:NotBlank(message = "멤버 Id는 필수입니다.")
    val memberId: String,
) {
    fun toServiceRequest(): StopMailServiceRequest = StopMailServiceRequest(this.memberId)
}
