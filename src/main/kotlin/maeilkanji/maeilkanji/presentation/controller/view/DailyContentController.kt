package maeilkanji.maeilkanji.presentation.controller.view

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.domain.QuizChoices
import maeilkanji.maeilkanji.business.service.DailyContentService
import maeilkanji.maeilkanji.business.service.KanjiLibraryService
import maeilkanji.maeilkanji.business.service.QuizService
import maeilkanji.maeilkanji.business.service.dto.DailyContentDto
import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/daily-content")
class DailyContentController(
    private val dailyContentService: DailyContentService,
    private val quizService: QuizService,
    private val kanjiLibraryService: KanjiLibraryService,
) {

    @GetMapping("/beginner/{contentId}")
    fun showBeginnerDailyContent(
        @PathVariable contentId: Long,
        model: Model
    ): String {
        val dailyContent = dailyContentService.findBeginnerDailyContent(contentId)
        addLessonAttributes(model, dailyContent)
        return "email/daily-lesson-beginner"
    }

    @GetMapping("/intermediate/{contentId}")
    fun showIntermediateDailyContent(
        @PathVariable contentId: Long,
        model: Model
    ): String {
        val dailyContent = dailyContentService.findIntermediateDailyContent(contentId)
        addLessonAttributes(model, dailyContent)
        return "email/daily-lesson-intermediate"
    }

    @GetMapping("/advanced/{contentId}")
    fun showAdvancedDailyContent(
        @PathVariable contentId: Long,
        model: Model
    ): String {
        val dailyContent = dailyContentService.findAdvancedDailyContent(contentId)
        addLessonAttributes(model, dailyContent)
        return "email/daily-lesson-advanced"
    }

    /**
     * 오늘의 5문제 페이지. 메일/학습 페이지의 1번 문제 보기를 누르면 ?choice=N 으로 들어와 1번 답이 선택된 채 시작한다.
     * 채점 피드백은 페이지에서 바로 보여주고, 기록·통계·스트릭은 마지막에 JS 가 POST 로 처리한다.
     * (메일 스캐너의 링크 미리 열기는 집계하지 않기 위해)
     */
    @GetMapping("/{levelPath}/{contentId}/quiz")
    fun showQuiz(
        @PathVariable levelPath: String,
        @PathVariable contentId: Long,
        @RequestParam(required = false) choice: Int?,
        model: Model
    ): String {
        val level = KanjiLevel.fromPath(levelPath)
            ?: throw MaeilKanjiException("Unknown level: $levelPath", HttpStatus.NOT_FOUND)
        val quizSession = quizService.createSession(level, contentId)
            ?: return "redirect:/daily-content/${level.path}/$contentId"
        val firstQuestion = quizSession.questions.first()

        model.addAttribute("quizSession", quizSession)
        model.addAttribute("initialChoice", choice?.takeIf { it in firstQuestion.options.indices })
        model.addAttribute("levelLabel", level.korean)
        model.addAttribute("levelPath", level.path)
        return "quiz-session"
    }

    private fun addLessonAttributes(model: Model, content: DailyContentDto) {
        model.addAttribute("content", content)
        model.addAttribute("levelPath", content.level.path)
        model.addAttribute("quiz", QuizChoices.of(content))
        model.addAttribute("neighbors", kanjiLibraryService.neighbors(content.level, content.id ?: 0L))
        addSeoAttributes(model, content)
    }

    /** 검색 결과에 보일 제목·설명과 구조화 데이터(LearningResource) */
    private fun addSeoAttributes(model: Model, content: DailyContentDto) {
        val level = content.level
        val words = listOf(content.word1, content.word2, content.word3, content.word4)
            .joinToString(", ") { it.substringBefore('(') }
        val title = "${content.kanji1}·${content.kanji2} 읽는 법과 단어 | ${level.korean} 일본어 한자"
        val description = "${content.kanji1}(${content.meaning1}) vs ${content.kanji2}(${content.meaning2}). " +
            "${level.korean} 일본어 한자 학습: $words 읽기와 뜻, 후리가나 예문, 4지선다 퀴즈."
        val path = "/daily-content/${level.path}/${content.id}"

        model.addAttribute("seoTitle", title)
        model.addAttribute("seoDescription", description)
        model.addAttribute("canonicalPath", path)
        model.addAttribute(
            "jsonLd", JsonLd.of(
                mapOf(
                    "@context" to "https://schema.org",
                    "@type" to "LearningResource",
                    "name" to title,
                    "description" to description,
                    "inLanguage" to listOf("ko", "ja"),
                    "learningResourceType" to "Lesson",
                    "educationalLevel" to level.korean,
                    "teaches" to listOf(content.kanji1, content.kanji2),
                    "isAccessibleForFree" to true,
                    "provider" to mapOf("@type" to "Organization", "name" to "매일 칸지"),
                )
            )
        )
    }

}
