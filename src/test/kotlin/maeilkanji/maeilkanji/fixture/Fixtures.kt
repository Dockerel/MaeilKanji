package maeilkanji.maeilkanji.fixture

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.domain.MemberStatus
import maeilkanji.maeilkanji.business.service.dto.DailyContentDto
import maeilkanji.maeilkanji.business.service.dto.MemberDto
import maeilkanji.maeilkanji.presentation.controller.api.request.ContentRequest
import java.time.LocalDateTime
import java.util.UUID

fun dailyContentDto(
    level: KanjiLevel = KanjiLevel.BEGINNER,
    id: Long? = 1L,
    quiz: String = "日 의 음독은?",
    wrongAnswer1: String? = null,
    wrongAnswer2: String? = null,
    wrongAnswer3: String? = null,
    sentenceTiles: String? = null,
) = DailyContentDto(
    level = level,
    id = id,
    kanji1 = "日",
    yomi1 = "にち",
    meaning1 = "날 일",
    kanji2 = "月",
    yomi2 = "げつ",
    meaning2 = "달 월",
    word1 = "日本",
    wordMeaning1 = "일본",
    word2 = "月曜日",
    wordMeaning2 = "월요일",
    word3 = "毎日",
    wordMeaning3 = "매일",
    word4 = "今月",
    wordMeaning4 = "이번 달",
    sentence = "毎日勉強します。",
    sentenceMeaning = "매일 공부합니다.",
    quiz = quiz,
    answer = "にち",
    explanation = "日 은 にち 로 읽는다.",
    wrongAnswer1 = wrongAnswer1,
    wrongAnswer2 = wrongAnswer2,
    wrongAnswer3 = wrongAnswer3,
    sentenceTiles = sentenceTiles,
)

/** 오답 보기 3개가 있는 객관식 콘텐츠 */
fun multipleChoiceContentDto(
    level: KanjiLevel = KanjiLevel.BEGINNER,
    id: Long = 1L,
) = dailyContentDto(level = level, id = id, wrongAnswer1 = "げつ", wrongAnswer2 = "ひ", wrongAnswer3 = "か")

fun memberDto(
    id: UUID = UUID.randomUUID(),
    email: String = "user@test.com",
    memberStatus: MemberStatus = MemberStatus.ACTIVE,
    level: KanjiLevel = KanjiLevel.BEGINNER,
    streak: Int = 0,
    lastAccessedAt: LocalDateTime? = null,
    maxStreak: Int = 0,
    nickname: String? = null,
) = MemberDto(
    id = id,
    email = email,
    memberStatus = memberStatus,
    level = level,
    streak = streak,
    lastAccessedAt = lastAccessedAt,
    maxStreak = maxStreak,
    nickname = nickname,
)

fun contentRequest(
    level: String = "BEGINNER",
    kanji1: String = "日",
) = ContentRequest(
    level = level,
    kanji1 = kanji1,
    yomi1 = "にち",
    meaning1 = "날 일",
    kanji2 = "月",
    yomi2 = "げつ",
    meaning2 = "달 월",
    word1 = "日本",
    wordMeaning1 = "일본",
    word2 = "月曜日",
    wordMeaning2 = "월요일",
    word3 = "毎日",
    wordMeaning3 = "매일",
    word4 = "今月",
    wordMeaning4 = "이번 달",
    sentence = "毎日勉強します。",
    sentenceMeaning = "매일 공부합니다.",
    quiz = "日 의 음독은?",
    answer = "にち",
    explanation = "日 은 にち 로 읽는다.",
    wrongAnswer1 = "げつ",
    wrongAnswer2 = "ひ",
    wrongAnswer3 = "か",
    sentenceTiles = "毎日|勉強します|。",
)

/**
 * 5문제 세션을 만들 수 있는 콘텐츠. id 마다 한자·단어 뜻·예문 블록이 달라 서로의 오답 보기가 된다.
 * 예문: <ruby>{한자}曜日</ruby>に<ruby>友達</ruby>と<ruby>会</ruby>いました。
 */
fun quizContentDto(id: Long, level: KanjiLevel = KanjiLevel.BEGINNER): DailyContentDto {
    val kanji = QUIZ_KANJI[((id - 1) % QUIZ_KANJI.size).toInt()]
    return DailyContentDto(
        level = level,
        id = id,
        kanji1 = kanji,
        yomi1 = "(よみ$id)",
        meaning1 = "뜻$id",
        kanji2 = "友",
        yomi2 = "(とも)",
        meaning2 = "벗",
        word1 = "${kanji}曜日(ようび$id)",
        wordMeaning1 = "요일$id",
        word2 = "${kanji}色(いろ$id)",
        wordMeaning2 = "색$id",
        word3 = "友達(ともだち)",
        wordMeaning3 = "친구$id",
        word4 = "友人(ゆうじん)",
        wordMeaning4 = "벗$id",
        sentence = "<ruby><rb>${kanji}曜日</rb><rt>ようび</rt></ruby>に<ruby><rb>友達</rb><rt>ともだち</rt></ruby>と<ruby><rb>会</rb><rt>あ</rt></ruby>いました。",
        sentenceMeaning = "요일$id 에 친구와 만났습니다.",
        quiz = "'요일$id'은 일본어로?",
        answer = "ようび$id (${kanji}曜日)",
        explanation = "해설$id",
        wrongAnswer1 = "오답A$id",
        wrongAnswer2 = "오답B$id",
        wrongAnswer3 = "오답C$id",
        sentenceTiles = "${kanji}曜日|に|友達|と|会いました|。",
    )
}

private val QUIZ_KANJI = listOf("月", "火", "水", "木", "金", "土", "日", "春", "夏", "秋")
