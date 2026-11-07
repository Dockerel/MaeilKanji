package maeilkanji.maeilkanji.business.service.dto

import maeilkanji.maeilkanji.business.domain.Badge
import maeilkanji.maeilkanji.business.domain.ProfileAvatar
import maeilkanji.maeilkanji.business.domain.RankingType

data class RankingEntryDto(
    val rank: Int,
    // 닉네임이 없으면 null (익명, 프로필 링크 없음)
    val nickname: String?,
    val avatar: ProfileAvatar,
    val featuredBadge: Badge?,
    val value: Int,
    val me: Boolean,
) {
    val displayName: String get() = nickname ?: ProfileDto.ANONYMOUS_NAME
}

data class RankingDto(
    val type: RankingType,
    val entries: List<RankingEntryDto>,
    // 보고 있는 회원의 순위. 회원이 아니거나 순위표 밖(값 0)이면 null
    val myEntry: RankingEntryDto?,
    val totalRanked: Int,
)
