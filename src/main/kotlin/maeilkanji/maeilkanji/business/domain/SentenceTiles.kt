package maeilkanji.maeilkanji.business.domain

/** 예문의 한 조각. reading 이 있으면 루비(후리가나)가 붙는 한자 부분이다. */
data class SentenceSegment(val text: String, val reading: String?)

/** 예문 블록 하나. html 은 루비 태그가 포함된 표시용 마크업이다. */
data class SentenceTile(val text: String, val html: String) {
    val isPunctuation: Boolean get() = text.all { it in SentenceTiles.PUNCTUATION }
}

/**
 * 예문(sentence, 루비 태그 포함)과 블록 문자열(sentence_tiles, "大学|の|前|..")을 다룬다.
 * 블록을 이어 붙이면 루비를 뺀 예문과 정확히 같아야 하고, 루비 한자가 블록 경계에서 잘리면 안 된다.
 */
object SentenceTiles {
    const val SEPARATOR = "|"
    const val PUNCTUATION = "、。，．！？!?"

    private val RUBY = Regex("<ruby><rb>(.*?)</rb><rt>(.*?)</rt></ruby>")

    fun segments(sentence: String): List<SentenceSegment> {
        val segments = mutableListOf<SentenceSegment>()
        var last = 0
        RUBY.findAll(sentence).forEach { match ->
            if (match.range.first > last) segments += SentenceSegment(sentence.substring(last, match.range.first), null)
            segments += SentenceSegment(match.groupValues[1], match.groupValues[2])
            last = match.range.last + 1
        }
        if (last < sentence.length) segments += SentenceSegment(sentence.substring(last), null)
        return segments
    }

    fun plainText(sentence: String): String = segments(sentence).joinToString("") { it.text }

    /** 예문 전체를 루비가 붙은 HTML 로 만든다. (원문 마크업을 이스케이프된 안전한 형태로 다시 쓴다) */
    fun html(sentence: String): String = segments(sentence).joinToString("") { segmentHtml(it.text, it.reading) }

    fun isValid(sentence: String, tiles: String?): Boolean = parse(sentence, tiles) != null

    /** 블록 문자열을 루비를 살린 블록 목록으로 바꾼다. 예문과 맞지 않으면 null. */
    fun parse(sentence: String, tiles: String?): List<SentenceTile>? {
        if (tiles.isNullOrBlank()) return null
        val texts = tiles.split(SEPARATOR)
        if (texts.any { it.isEmpty() }) return null

        val segments = segments(sentence)
        if (texts.joinToString("") != segments.joinToString("") { it.text }) return null

        // 세그먼트별 시작 위치
        val starts = segments.runningFold(0) { acc, s -> acc + s.text.length }

        val result = mutableListOf<SentenceTile>()
        var tileStart = 0
        for (text in texts) {
            val tileEnd = tileStart + text.length
            val html = StringBuilder()
            segments.forEachIndexed { i, segment ->
                val segStart = starts[i]
                val segEnd = starts[i + 1]
                val from = maxOf(segStart, tileStart)
                val to = minOf(segEnd, tileEnd)
                if (from >= to) return@forEachIndexed
                if (segment.reading != null) {
                    // 루비 한자가 블록 경계에서 잘리면 읽기를 나눌 수 없다.
                    if (from != segStart || to != segEnd) return null
                    html.append(segmentHtml(segment.text, segment.reading))
                } else {
                    html.append(escape(segment.text.substring(from - segStart, to - segStart)))
                }
            }
            result += SentenceTile(text, html.toString())
            tileStart = tileEnd
        }
        return result
    }

    /**
     * 문장 배열용으로 블록 수를 max 이하로 줄인다. 문장부호는 뺀다.
     * 짧은 히라가나 블록(조사 등)을 앞 블록에 붙이고, 그래도 많으면 가장 짧은 인접 쌍을 합친다.
     */
    fun mergeForArrange(tiles: List<SentenceTile>, max: Int): List<SentenceTile> {
        val merged = tiles.filterNot { it.isPunctuation }.toMutableList()
        while (merged.size > max) {
            val kanaIndex = (1 until merged.size).firstOrNull { isShortKana(merged[it].text) }
            val index = kanaIndex
                ?: (1 until merged.size).minByOrNull { merged[it - 1].text.length + merged[it].text.length }!!
            val joined = SentenceTile(merged[index - 1].text + merged[index].text, merged[index - 1].html + merged[index].html)
            merged[index - 1] = joined
            merged.removeAt(index)
        }
        return merged
    }

    fun escape(text: String): String = buildString {
        text.forEach {
            when (it) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(it)
            }
        }
    }

    private fun segmentHtml(text: String, reading: String?): String =
        if (reading == null) escape(text) else "<ruby>${escape(text)}<rt>${escape(reading)}</rt></ruby>"

    private fun isShortKana(text: String): Boolean = text.length <= 3 && text.all { it in 'ぁ'..'ゟ' }
}
