package maeilkanji.maeilkanji.presentation.controller.api.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.service.request.SubmitQuizAnswerServiceRequest

data class SubmitQuizAnswerRequest(
    @field:NotBlank(message = "레벨은 필수입니다.")
    @field:Pattern(
        regexp = "BEGINNER|INTERMEDIATE|ADVANCED",
        message = "레벨은 BEGINNER, INTERMEDIATE, ADVANCED 중 하나여야 합니다."
    )
    val level: String,
    @field:NotNull(message = "콘텐츠 Id는 필수입니다.")
    val contentId: Long?,
    @field:NotNull(message = "보기 번호는 필수입니다.")
    val choice: Int?,
    val memberId: String? = null,
) {
    fun toServiceRequest(): SubmitQuizAnswerServiceRequest =
        SubmitQuizAnswerServiceRequest(KanjiLevel.valueOf(level), contentId!!, choice!!, memberId)
}
