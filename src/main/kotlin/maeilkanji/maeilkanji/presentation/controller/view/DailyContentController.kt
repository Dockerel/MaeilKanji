package maeilkanji.maeilkanji.presentation.controller.view

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.domain.QuizChoices
import maeilkanji.maeilkanji.business.service.DailyContentService
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
     * 퀴즈 결과 페이지. 채점 결과는 서버에서 바로 보여준다.
     * 응답 기록과 정답률 조회는 페이지의 JS 가 POST 로 한다. (메일 스캐너의 링크 미리 열기는 집계하지 않기 위해)
     */
    @GetMapping("/{levelPath}/{contentId}/quiz")
    fun showQuizResult(
        @PathVariable levelPath: String,
        @PathVariable contentId: Long,
        @RequestParam(required = false) choice: Int?,
        model: Model
    ): String {
        val level = KanjiLevel.fromPath(levelPath)
            ?: throw MaeilKanjiException("Unknown level: $levelPath", HttpStatus.NOT_FOUND)
        val content = quizService.findContent(level, contentId)
        val quizChoices = QuizChoices.of(content)
        if (quizChoices == null || choice == null || !quizChoices.isValidChoice(choice)) {
            return "redirect:/daily-content/${level.path}/$contentId"
        }

        model.addAttribute("content", content)
        model.addAttribute("levelLabel", level.korean)
        model.addAttribute("levelPath", level.path)
        model.addAttribute("quiz", quizChoices)
        model.addAttribute("choice", choice)
        model.addAttribute("correct", quizChoices.isCorrect(choice))
        return "quiz-result"
    }

    private fun addLessonAttributes(model: Model, content: DailyContentDto) {
        model.addAttribute("content", content)
        model.addAttribute("levelPath", content.level.path)
        model.addAttribute("quiz", QuizChoices.of(content))
    }

}
