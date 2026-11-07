package maeilkanji.maeilkanji.business.service.dto

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import java.time.LocalDate

/** 회원이 기록한 퀴즈 세션 하나 (콘텐츠당 첫 세션) */
data class QuizSessionRecordDto(
    val level: KanjiLevel,
    val contentId: Long,
    val score: Int,
    val questionCount: Int,
    // 푼 날 (한국 시간)
    val solvedOn: LocalDate,
) {
    val perfect: Boolean get() = questionCount > 0 && score == questionCount
    val ratio: Double get() = if (questionCount == 0) 0.0 else score.toDouble() / questionCount
}
