package maeilkanji.maeilkanji.presentation.controller.api

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import maeilkanji.maeilkanji.business.service.ProfileService
import maeilkanji.maeilkanji.business.service.response.NicknameAvailabilityResponse
import maeilkanji.maeilkanji.business.service.response.UpdateProfileResponse
import maeilkanji.maeilkanji.presentation.auth.MemberSessionCookie
import maeilkanji.maeilkanji.presentation.controller.api.request.UpdateProfileRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/** 내 프로필 꾸미기. 회원은 메일 링크로 들어올 때 발급된 쿠키(MemberSessionCookie)로 식별한다. */
@RestController
@RequestMapping("/api/v1/profile")
class ProfileRestController(
    private val profileService: ProfileService,
) {

    @PutMapping("/me")
    fun updateMyProfile(
        @Valid @RequestBody request: UpdateProfileRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<UpdateProfileResponse> {
        val memberId = MemberSessionCookie.read(httpRequest)
            ?: throw MaeilKanjiException("메일의 학습 링크로 한 번 접속한 뒤 다시 시도해 주세요.", HttpStatus.UNAUTHORIZED)
        return ResponseEntity.ok(profileService.updateProfile(memberId, request.toServiceRequest()))
    }

    @GetMapping("/nickname-check")
    fun checkNickname(
        @RequestParam nickname: String,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<NicknameAvailabilityResponse> {
        val memberId = MemberSessionCookie.read(httpRequest)
        return ResponseEntity.ok(profileService.checkNickname(memberId, nickname))
    }
}
