package maeilkanji.maeilkanji.business.domain

import maeilkanji.maeilkanji.business.service.dto.DailyContentDto
import kotlin.random.Random

/**
 * 하루 5문제를 만든다. 같은 (레벨, 콘텐츠)는 항상 같은 문제와 보기 순서가 나온다.
 *
 * 1. 단어 고르기 (한→일): 기존 미니 퀴즈. 메일 보기 버튼과 같은 순서 ([QuizChoices])
 * 2. 뜻 고르기 (일→한): 오늘의 kanji2 단어(word3)
 * 3. 빈칸 채우기: 어제 예문
 * 4. 빈칸 채우기: 그저께 예문
 * 5. 문장 만들기: 오늘 예문 (한국어 뜻을 보고 일본어 블록 배열)
 *
 * 빈칸 문제를 지난 예문으로 내는 이유: 오늘 예문으로 내면 5번 문장 만들기의 답이 드러난다. 복습 효과도 있다.
 */
object QuizSessionFactory {
    const val QUESTION_COUNT = 5
    const val ARRANGE_MAX_TILES = 10
    private const val ARRANGE_DISTRACTOR_COUNT = 2
    private const val WRONG_OPTION_COUNT = 3
    private val WORD_WITH_READING = Regex("^(.+?)[(（](.+)[)）]$")

    /**
     * @param today 오늘 콘텐츠
     * @param pool 같은 레벨의 전체 콘텐츠 (오답 보기, 지난 예문 출처)
     * @return 오늘 콘텐츠에 오답 보기나 예문 블록이 없으면 null
     */
    fun create(today: DailyContentDto, pool: List<DailyContentDto>): QuizSession? {
        val todayId = today.id ?: return null
        val firstQuestion = wordToJapanese(today) ?: return null
        val arrange = arrange(today, pool) ?: return null
        val others = pool.filter { it.id != todayId }

        val blankSources = pastSentences(today, others)
        val blanks = blankSources.mapIndexed { i, source -> fillBlank(today, source, others, questionNo = 3 + i) }
        if (blanks.size < 2 || blanks.any { it == null }) return null

        return QuizSession(
            level = today.level,
            contentId = todayId,
            questions = listOf(firstQuestion, wordToKorean(today, others), blanks[0]!!, blanks[1]!!, arrange),
        )
    }

    private fun wordToJapanese(content: DailyContentDto): QuizQuestion? {
        val choices = QuizChoices.of(content) ?: return null
        return QuizQuestion(
            type = QuizQuestionType.WORD_TO_JAPANESE,
            prompt = content.quiz,
            options = choices.choices.map { QuizOption(it, SentenceTiles.escape(it)) },
            answerIndex = choices.answerIndex,
            explanation = content.explanation,
        )
    }

    private fun wordToKorean(content: DailyContentDto, others: List<DailyContentDto>): QuizQuestion {
        val random = random(content, questionNo = 2)
        val (word, reading) = splitWord(content.word3)
        val answer = content.wordMeaning3

        val sameDay = listOf(content.wordMeaning1, content.wordMeaning2, content.wordMeaning4)
        val fromOthers = others.flatMap { listOf(it.wordMeaning1, it.wordMeaning2, it.wordMeaning3, it.wordMeaning4) }
            .shuffled(random)
        val wrongs = (sameDay + fromOthers).filter { it != answer }.distinct().take(WRONG_OPTION_COUNT)
        val options = (wrongs + answer).shuffled(random)

        return QuizQuestion(
            type = QuizQuestionType.WORD_TO_KOREAN,
            prompt = "다음 단어의 뜻은 무엇일까요?",
            promptHtml = wordHtml(word, reading),
            options = options.map { QuizOption(it, SentenceTiles.escape(it)) },
            answerIndex = options.indexOf(answer),
            explanation = "$word(${reading ?: word})는 '$answer'라는 뜻입니다.",
        )
    }

    private fun fillBlank(
        today: DailyContentDto,
        source: DailyContentDto,
        others: List<DailyContentDto>,
        questionNo: Int,
    ): QuizQuestion? {
        val tiles = SentenceTiles.parse(source.sentence, source.sentenceTiles) ?: return null
        val target = blankTargetIndex(source, tiles) ?: return null
        val answer = tiles[target]
        val random = random(today, questionNo)

        val wrongs = others.filter { it.id != source.id }
            .shuffled(random)
            .mapNotNull { other ->
                SentenceTiles.parse(other.sentence, other.sentenceTiles)?.let { t -> blankTargetIndex(other, t)?.let { t[it] } }
            }
            .filter { it.text != answer.text }
            .distinctBy { it.text }
            .take(WRONG_OPTION_COUNT)
        if (wrongs.size < WRONG_OPTION_COUNT) return null
        val options = (wrongs + answer).shuffled(random)

        val todayId = today.id!!
        val sourceLabel = when (source.id) {
            todayId - 1 -> "어제 배운 예문"
            todayId - 2 -> "그저께 배운 예문"
            else -> "지난 예문"
        }
        return QuizQuestion(
            type = QuizQuestionType.FILL_BLANK,
            prompt = "빈칸에 알맞은 말을 고르세요.",
            promptHtml = tiles.mapIndexed { i, tile ->
                if (i == target) """<span class="blank" aria-label="빈칸"></span>""" else tile.html
            }.joinToString(""),
            source = sourceLabel,
            hint = source.sentenceMeaning,
            options = options.map { QuizOption(it.text, it.html) },
            answerIndex = options.indexOf(answer),
            explanation = source.sentenceMeaning,
            explanationHtml = SentenceTiles.html(source.sentence),
        )
    }

    private fun arrange(today: DailyContentDto, pool: List<DailyContentDto>): QuizQuestion? {
        val tiles = SentenceTiles.parse(today.sentence, today.sentenceTiles) ?: return null
        val answerTiles = SentenceTiles.mergeForArrange(tiles, ARRANGE_MAX_TILES)
        val answerTexts = answerTiles.map { it.text }
        val random = random(today, questionNo = 5)

        val distractors = pool.filter { it.id != today.id }
            .shuffled(random)
            .flatMap { SentenceTiles.parse(it.sentence, it.sentenceTiles).orEmpty() }
            .filter { !it.isPunctuation && it.text.length >= 2 && it.text !in answerTexts }
            .distinctBy { it.text }
            .take(ARRANGE_DISTRACTOR_COUNT)

        return QuizQuestion(
            type = QuizQuestionType.ARRANGE,
            prompt = "다음 문장을 일본어로 만들어 보세요.",
            hint = today.sentenceMeaning,
            options = (answerTiles + distractors).shuffled(random).map { QuizOption(it.text, it.html) },
            answerTexts = answerTexts,
            explanation = today.sentenceMeaning,
            explanationHtml = SentenceTiles.html(today.sentence),
        )
    }

    /** 어제, 그저께 콘텐츠. 없으면(서비스 초기) 다른 콘텐츠를 id 큰 순으로 대신 쓴다. */
    private fun pastSentences(today: DailyContentDto, others: List<DailyContentDto>): List<DailyContentDto> {
        val usable = others.filter { SentenceTiles.isValid(it.sentence, it.sentenceTiles) }
        val byId = usable.associateBy { it.id }
        val todayId = today.id!!
        val preferred = listOfNotNull(byId[todayId - 1], byId[todayId - 2])
        val fallback = usable.filter { it !in preferred }.sortedByDescending { it.id }
        return (preferred + fallback).take(2)
    }

    /** 빈칸으로 만들 블록: kanji1 이 든 블록, 없으면 kanji2 가 든 블록 */
    private fun blankTargetIndex(content: DailyContentDto, tiles: List<SentenceTile>): Int? =
        tiles.indexOfFirst { content.kanji1 in it.text }.takeIf { it >= 0 }
            ?: tiles.indexOfFirst { content.kanji2 in it.text }.takeIf { it >= 0 }

    private fun splitWord(word: String): Pair<String, String?> {
        val match = WORD_WITH_READING.find(word.trim()) ?: return word.trim() to null
        return match.groupValues[1].trim() to match.groupValues[2].trim()
    }

    private fun wordHtml(word: String, reading: String?): String =
        if (reading == null) SentenceTiles.escape(word)
        else "<ruby>${SentenceTiles.escape(word)}<rt>${SentenceTiles.escape(reading)}</rt></ruby>"

    private fun random(content: DailyContentDto, questionNo: Int): Random =
        Random(content.level.ordinal * 1_000_003L + content.id!! * 31L + questionNo)
}
