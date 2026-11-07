package maeilkanji.maeilkanji.infra.repository

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.repository.QuizAnswerRepository
import maeilkanji.maeilkanji.business.service.dto.QuizStatsDto
import maeilkanji.maeilkanji.infra.entity.QuizAnswerEntity
import org.springframework.stereotype.Component
import java.util.*

@Component
class QuizAnswerRepositoryImpl(
    private val quizAnswerJpaRepository: QuizAnswerJpaRepository,
) : QuizAnswerRepository {

    override fun exists(memberId: UUID, level: KanjiLevel, contentId: Long): Boolean {
        return quizAnswerJpaRepository.existsByMemberIdAndLevelAndContentId(memberId, level, contentId)
    }

    override fun save(memberId: UUID, level: KanjiLevel, contentId: Long, choiceIndex: Int, correct: Boolean) {
        quizAnswerJpaRepository.save(QuizAnswerEntity(memberId, level, contentId, choiceIndex, correct))
    }

    override fun getStats(level: KanjiLevel, contentId: Long): QuizStatsDto {
        return QuizStatsDto(
            totalCount = quizAnswerJpaRepository.countByLevelAndContentId(level, contentId),
            correctCount = quizAnswerJpaRepository.countByLevelAndContentIdAndCorrect(level, contentId, true),
        )
    }

}
