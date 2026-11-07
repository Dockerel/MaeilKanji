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
) = MemberDto(
    id = id,
    email = email,
    memberStatus = memberStatus,
    level = level,
    streak = streak,
    lastAccessedAt = lastAccessedAt,
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
)
