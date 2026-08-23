package maeilkanji.maeilkanji.presentation.controller.api

import jakarta.validation.Valid
import maeilkanji.maeilkanji.business.service.AdminService
import maeilkanji.maeilkanji.business.service.SendAdminLoginCodeResponse
import maeilkanji.maeilkanji.presentation.controller.api.request.ContentRequest
import maeilkanji.maeilkanji.presentation.controller.api.request.SendAdminLoginCodeRequest
import maeilkanji.maeilkanji.presentation.controller.api.request.VerifyAdminLoginCodeRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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

}