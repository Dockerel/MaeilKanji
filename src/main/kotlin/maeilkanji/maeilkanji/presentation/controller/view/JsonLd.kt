package maeilkanji.maeilkanji.presentation.controller.view

import com.fasterxml.jackson.databind.ObjectMapper

/**
 * <script type="application/ld+json"> 에 그대로 넣을 구조화 데이터 문자열을 만든다.
 * 값에 "</script>" 가 섞여도 태그가 닫히지 않도록 "</" 를 "<\/" 로 바꾼다. (JSON 으로는 같은 값)
 */
object JsonLd {
    private val mapper = ObjectMapper()

    fun of(data: Map<String, Any?>): String =
        mapper.writeValueAsString(data).replace("</", "<\\/")
}
