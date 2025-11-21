package maeilkanji.maeilkanji.presentation.controller.api

import maeilkanji.maeilkanji.business.service.DailyContentService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/tests")
class TestController(
    private val dailyContentService: DailyContentService
) {

    @GetMapping("/daily-content")
    fun sendDailyContentMailTest() {
        dailyContentService.sendBeginnerDailyContentEmails()
        dailyContentService.sendIntermediateDailyContentEmails()
        dailyContentService.sendAdvancedDailyContentEmails()
    }

}