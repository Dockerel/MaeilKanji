package maeilkanji.maeilkanji.infra.entity

import jakarta.persistence.*
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import java.util.*

/** 회원의 콘텐츠별 첫 퀴즈 세션의 문제별 정답 여부. 점수·정답률 집계에 쓴다. */
@Entity
@Table(
    name = "quiz_question_answer",
    uniqueConstraints = [UniqueConstraint(columnNames = ["member_id", "level", "content_id", "question_no"])],
)
class QuizQuestionAnswerEntity(
    @Column(name = "member_id", nullable = false)
    val memberId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    val level: KanjiLevel,
    @Column(name = "content_id", nullable = false)
    val contentId: Long,
    // 1부터 시작하는 문제 번호
    @Column(name = "question_no", nullable = false)
    val questionNo: Int,
    val correct: Boolean,
) : BaseEntity()
