package maeilkanji.maeilkanji.presentation.controller.api.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import maeilkanji.maeilkanji.business.service.dto.DailyContentDto

private const val MAX_BULK_SIZE = 500

data class BulkContentRequest(
    @field:NotEmpty(message = "등록할 콘텐츠가 없습니다.")
    @field:Size(max = MAX_BULK_SIZE, message = "한 번에 최대 ${MAX_BULK_SIZE}개까지 등록할 수 있습니다.")
    @field:Valid
    val contents: List<ContentRequest>,
) {
    fun toDailyContentDtos(): List<DailyContentDto> = contents.map { it.toDailyContentDto() }
}
