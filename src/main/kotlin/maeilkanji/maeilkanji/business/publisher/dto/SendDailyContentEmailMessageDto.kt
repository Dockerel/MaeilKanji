package maeilkanji.maeilkanji.business.publisher.dto

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import java.util.*

data class SendDailyContentEmailMessageDto(
    val level: KanjiLevel,
    val contentIndex: Long,
    val memberId: UUID,
    val email: String,
)
