package maeilkanji.maeilkanji.presentation.controller.api

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.service.AdminService
import maeilkanji.maeilkanji.business.service.DailyContentService
import maeilkanji.maeilkanji.business.service.SendAdminLoginCodeResponse
import maeilkanji.maeilkanji.business.service.dto.DailyContentDto
import maeilkanji.maeilkanji.business.service.dto.FeedbackDto
import maeilkanji.maeilkanji.business.service.dto.MemberDto
import maeilkanji.maeilkanji.presentation.auth.AdminSessionCookie
import maeilkanji.maeilkanji.presentation.controller.api.request.BulkContentRequest
import maeilkanji.maeilkanji.presentation.controller.api.request.ContentRequest
import maeilkanji.maeilkanji.presentation.controller.api.request.SendAdminLoginCodeRequest
import maeilkanji.maeilkanji.presentation.controller.api.request.VerifyAdminLoginCodeRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin")
class AdminRestController(
    private val adminService: AdminService,
    private val dailyContentService: DailyContentService,
) {

    @PostMapping("/send-login-code")
    fun sendAdminLoginCode(@Valid @RequestBody request: SendAdminLoginCodeRequest): ResponseEntity<SendAdminLoginCodeResponse> {
        val response = adminService.sendAdminLoginCode(request.toServiceRequest())
        return ResponseEntity.ok(response)
    }

    @PostMapping("/verify-login")
    fun verifyAdminLoginCode(
        @Valid @RequestBody request: VerifyAdminLoginCodeRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Map<String, Boolean>> {
        val token = adminService.verifyAdminLoginCode(request.toServiceRequest())
            ?: return ResponseEntity.ok(mapOf("success" to false))
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, AdminSessionCookie.issue(token, httpRequest).toString())
            .body(mapOf("success" to true))
    }

    @PostMapping("/logout")
    fun logout(httpRequest: HttpServletRequest): ResponseEntity<Void> {
        adminService.logoutAdmin(AdminSessionCookie.read(httpRequest))
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, AdminSessionCookie.expire(httpRequest).toString())
            .build()
    }

    @PostMapping("/content")
    fun saveDailyContent(@Valid @RequestBody request: ContentRequest): ResponseEntity<Map<String, Long>> {
        val id = adminService.saveDailyContent(request.toDailyContentDto().level, request.toDailyContentDto())
        return ResponseEntity.ok(mapOf("id" to id))
    }

    @PostMapping("/content/bulk")
    fun saveDailyContents(@Valid @RequestBody request: BulkContentRequest): ResponseEntity<Map<String, Any>> {
        val ids = adminService.saveDailyContents(request.toDailyContentDtos())
        return ResponseEntity.ok(mapOf("count" to ids.size, "ids" to ids))
    }

    @GetMapping("/content/{level}")
    fun getDailyContents(@PathVariable level: String): ResponseEntity<List<DailyContentDto>> {
        val contents = adminService.getDailyContents(KanjiLevel.valueOf(level))
        return ResponseEntity.ok(contents)
    }

    @DeleteMapping("/content/{level}/{id}")
    fun deleteDailyContent(@PathVariable level: String, @PathVariable id: Long): ResponseEntity<Void> {
        adminService.deleteDailyContent(KanjiLevel.valueOf(level), id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/members")
    fun getMembers(): ResponseEntity<List<MemberDto>> {
        return ResponseEntity.ok(adminService.getMembers())
    }

    @PostMapping("/members/{memberId}/reactivate")
    fun reactivateMember(@PathVariable memberId: UUID): ResponseEntity<Void> {
        adminService.reactivateMember(memberId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/daily-content/resend")
    fun resendTodayDailyContentEmails(): ResponseEntity<Map<String, Int>> {
        val count = dailyContentService.resendTodayDailyContentEmails()
        return ResponseEntity.ok(mapOf("count" to count))
    }

    @GetMapping("/feedbacks")
    fun getFeedbacks(): ResponseEntity<List<FeedbackDto>> {
        return ResponseEntity.ok(adminService.getFeedbacks())
    }

}