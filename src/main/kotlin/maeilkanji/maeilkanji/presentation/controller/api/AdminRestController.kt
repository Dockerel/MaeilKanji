package maeilkanji.maeilkanji.presentation.controller.api

import jakarta.validation.Valid
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.service.AdminService
import maeilkanji.maeilkanji.business.service.SendAdminLoginCodeResponse
import maeilkanji.maeilkanji.business.service.dto.DailyContentDto
import maeilkanji.maeilkanji.business.service.dto.FeedbackDto
import maeilkanji.maeilkanji.business.service.dto.MemberDto
import maeilkanji.maeilkanji.presentation.controller.api.request.ContentRequest
import maeilkanji.maeilkanji.presentation.controller.api.request.SendAdminLoginCodeRequest
import maeilkanji.maeilkanji.presentation.controller.api.request.VerifyAdminLoginCodeRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin")
class AdminRestController(
    private val adminService: AdminService
) {

    @PostMapping("/send-login-code")
    fun sendAdminLoginCode(@Valid @RequestBody request: SendAdminLoginCodeRequest): ResponseEntity<SendAdminLoginCodeResponse> {
        val response = adminService.sendAdminLoginCode(request.toServiceRequest())
        return ResponseEntity.ok(response)
    }

    @PostMapping("/verify-login")
    fun verifyAdminLoginCode(@Valid @RequestBody request: VerifyAdminLoginCodeRequest): ResponseEntity<Map<String, Boolean>> {
        val success = adminService.verifyAdminLoginCode(request.toServiceRequest())
        return ResponseEntity.ok(mapOf("success" to success))
    }

    @PostMapping("/content")
    fun saveDailyContent(@Valid @RequestBody request: ContentRequest): ResponseEntity<Map<String, Long>> {
        val id = adminService.saveDailyContent(request.toDailyContentDto().level, request.toDailyContentDto())
        return ResponseEntity.ok(mapOf("id" to id))
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

    @GetMapping("/feedbacks")
    fun getFeedbacks(): ResponseEntity<List<FeedbackDto>> {
        return ResponseEntity.ok(adminService.getFeedbacks())
    }

}