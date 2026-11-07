package maeilkanji.maeilkanji.infra.mapper

import maeilkanji.maeilkanji.business.domain.Badge
import maeilkanji.maeilkanji.business.domain.ProfileAvatar
import maeilkanji.maeilkanji.business.domain.ProfileTheme
import maeilkanji.maeilkanji.business.service.dto.MemberDto
import maeilkanji.maeilkanji.infra.entity.MemberEntity
import org.springframework.stereotype.Component

@Component
class MemberMapper {

    fun convert(memberEntity: MemberEntity): MemberDto {
        return MemberDto(
            memberEntity.id!!,
            memberEntity.email,
            memberEntity.memberStatus,
            memberEntity.level,
            memberEntity.streak,
            memberEntity.lastAccessedAt,
            memberEntity.maxStreak,
            memberEntity.nickname,
            memberEntity.bio,
            ProfileAvatar.fromName(memberEntity.avatar),
            ProfileTheme.fromName(memberEntity.profileTheme),
            Badge.fromName(memberEntity.featuredBadge),
        )
    }

}
