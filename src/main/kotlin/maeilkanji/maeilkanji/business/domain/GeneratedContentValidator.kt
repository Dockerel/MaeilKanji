package maeilkanji.maeilkanji.business.domain

import maeilkanji.maeilkanji.business.service.dto.DailyContentDto

/**
 * 자동 생성한 학습 문제가 `.claude/commands/create_question.md` 규칙을 지키는지 코드로 확인한다.
 * 문제가 없으면 빈 목록, 있으면 사람이 읽을 수 있는 사유 목록을 돌려준다. (재시도 프롬프트에 그대로 넣는다)
 */
object GeneratedContentValidator {

    // "上着(うわぎ)" → 한자 부분, 읽기
    private val WORD = Regex("^(.+)\\((.+)\\)$")

    fun validate(level: KanjiLevel, content: DailyContentDto, existing: List<DailyContentDto>): List<String> {
        val errors = mutableListOf<String>()

        val fields = mapOf(
            "kanji1" to content.kanji1, "yomi1" to content.yomi1, "meaning1" to content.meaning1,
            "kanji2" to content.kanji2, "yomi2" to content.yomi2, "meaning2" to content.meaning2,
            "word1" to content.word1, "wordMeaning1" to content.wordMeaning1,
            "word2" to content.word2, "wordMeaning2" to content.wordMeaning2,
            "word3" to content.word3, "wordMeaning3" to content.wordMeaning3,
            "word4" to content.word4, "wordMeaning4" to content.wordMeaning4,
            "sentence" to content.sentence, "sentenceMeaning" to content.sentenceMeaning,
            "quiz" to content.quiz, "answer" to content.answer, "explanation" to content.explanation,
            "wrongAnswer1" to content.wrongAnswer1, "wrongAnswer2" to content.wrongAnswer2,
            "wrongAnswer3" to content.wrongAnswer3, "sentenceTiles" to content.sentenceTiles,
        )
        fields.forEach { (name, value) ->
            when {
                value.isNullOrBlank() -> errors += "$name 이 비어 있습니다."
                value.contains(';') || value.contains('\n') -> errors += "$name 에 ; 또는 줄바꿈이 있습니다."
                value.contains("[translate:") -> errors += "$name 에 [translate:] 태그가 있습니다."
            }
        }
        if (errors.isNotEmpty()) return errors

        if (content.level != level) errors += "level 이 ${level.name} 이 아닙니다."
        if (content.kanji1.length != 1) errors += "kanji1 은 한자 한 글자여야 합니다."
        if (content.kanji2.length != 1) errors += "kanji2 는 한자 한 글자여야 합니다."
        if (content.kanji1 == content.kanji2) errors += "kanji1 과 kanji2 가 같습니다."

        if (!content.word1.contains(content.kanji1)) errors += "word1 에 kanji1(${content.kanji1})이 없습니다."
        if (!content.word2.contains(content.kanji1)) errors += "word2 에 kanji1(${content.kanji1})이 없습니다."
        if (!content.word3.contains(content.kanji2)) errors += "word3 에 kanji2(${content.kanji2})가 없습니다."
        if (!content.word4.contains(content.kanji2)) errors += "word4 에 kanji2(${content.kanji2})가 없습니다."

        val word1 = WORD.matchEntire(content.word1)
        if (word1 == null) {
            errors += "word1 이 '한자(읽기)' 형식이 아닙니다."
        } else {
            val expected = "${word1.groupValues[2]} (${word1.groupValues[1]})"
            if (content.answer != expected) errors += "answer 는 word1 과 같은 단어인 '$expected' 여야 합니다."
        }

        val choices = listOf(content.answer, content.wrongAnswer1!!, content.wrongAnswer2!!, content.wrongAnswer3!!)
        if (choices.toSet().size != choices.size) errors += "오답 보기 3개는 서로 다르고 answer 와도 달라야 합니다."

        if (!content.explanation.contains("오늘 배운 ${content.kanji1}")) {
            errors += "explanation 마지막 문장은 '오늘 배운 ${content.kanji1}(음독)를 사용한 단어였습니다!' 형식이어야 합니다."
        }

        val tiles = SentenceTiles.parse(content.sentence, content.sentenceTiles)
        if (tiles == null) {
            errors += "sentenceTiles 를 이어 붙이면 루비를 뺀 예문과 같아야 하고, 루비 한자 묶음을 블록 경계에서 자르면 안 됩니다."
        } else if (tiles.none { it.text.contains(content.kanji1) || it.text.contains(content.kanji2) }) {
            errors += "sentenceTiles 에 kanji1 또는 kanji2 가 들어간 블록이 없습니다."
        }

        val sentenceText = SentenceTiles.plainText(content.sentence)
        val usedWords = listOf(content.word1, content.word2, content.word3, content.word4)
            .mapNotNull { WORD.matchEntire(it)?.groupValues?.get(1) }
            .count { sentenceText.contains(it) || sentenceText.contains(it.stem()) }
        if (usedWords < 2) errors += "예문에 word1~word4 중 2개 이상이 들어가야 합니다."

        val pair = setOf(content.kanji1, content.kanji2)
        if (existing.any { setOf(it.kanji1, it.kanji2) == pair }) {
            errors += "한자 쌍 ${content.kanji1}/${content.kanji2} 은 이미 사용했습니다."
        }
        if (existing.any { it.level == level && it.kanji1 == content.kanji1 }) {
            errors += "${level.name} 에서 kanji1(${content.kanji1})은 이미 사용했습니다."
        }

        return errors
    }

    /** 활용형 단어(増える → 増えて)도 예문에서 찾을 수 있도록 끝의 오쿠리가나를 뗀다. */
    private fun String.stem(): String =
        dropLastWhile { it in 'ぁ'..'ゟ' }.ifEmpty { this }
}
