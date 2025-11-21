package maeilkanji.maeilkanji.presentation.controller.api

import jakarta.validation.Valid
import maeilkanji.maeilkanji.business.service.MemberService
import maeilkanji.maeilkanji.business.service.response.ChangeLevelResponse
import maeilkanji.maeilkanji.business.service.response.SendVerificationCodeResponse
import maeilkanji.maeilkanji.business.service.response.SignupResponse
import maeilkanji.maeilkanji.business.service.response.VerifyEmailVerificationCodeResponse
import maeilkanji.maeilkanji.presentation.controller.api.request.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

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

}