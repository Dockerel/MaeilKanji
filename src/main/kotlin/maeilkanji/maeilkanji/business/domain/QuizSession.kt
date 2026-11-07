package maeilkanji.maeilkanji.business.domain

enum class QuizQuestionType(val label: String) {
    WORD_TO_JAPANESE("일본어 단어 고르기"),
    WORD_TO_KOREAN("뜻 고르기"),
    FILL_BLANK("빈칸 채우기"),
    ARRANGE("문장 만들기"),
}

/** 보기 또는 문장 배열 블록. html 은 루비가 붙은 표시용 마크업. */
data class QuizOption(val text: String, val html: String)

data class QuizQuestion(
    val type: QuizQuestionType,
    // 질문 (한국어)
    val prompt: String,
    // 질문에 딸린 일본어 (루비 HTML). 단어 / 빈칸이 있는 예문
    val promptHtml: String? = null,
    // 출처 라벨. 예: 어제 배운 예문
    val source: String? = null,
    // 보조 설명. 예: 예문의 한국어 뜻
    val hint: String? = null,
    val options: List<QuizOption>,
    // 4지선다 정답 위치
    val answerIndex: Int? = null,
    // 문장 배열 정답 블록 순서
    val answerTexts: List<String>? = null,
    val explanation: String,
    // 해설에 함께 보여줄 일본어 (루비 HTML)
    val explanationHtml: String? = null,
) {
    val typeLabel: String get() = type.label

    /**
     * 4지선다: [보기 번호] 하나.
     * 문장 배열: 고른 블록 번호를 순서대로. 같은 글자의 블록은 어느 쪽을 골라도 정답으로 본다.
     */
    fun isCorrect(answer: List<Int>): Boolean = when (type) {
        QuizQuestionType.ARRANGE ->
            answer.distinct().size == answer.size &&
                answer.all { it in options.indices } &&
                answer.map { options[it].text } == answerTexts

        else -> answer.size == 1 && answer[0] == answerIndex
    }
}

data class QuizSession(
    val level: KanjiLevel,
    val contentId: Long,
    val questions: List<QuizQuestion>,
) {
    /** 문제별 정답 여부. 답 개수가 문제 수와 다르면 null. */
    fun grade(answers: List<List<Int>>): List<Boolean>? {
        if (answers.size != questions.size) return null
        return questions.zip(answers) { question, answer -> question.isCorrect(answer) }
    }
}
