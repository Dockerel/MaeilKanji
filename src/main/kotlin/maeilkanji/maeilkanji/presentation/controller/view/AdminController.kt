package maeilkanji.maeilkanji.presentation.controller.view

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

data class LevelInfo(
    val level: String,
    val korean: String,
)

@Controller
@RequestMapping("/admin")
class AdminController {

    @GetMapping("/login")
    fun adminLoginPage(): String {
        return "admin-login" // admin-login.html 템플릿 렌더링
    }

    @GetMapping("/dashboard")
    fun adminDashboard(model: Model): String {
        model.addAttribute("levels", listOf(
            LevelInfo(KanjiLevel.BEGINNER.name, KanjiLevel.BEGINNER.korean),
            LevelInfo(KanjiLevel.INTERMEDIATE.name, KanjiLevel.INTERMEDIATE.korean),
            LevelInfo(KanjiLevel.ADVANCED.name, KanjiLevel.ADVANCED.korean),
        ))
        return "admin-dashboard"
    }

}