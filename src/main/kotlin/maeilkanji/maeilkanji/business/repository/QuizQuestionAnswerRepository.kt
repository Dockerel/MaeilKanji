package maeilkanji.maeilkanji.business.repository

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.service.dto.QuizSessionStatsDto
import java.util.*

interface QuizQuestionAnswerRepository {

    fun exists(memberId: UUID, level: KanjiLevel, contentId: Long): Boolean

    /** results[i] 는 (i + 1)번 문제의 정답 여부 */
    fun saveAll(memberId: UUID, level: KanjiLevel, contentId: Long, results: List<Boolean>)

    fun getStats(level: KanjiLevel, contentId: Long, questionCount: Int): QuizSessionStatsDto

}
