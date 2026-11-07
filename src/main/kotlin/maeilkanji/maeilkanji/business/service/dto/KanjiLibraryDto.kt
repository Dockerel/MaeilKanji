package maeilkanji.maeilkanji.business.service.dto

import maeilkanji.maeilkanji.business.domain.KanjiLevel

/** 같은 레벨에서 앞뒤 학습 id. 없으면 null */
data class LessonNeighbors(
    val previousId: Long?,
    val nextId: Long?,
)

/** 한자 페이지(/kanji/{한자}) 내용. 이 한자가 나온 모든 학습을 모은다. */
data class KanjiEntryDto(
    val kanji: String,
    /** "(じょう, うえ)" 형식 그대로. 학습마다 다르게 적혔을 수 있어 여러 개다. */
    val readings: List<String>,
    val meanings: List<String>,
    /** 이 한자와 짝을 이뤄 나온 한자 (주로 반의 관계) */
    val pairedKanji: List<String>,
    val lessons: List<KanjiLessonRef>,
    val words: List<KanjiWord>,
    val sentences: List<KanjiSentence>,
)

data class KanjiLessonRef(
    val level: KanjiLevel,
    val id: Long,
    val kanji1: String,
    val kanji2: String,
    val meaning1: String,
    val meaning2: String,
)

data class KanjiWord(
    val word: String,
    val meaning: String,
    val level: KanjiLevel,
)

data class KanjiSentence(
    /** 루비 태그가 들어간 원문. 화면에서는 SentenceTiles.html 로 이스케이프해 쓴다. */
    val sentence: String,
    val meaning: String,
    val level: KanjiLevel,
    val lessonId: Long,
)
