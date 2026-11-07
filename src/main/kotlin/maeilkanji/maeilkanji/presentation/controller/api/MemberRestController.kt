package maeilkanji.maeilkanji.presentation.controller.api

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import maeilkanji.maeilkanji.business.service.MemberService
import maeilkanji.maeilkanji.business.service.response.*
import maeilkanji.maeilkanji.presentation.controller.api.request.*
import maeilkanji.maeilkanji.presentation.auth.MemberSessionCookie
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/members")
class MemberRestController(
    private val memberService: MemberService,
) {

    @PostMapping("/send-verification-code")
    fun sendEmailVerificationCode(@Valid @RequestBody request: SendVerificationCodeRequest): ResponseEntity<SendVerificationCodeResponse> {
        val response = memberService.sendVerificationCode(request.toServiceRequest())
        return ResponseEntity.ok(response)
    }

    @PostMapping("/verify-verification-code")
    fun verifyEmailVerificationCode(@Valid @RequestBody request: VerifyVerificationCodeRequest): ResponseEntity<VerifyEmailVerificationCodeResponse> {
        val response = memberService.verifyEmailVerificationCode(request.toServiceRequest())
        return ResponseEntity.ok(response)
    }

    @PostMapping("/signup")
    fun signup(@Valid @RequestBody request: SignupRequest): ResponseEntity<SignupResponse> {
        val response = memberService.signup(request.toServiceRequest())
        return ResponseEntity.ok(response)
    }

    @PostMapping("/change-level")
    fun changeLevel(@Valid @RequestBody request: ChangeLevelRequest): ResponseEntity<ChangeLevelResponse> {
        val response = memberService.changeLevel(request.toServiceRequest())
        return ResponseEntity.ok(response)
    }

    @PostMapping("/unsubscribe")
    fun unsubscribe(@Valid @RequestBody request: StopMailRequest): ResponseEntity<StopMailResponse> {
        val response = memberService.stopMail(request.memberId)
        return ResponseEntity.ok(response)
    }

    /** 학습 접속 기록. 이 브라우저를 회원으로 기억하는 쿠키도 함께 발급한다. (내 프로필 / 랭킹의 내 순위용) */
    @PostMapping("/streak")
    fun recordStreak(
        @Valid @RequestBody request: StreakRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<StreakResponse> {
        val response = memberService.recordStudyAccess(request.memberId)
        val cookie = MemberSessionCookie.issue(UUID.fromString(request.memberId), httpRequest)
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(response)
    }

    @PostMapping("/feedback")
    fun feedback(@RequestBody request: FeedbackRequest): ResponseEntity<FeedbackResponse> {
        val response = memberService.feedback(request.toServiceRequest())
        return ResponseEntity.ok(response)
    }

}