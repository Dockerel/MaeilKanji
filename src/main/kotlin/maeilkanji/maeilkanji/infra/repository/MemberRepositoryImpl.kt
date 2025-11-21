package maeilkanji.maeilkanji.infra.repository

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.domain.MemberStatus
import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import maeilkanji.maeilkanji.business.repository.MemberRepository
import maeilkanji.maeilkanji.business.service.dto.MemberDto
import maeilkanji.maeilkanji.business.service.dto.MemberSignupDto
import maeilkanji.maeilkanji.infra.entity.MemberEntity
import maeilkanji.maeilkanji.infra.mapper.MemberMapper
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.util.*

@Component
class MemberRepositoryImpl(
    private val memberJpaRepository: MemberJpaRepository,
    private val memberMapper: MemberMapper,
) : MemberRepository {

    override fun existByEmailAndMemberStatus(email: String, memberStatus: MemberStatus): Boolean {
        return memberJpaRepository.existsByEmailAndMemberStatus(email, memberStatus)
    }

    override fun existByEmail(email: String): Boolean {
        return memberJpaRepository.existsByEmail(email)
    }

    override fun findByEmail(email: String): MemberDto {
        val memberEntity = findMemberEntityByEmailOptional(email)!!
        return memberMapper.convert(memberEntity)
    }

    override fun update(email: String, findMember: MemberDto) {
        val memberEntity = findMemberEntityByEmailOptional(email)!!
        memberEntity.memberStatus = findMember.memberStatus
        memberEntity.level = findMember.level
    }

    override fun update(memberId: UUID, findMember: MemberDto) {
        val memberEntity = findMemberEntityByIdOptional(memberId)!!
        memberEntity.memberStatus = findMember.memberStatus
        memberEntity.level = findMember.level
    }

    override fun save(memberSignupDto: MemberSignupDto) {
        val memberEntity = MemberEntity(memberSignupDto.email, MemberStatus.ACTIVE, memberSignupDto.level)
        memberJpaRepository.save(memberEntity)
    }

    override fun findById(memberId: UUID): MemberDto {
        val memberEntity = memberJpaRepository.findById(memberId)
            .orElseThrow { MaeilKanjiException("Can't find user with memberId: $memberId", HttpStatus.NOT_FOUND) }
        return memberMapper.convert(memberEntity)
    }

    override fun findAllByMemberStatusAndLevel(
        memberStatus: MemberStatus,
        level: KanjiLevel
    ): List<MemberDto> {
        return memberJpaRepository.findAllByMemberStatusAndLevel(memberStatus, level).map { memberMapper.convert(it) }
    }

    private fun findMemberEntityByIdOptional(memberId: UUID): MemberEntity {
        val memberEntity = memberJpaRepository.findById(memberId)
            .orElseThrow { MaeilKanjiException("Can't find user with memberId: $memberId", HttpStatus.NOT_FOUND) }
        return memberEntity
    }

    private fun findMemberEntityByEmailOptional(email: String): MemberEntity {
        val memberEntity = memberJpaRepository.findByEmail(email)
            .orElseThrow { MaeilKanjiException("Can't find user with email: $email", HttpStatus.NOT_FOUND) }
        return memberEntity
    }

}