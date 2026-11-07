package maeilkanji.maeilkanji.infra.entity

import jakarta.persistence.*
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import java.util.*

/** 회원의 콘텐츠별 첫 퀴즈 응답. 정답률 집계에 쓴다. */
@Entity
@Table(
    name = "quiz_answer",
    uniqueConstraints = [UniqueConstraint(columnNames = ["member_id", "level", "content_id"])],
)
class QuizAnswerEntity(
    @Column(name = "member_id", nullable = false)
    val memberId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    val level: KanjiLevel,
    @Column(name = "content_id", nullable = false)
    val contentId: Long,
    val choiceIndex: Int,
    val correct: Boolean,
) : BaseEntity()
