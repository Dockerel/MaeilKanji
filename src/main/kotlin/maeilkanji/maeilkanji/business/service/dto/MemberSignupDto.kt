package maeilkanji.maeilkanji.business.service.dto

import maeilkanji.maeilkanji.business.domain.KanjiLevel

data class MemberSignupDto(
    val email: String,
    val level: KanjiLevel,
)
