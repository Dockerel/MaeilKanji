package maeilkanji.maeilkanji.presentation.controller.api

import jakarta.validation.Valid
import maeilkanji.maeilkanji.business.service.MemberService
import maeilkanji.maeilkanji.business.service.response.*
import maeilkanji.maeilkanji.presentation.controller.api.request.ChangeLevelRequest
import maeilkanji.maeilkanji.presentation.controller.api.request.SendVerificationCodeRequest
import maeilkanji.maeilkanji.presentation.controller.api.request.SignupRequest
import maeilkanji.maeilkanji.presentation.controller.api.request.StopMailRequest
import maeilkanji.maeilkanji.presentation.controller.api.request.VerifyVerificationCodeRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/members")
class MemberController(
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

    @PostMapping("/stop-mail")
    fun stopMail(@Valid @RequestBody request: StopMailRequest): ResponseEntity<StopMailResponse> {
        val response = memberService.stopMail(request.toServiceRequest())
        return ResponseEntity.ok(response)
    }

}