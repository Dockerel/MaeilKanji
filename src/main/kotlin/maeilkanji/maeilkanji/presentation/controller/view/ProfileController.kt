package maeilkanji.maeilkanji.presentation.controller.view

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import maeilkanji.maeilkanji.business.domain.*
import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import maeilkanji.maeilkanji.business.service.ProfileService
import maeilkanji.maeilkanji.business.service.RankingService
import maeilkanji.maeilkanji.business.service.dto.ProfileDto
import maeilkanji.maeilkanji.presentation.auth.MemberSessionCookie
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import java.util.*

/**
 * 프로필 / 프로필 꾸미기 / 스트릭 랭킹 페이지.
 * - /u/{닉네임}: 누구나 볼 수 있는 공개 프로필
 * - /me: 내 프로필. 메일 링크로 들어왔던 브라우저(쿠키)에서만 열린다. 메일의 ?memberId= 링크로 들어오면 쿠키를 발급한다.
 */
@Controller
class ProfileController(
    private val profileService: ProfileService,
    private val rankingService: RankingService,
) {

    @GetMapping("/me")
    fun myProfile(
        @RequestParam(required = false) memberId: String?,
        request: HttpServletRequest,
        response: HttpServletResponse,
        model: Model,
    ): String {
        // 메일 링크: 쿠키를 발급하고 memberId 가 주소창에 남지 않도록 다시 보낸다.
        val linkedId = memberId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        if (linkedId != null) {
            profileService.getProfile(linkedId) // 없는 회원이면 404
            response.addHeader(HttpHeaders.SET_COOKIE, MemberSessionCookie.issue(linkedId, request).toString())
            return "redirect:/me"
        }

        val myId = MemberSessionCookie.read(request) ?: return "profile-guest"
        val profile = runCatching { profileService.getProfile(myId) }.getOrNull()
        if (profile == null) {
            response.addHeader(HttpHeaders.SET_COOKIE, MemberSessionCookie.expire(request).toString())
            return "profile-guest"
        }
        addProfileAttributes(model, profile, owner = true)
        return "profile"
    }

    @GetMapping("/me/edit")
    fun editMyProfile(request: HttpServletRequest, model: Model): String {
        val myId = MemberSessionCookie.read(request) ?: return "profile-guest"
        val profile = profileService.getProfile(myId)
        model.addAttribute("profile", profile)
        model.addAttribute("avatars", ProfileAvatar.values())
        model.addAttribute("themes", ProfileTheme.values())
        model.addAttribute("earnedBadges", profile.badges.filter { it.earned }.map { it.badge })
        model.addAttribute("nicknameMaxLength", NicknamePolicy.MAX_LENGTH)
        model.addAttribute("anonymousName", ProfileDto.ANONYMOUS_NAME)
        return "profile-edit"
    }

    @GetMapping("/u/{nickname}")
    fun publicProfile(@PathVariable nickname: String, request: HttpServletRequest, model: Model): String {
        val profile = profileService.getPublicProfile(nickname)
        val myId = MemberSessionCookie.read(request)
        val owner = myId != null && profileService.nicknameOf(myId) == profile.nickname
        addProfileAttributes(model, profile, owner)
        return "profile"
    }

    @GetMapping("/ranking")
    fun ranking(@RequestParam(required = false) type: String?, request: HttpServletRequest, model: Model): String {
        val rankingType = RankingType.fromPath(type)
        val ranking = rankingService.getRanking(rankingType, MemberSessionCookie.read(request))
        model.addAttribute("ranking", ranking)
        model.addAttribute("rankingTypes", RankingType.values())
        return "ranking"
    }

    private fun addProfileAttributes(model: Model, profile: ProfileDto, owner: Boolean) {
        model.addAttribute("profile", profile)
        model.addAttribute("owner", owner)
        model.addAttribute("rankingTypes", RankingType.values())
        model.addAttribute("badgeCategories", BadgeCategory.values())
        model.addAttribute("weekdayLabels", listOf("", "월", "", "수", "", "금", ""))
    }
}
