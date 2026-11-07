package maeilkanji.maeilkanji.business.repository

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.service.dto.QuizStatsDto
import java.util.*

interface QuizAnswerRepository {

    fun exists(memberId: UUID, level: KanjiLevel, contentId: Long): Boolean
    fun save(memberId: UUID, level: KanjiLevel, contentId: Long, choiceIndex: Int, correct: Boolean)
    fun getStats(level: KanjiLevel, contentId: Long): QuizStatsDto

}
