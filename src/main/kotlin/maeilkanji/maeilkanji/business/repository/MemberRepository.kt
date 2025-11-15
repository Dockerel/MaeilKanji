package maeilkanji.maeilkanji.business.repository

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.domain.MemberStatus
import maeilkanji.maeilkanji.business.service.dto.MemberDto
import maeilkanji.maeilkanji.business.service.dto.MemberSignupDto
import java.util.*

interface MemberRepository {

    fun existByEmailAndMemberStatus(email: String, memberStatus: MemberStatus): Boolean
    fun existByEmail(email: String): Boolean
    fun findByEmail(email: String): MemberDto
    fun update(email: String, findMember: MemberDto)
    fun update(memberId: UUID, findMember: MemberDto)
    fun save(memberSignupDto: MemberSignupDto)
    fun findById(memberId: UUID): MemberDto
    fun findAllByMemberStatusAndLevel(memberStatus: MemberStatus, level: KanjiLevel): List<MemberDto>

}
