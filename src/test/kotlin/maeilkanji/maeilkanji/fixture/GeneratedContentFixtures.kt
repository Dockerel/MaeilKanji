package maeilkanji.maeilkanji.fixture

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.service.dto.DailyContentDto

/** 생성 규칙을 모두 지키는 문제 (create_question.md 예시의 중급 행) */
fun validGeneratedContent(level: KanjiLevel = KanjiLevel.INTERMEDIATE) = DailyContentDto(
    level = level,
    kanji1 = "増",
    yomi1 = "(ぞう, ふ)",
    meaning1 = "늘다, 증가하다",
    kanji2 = "減",
    yomi2 = "(げん, へ)",
    meaning2 = "줄다, 감소하다",
    word1 = "人口増加(じんこうぞうか)",
    wordMeaning1 = "인구 증가",
    word2 = "増える(ふえる)",
    wordMeaning2 = "늘다",
    word3 = "人口減少(じんこうげんしょう)",
    wordMeaning3 = "인구 감소",
    word4 = "減る(へる)",
    wordMeaning4 = "줄다",
    sentence = "<ruby><rb>都市</rb><rt>とし</rt></ruby>では<ruby><rb>人口</rb><rt>じんこう</rt></ruby>が<ruby><rb>増</rb><rt>ふ</rt></ruby>え、<ruby><rb>地方</rb><rt>ちほう</rt></ruby>では<ruby><rb>減</rb><rt>へ</rt></ruby>っています。",
    sentenceMeaning = "도시에서는 인구가 늘고, 지방에서는 줄고 있습니다.",
    quiz = "'인구 증가'를 일본어로 어떻게 표현할까요?",
    answer = "じんこうぞうか (人口増加)",
    explanation = "'인구 증가'는 일본어로 人口増加(じんこうぞうか)라고 표현합니다. 오늘 배운 増(ぞう)를 사용한 단어였습니다!",
    wrongAnswer1 = "じんこうげんしょう (人口減少)",
    wrongAnswer2 = "ぞうぜい (増税)",
    wrongAnswer3 = "じんこうてき (人工的)",
    sentenceTiles = "都市|では|人口|が|増え|、|地方|では|減って|います|。",
)
