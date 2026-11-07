package maeilkanji.maeilkanji.business.service.request

import maeilkanji.maeilkanji.business.domain.KanjiLevel

data class SubmitQuizSessionServiceRequest(
    val level: KanjiLevel,
    val contentId: Long,
    // 문제 순서대로의 답. 4지선다는 [보기 번호], 문장 만들기는 고른 블록 번호 순서
    val answers: List<List<Int>>,
    // 메일 링크로 들어온 회원만 집계한다. 공유 링크 방문자는 null.
    val memberId: String?,
)
