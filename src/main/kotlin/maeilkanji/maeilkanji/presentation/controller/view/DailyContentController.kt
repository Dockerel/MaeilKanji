package maeilkanji.maeilkanji.presentation.controller.view

import maeilkanji.maeilkanji.business.service.DailyContentService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/daily-content")
class DailyContentController(
    private val dailyContentService: DailyContentService,
) {

    @GetMapping("/beginner/{contentId}")
    fun showBeginnerDailyContent(
        @PathVariable contentId: Long,
        model: Model
    ): String {
        val dailyContent = dailyContentService.findBeginnerDailyContent(contentId)
        model.addAttribute("content", dailyContent)
        return "email/daily-lesson-beginner"
    }

    @GetMapping("/intermediate/{contentId}")
    fun showIntermediateDailyContent(
        @PathVariable contentId: Long,
        model: Model
    ): String {
        val dailyContent = dailyContentService.findIntermediateDailyContent(contentId)
        model.addAttribute("content", dailyContent)
        return "email/daily-lesson-intermediate"
    }

    @GetMapping("/advanced/{contentId}")
    fun showAdvancedDailyContent(
        @PathVariable contentId: Long,
        model: Model
    ): String {
        val dailyContent = dailyContentService.findAdvancedDailyContent(contentId)
        model.addAttribute("content", dailyContent)
        return "email/daily-lesson-advanced"
    }

}