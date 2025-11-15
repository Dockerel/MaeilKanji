package maeilkanji.maeilkanji.infra.entity

import jakarta.persistence.*
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.domain.MemberStatus

@Entity
@Table(name = "member")
class MemberEntity(
    @Column(unique = true)
    val email: String,
    @Enumerated(EnumType.STRING)
    var memberStatus: MemberStatus,
    @Enumerated(EnumType.STRING)
    var level: KanjiLevel,
    var contentIndex: Long = 1L,
) : BaseEntity() {
    fun increaseContextIndex() {
        this.contentIndex++
    }
}