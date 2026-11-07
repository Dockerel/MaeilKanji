package maeilkanji.maeilkanji.infra.entity

import jakarta.persistence.*
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.domain.MemberStatus
import org.hibernate.annotations.ColumnDefault
import java.time.LocalDateTime

@Entity
@Table(name = "member")
class MemberEntity(
    @Column(unique = true)
    val email: String,
    @Enumerated(EnumType.STRING)
    var memberStatus: MemberStatus,
    @Enumerated(EnumType.STRING)
    var level: KanjiLevel,
    // 연속 학습 일수. ddl-auto=update 로 기존 행에 컬럼이 추가될 때 0 으로 채운다.
    @ColumnDefault("0")
    @Column(nullable = false)
    var streak: Int = 0,
    // 학습 페이지 마지막 접속 시각 (한국 시간)
    var lastAccessedAt: LocalDateTime? = null,
) : BaseEntity()