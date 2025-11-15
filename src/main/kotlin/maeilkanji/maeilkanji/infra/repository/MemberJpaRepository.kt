package maeilkanji.maeilkanji.infra.repository

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.domain.MemberStatus
import maeilkanji.maeilkanji.infra.entity.MemberEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface MemberJpaRepository : JpaRepository<MemberEntity, UUID> {

    fun existsByEmailAndMemberStatus(email: String, memberStatus: MemberStatus): Boolean

    fun existsByEmail(email: String): Boolean

    fun findByEmail(email: String): Optional<MemberEntity>

    fun findAllByMemberStatusAndLevel(memberStatus: MemberStatus, level: KanjiLevel): List<MemberEntity>

}