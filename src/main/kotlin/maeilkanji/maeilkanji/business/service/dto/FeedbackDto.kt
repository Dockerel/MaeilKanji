package maeilkanji.maeilkanji.business.service.dto

import java.time.LocalDateTime
import java.util.*

data class FeedbackDto(
    val id: UUID,
    val memberId: String,
    val reason: String,
    val comment: String,
    val createdAt: LocalDateTime,
)
