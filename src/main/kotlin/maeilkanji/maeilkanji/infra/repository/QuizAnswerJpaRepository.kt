package maeilkanji.maeilkanji.infra.repository

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.infra.entity.QuizAnswerEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface QuizAnswerJpaRepository : JpaRepository<QuizAnswerEntity, UUID> {
    fun existsByMemberIdAndLevelAndContentId(memberId: UUID, level: KanjiLevel, contentId: Long): Boolean
    fun countByLevelAndContentId(level: KanjiLevel, contentId: Long): Long
    fun countByLevelAndContentIdAndCorrect(level: KanjiLevel, contentId: Long, correct: Boolean): Long
}
