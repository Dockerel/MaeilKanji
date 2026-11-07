package maeilkanji.maeilkanji.business.service.dto

import maeilkanji.maeilkanji.business.domain.Badge
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.domain.MemberStatus
import maeilkanji.maeilkanji.business.domain.ProfileAvatar
import maeilkanji.maeilkanji.business.domain.ProfileTheme
import java.time.LocalDateTime
import java.util.*

data class MemberDto(
    val id: UUID,
    val email: String,
    var memberStatus: MemberStatus = MemberStatus.ACTIVE,
    var level: KanjiLevel,
    var streak: Int = 0,
    var lastAccessedAt: LocalDateTime? = null,
    var maxStreak: Int = 0,
    var nickname: String? = null,
    var bio: String? = null,
    var avatar: ProfileAvatar? = null,
    var profileTheme: ProfileTheme? = null,
    var featuredBadge: Badge? = null,
)
