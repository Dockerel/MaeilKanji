package maeilkanji.maeilkanji.presentation.controller.view

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HomeController {

    @GetMapping("/")
    fun showLandingPage(): String {
        return "landing" // landing.html 템플릿 반환
    }

}