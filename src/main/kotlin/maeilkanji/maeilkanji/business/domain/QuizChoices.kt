package maeilkanji.maeilkanji.business.domain

import maeilkanji.maeilkanji.business.service.dto.DailyContentDto
import kotlin.random.Random

/**
 * 객관식 보기 4개(정답 1 + 오답 3)와 정답 위치.
 * 보기 순서는 (레벨, 콘텐츠 id)로 고정된 시드로 섞는다. 메일, 학습 페이지, 채점이 모두 같은 순서를 쓴다.
 */
data class QuizChoices(
    val choices: List<String>,
    val answerIndex: Int,
) {
    fun isValidChoice(choiceIndex: Int): Boolean = choiceIndex in choices.indices

    fun isCorrect(choiceIndex: Int): Boolean = choiceIndex == answerIndex

    companion object {
        const val CHOICE_COUNT = 4

        /** 오답 보기가 3개 모두 있어야 객관식을 만든다. 없으면 null. */
        fun of(content: DailyContentDto): QuizChoices? {
            val id = content.id ?: return null
            val wrongAnswers = listOfNotNull(content.wrongAnswer1, content.wrongAnswer2, content.wrongAnswer3)
                .filter { it.isNotBlank() }
            if (wrongAnswers.size != CHOICE_COUNT - 1) return null

            val shuffled = (listOf(content.answer) + wrongAnswers).shuffled(Random(seed(content.level, id)))
            return QuizChoices(shuffled, shuffled.indexOf(content.answer))
        }

        private fun seed(level: KanjiLevel, contentId: Long): Long = level.ordinal * 1_000_003L + contentId
    }
}
