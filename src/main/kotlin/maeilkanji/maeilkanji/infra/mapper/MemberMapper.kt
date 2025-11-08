package maeilkanji.maeilkanji.infra.mapper

import maeilkanji.maeilkanji.business.service.dto.MemberDto
import maeilkanji.maeilkanji.infra.entity.MemberEntity
import org.springframework.stereotype.Component

@Component
class MemberMapper {

    fun convert(memberEntity: MemberEntity): MemberDto {
        return MemberDto(memberEntity.id!!, memberEntity.email, memberEntity.memberStatus, memberEntity.level)
    }

}
