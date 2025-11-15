package maeilkanji.maeilkanji.presentation.controller.view

import maeilkanji.maeilkanji.business.service.MemberService
import maeilkanji.maeilkanji.presentation.controller.api.request.FeedbackRequest
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class HomeController(
    private val memberService: MemberService,
) {

    @GetMapping("/")
    fun showLandingPage(): String {
        return "landing"
    }

    @GetMapping("/unsubscribe-confirm")
    fun unsubscribePage(
        @RequestParam(value = "memberId", defaultValue = "") memberId: String,
        model: Model,
    ): String {
        model.addAttribute("memberId", memberId)
        return "unsubscribe"
    }

    @GetMapping("/bye")
    fun byePage(
        @RequestParam(value = "memberId", defaultValue = "") memberId: String,
        model: Model,
    ): String {
        model.addAttribute("memberId", memberId)
        return "bye"
    }

    @GetMapping("/thank-you-for-feedback")
    fun thankYouForFeedback(): String {
        return "thank-you-for-feedback"
    }

    @GetMapping("/unsubscribe")
    fun unsubscribe(@RequestParam memberId: String): String {
        val response = memberService.stopMail(memberId)
        return "redirect:/bye?memberId=$memberId"
    }

    @PostMapping("/feedback")
    fun feedback(request: FeedbackRequest): String {
        val response = memberService.feedback(request.toServiceRequest())
        return "redirect:/thank-you-for-feedback"
    }

}