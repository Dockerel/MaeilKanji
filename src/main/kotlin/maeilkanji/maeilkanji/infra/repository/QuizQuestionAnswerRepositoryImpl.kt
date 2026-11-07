package maeilkanji.maeilkanji.infra.repository

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.repository.QuizQuestionAnswerRepository
import maeilkanji.maeilkanji.business.service.dto.QuizSessionRecordDto
import maeilkanji.maeilkanji.business.service.dto.QuizSessionStatsDto
import maeilkanji.maeilkanji.infra.entity.QuizQuestionAnswerEntity
import org.springframework.stereotype.Component
import java.util.*

@Component
class QuizQuestionAnswerRepositoryImpl(
    private val quizQuestionAnswerJpaRepository: QuizQuestionAnswerJpaRepository,
) : QuizQuestionAnswerRepository {

    override fun exists(memberId: UUID, level: KanjiLevel, contentId: Long): Boolean {
        return quizQuestionAnswerJpaRepository.existsByMemberIdAndLevelAndContentId(memberId, level, contentId)
    }

    override fun saveAll(memberId: UUID, level: KanjiLevel, contentId: Long, results: List<Boolean>) {
        val entities = results.mapIndexed { i, correct ->
            QuizQuestionAnswerEntity(memberId, level, contentId, questionNo = i + 1, correct = correct)
        }
        quizQuestionAnswerJpaRepository.saveAll(entities)
    }

    // 하루 응답자 수 규모(수백 명 × 5행)라 메모리에서 집계한다.
    override fun getStats(level: KanjiLevel, contentId: Long, questionCount: Int): QuizSessionStatsDto {
        val rows = quizQuestionAnswerJpaRepository.findAllByLevelAndContentId(level, contentId)
        val memberScores = rows.groupBy { it.memberId }.values.map { answers -> answers.count { it.correct } }
        val questionCorrectCounts = (1..questionCount).map { no -> rows.count { it.questionNo == no && it.correct } }
        return QuizSessionStatsDto(memberScores, questionCorrectCounts)
    }

    // 회원 한 명의 기록은 많아야 (학습일 × 5행)이라 메모리에서 세션 단위로 묶는다.
    override fun findSessionsByMember(memberId: UUID): List<QuizSessionRecordDto> {
        return quizQuestionAnswerJpaRepository.findAllByMemberId(memberId)
            .groupBy { it.level to it.contentId }
            .map { (key, answers) ->
                QuizSessionRecordDto(
                    level = key.first,
                    contentId = key.second,
                    score = answers.count { it.correct },
                    questionCount = answers.size,
                    solvedOn = answers.minOf { it.createdAt }.toLocalDate(),
                )
            }
    }

}
