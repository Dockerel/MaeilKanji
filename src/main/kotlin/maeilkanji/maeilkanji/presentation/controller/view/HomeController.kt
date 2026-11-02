package maeilkanji.maeilkanji.presentation.controller.view

import jakarta.validation.Valid
import maeilkanji.maeilkanji.business.service.MemberService
import maeilkanji.maeilkanji.presentation.controller.api.request.ChangeLevelRequest
import maeilkanji.maeilkanji.presentation.controller.api.request.FeedbackRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class HomeController(
    private val memberService: MemberService,

    @Value("\${service.base-url}") private val baseUrl: String,
    @Value("\${seo.google-site-verification:}") private val googleSiteVerification: String,
    @Value("\${seo.naver-site-verification:}") private val naverSiteVerification: String,
) {

    @GetMapping("/")
    fun showLandingPage(model: Model): String {
        model.addAttribute("siteUrl", baseUrl.trimEnd('/'))
        model.addAttribute("googleSiteVerification", googleSiteVerification)
        model.addAttribute("naverSiteVerification", naverSiteVerification)
        return "landing"
    }

    @GetMapping("/unsubscribe-confirm")
    fun unsubscribePage(
        @RequestParam(value = "memberId", defaultValue = "") memberId: String,
        model: Model,
    ): String {
        model.addAttribute("baseUrl", baseUrl)
        model.addAttribute("memberId", memberId)
        return "unsubscribe"
    }

    @GetMapping("/change-level-confirm")
    fun changeLevelPage(
        @RequestParam(value = "memberId", defaultValue = "") memberId: String,
        model: Model,
    ): String {
        model.addAttribute("memberId", memberId)
        return "change-level"
    }

    @GetMapping("/bye")
    fun byePage(
        @RequestParam(value = "memberId", defaultValue = "") memberId: String,
        model: Model,
    ): String {
        model.addAttribute("memberId", memberId)
        return "bye"
    }

    @GetMapping("/feedback")
    fun feedbackPage(
        @RequestParam(value = "memberId", defaultValue = "") memberId: String,
        @RequestParam(value = "context", defaultValue = "") context: String,
        model: Model,
    ): String {
        model.addAttribute("memberId", memberId)
        model.addAttribute("context", context)
        return "feedback"
    }

    @GetMapping("/thank-you-for-feedback")
    fun thankYouForFeedback(): String {
        return "thank-you-for-feedback"
    }

    @GetMapping("/unsubscribe")
    fun unsubscribe(@RequestParam memberId: String): String {
        memberService.stopMail(memberId)
        return "redirect:/bye?memberId=$memberId"
    }

    @PostMapping("/feedback")
    fun feedback(request: FeedbackRequest): String {
        memberService.feedback(request.toServiceRequest())
        return "redirect:/thank-you-for-feedback"
    }

    @GetMapping("/successfully-changed-level")
    fun successfullyChangedLevel(): String {
        return "successfully-changed-level"
    }

    @PostMapping("/change-level")
    fun changeLevel(@Valid request: ChangeLevelRequest): String {
        memberService.changeLevel(request.toServiceRequest())
        return "redirect:/successfully-changed-level"
    }

}