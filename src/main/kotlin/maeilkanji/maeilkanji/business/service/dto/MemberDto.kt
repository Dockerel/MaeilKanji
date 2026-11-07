package maeilkanji.maeilkanji.business.service.dto

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.domain.MemberStatus
import java.time.LocalDateTime
import java.util.*

data class MemberDto(
    val id: UUID,
    val email: String,
    var memberStatus: MemberStatus = MemberStatus.ACTIVE,
    var level: KanjiLevel,
    var streak: Int = 0,
    var lastAccessedAt: LocalDateTime? = null,
)
