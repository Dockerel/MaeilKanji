package maeilkanji.maeilkanji.business.service.request

import maeilkanji.maeilkanji.business.domain.KanjiLevel

data class SubmitQuizAnswerServiceRequest(
    val level: KanjiLevel,
    val contentId: Long,
    val choiceIndex: Int,
    // 메일 링크로 들어온 회원만 집계한다. 공유 링크 방문자는 null.
    val memberId: String?,
)
