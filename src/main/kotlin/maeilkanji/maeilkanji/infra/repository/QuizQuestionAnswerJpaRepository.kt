package maeilkanji.maeilkanji.infra.repository

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.infra.entity.QuizQuestionAnswerEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface QuizQuestionAnswerJpaRepository : JpaRepository<QuizQuestionAnswerEntity, UUID> {
    fun existsByMemberIdAndLevelAndContentId(memberId: UUID, level: KanjiLevel, contentId: Long): Boolean
    fun findAllByLevelAndContentId(level: KanjiLevel, contentId: Long): List<QuizQuestionAnswerEntity>
    fun findAllByMemberId(memberId: UUID): List<QuizQuestionAnswerEntity>
}
