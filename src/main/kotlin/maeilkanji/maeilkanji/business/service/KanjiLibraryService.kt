package maeilkanji.maeilkanji.business.service

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.domain.SentenceTiles
import maeilkanji.maeilkanji.business.repository.DailyContentRepository
import maeilkanji.maeilkanji.business.service.dto.DailyContentDto
import maeilkanji.maeilkanji.business.service.dto.KanjiEntryDto
import maeilkanji.maeilkanji.business.service.dto.KanjiLessonRef
import maeilkanji.maeilkanji.business.service.dto.KanjiSentence
import maeilkanji.maeilkanji.business.service.dto.KanjiWord
import maeilkanji.maeilkanji.business.service.dto.LessonNeighbors
import org.springframework.stereotype.Service

/**
 * 검색 유입용 공개 페이지(학습 목록, 한자 페이지, sitemap)에 쓰는 학습 콘텐츠 조회.
 * 매일 자동 생성되는 문제도 DB 에서 바로 읽으므로 따로 등록하지 않아도 목록·sitemap 에 들어간다.
 */
@Service
class KanjiLibraryService(
    private val dailyContentRepository: DailyContentRepository,
) {

    fun lessons(level: KanjiLevel): List<DailyContentDto> = when (level) {
        KanjiLevel.BEGINNER -> dailyContentRepository.findAllBeginner()
        KanjiLevel.INTERMEDIATE -> dailyContentRepository.findAllIntermediate()
        KanjiLevel.ADVANCED -> dailyContentRepository.findAllAdvanced()
    }.sortedBy { it.id }

    fun allLessons(): Map<KanjiLevel, List<DailyContentDto>> =
        KanjiLevel.values().associateWith { lessons(it) }

    fun neighbors(level: KanjiLevel, id: Long): LessonNeighbors {
        val ids = lessons(level).mapNotNull { it.id }
        return LessonNeighbors(
            previousId = ids.filter { it < id }.maxOrNull(),
            nextId = ids.filter { it > id }.minOrNull(),
        )
    }

    /** 학습에 kanji1·kanji2 로 나온 한자 전체. sitemap 용 */
    fun allKanji(): List<String> =
        allLessons().values.flatten().flatMap { listOf(it.kanji1, it.kanji2) }.distinct().sorted()

    /** 한자 페이지 내용. 학습에 kanji1·kanji2 로 나온 적이 없으면 null */
    fun kanji(kanji: String): KanjiEntryDto? {
        val all = allLessons().values.flatten()
        val asMain = all.filter { it.kanji1 == kanji || it.kanji2 == kanji }
        if (asMain.isEmpty()) return null

        val readings = asMain.map { if (it.kanji1 == kanji) it.yomi1 else it.yomi2 }.distinct()
        val meanings = asMain.map { if (it.kanji1 == kanji) it.meaning1 else it.meaning2 }.distinct()
        val paired = asMain.map { if (it.kanji1 == kanji) it.kanji2 else it.kanji1 }.distinct()

        val words = all.flatMap { content ->
            listOf(
                content.word1 to content.wordMeaning1,
                content.word2 to content.wordMeaning2,
                content.word3 to content.wordMeaning3,
                content.word4 to content.wordMeaning4,
            ).map { (word, meaning) -> KanjiWord(word, meaning, content.level) }
        }
            .filter { it.word.substringBefore('(').contains(kanji) }
            .distinctBy { it.word }

        val sentences = all
            .filter { SentenceTiles.plainText(it.sentence).contains(kanji) && it.id != null }
            .map { KanjiSentence(it.sentence, it.sentenceMeaning, it.level, it.id!!) }

        return KanjiEntryDto(
            kanji = kanji,
            readings = readings,
            meanings = meanings,
            pairedKanji = paired,
            lessons = asMain.filter { it.id != null }.map {
                KanjiLessonRef(it.level, it.id!!, it.kanji1, it.kanji2, it.meaning1, it.meaning2)
            },
            words = words,
            sentences = sentences,
        )
    }
}
