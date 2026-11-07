package maeilkanji.maeilkanji.presentation.controller.view

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

/** 서비스 소개·개인정보처리방침. (애드센스 승인 요건) */
@Controller
class InfoController {

    @GetMapping("/about")
    fun showAbout(): String = "about"

    @GetMapping("/privacy")
    fun showPrivacy(): String = "privacy"
}
