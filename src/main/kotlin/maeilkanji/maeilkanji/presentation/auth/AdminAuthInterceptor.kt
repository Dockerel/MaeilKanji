package maeilkanji.maeilkanji.presentation.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import maeilkanji.maeilkanji.business.service.AdminService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.servlet.HandlerInterceptor

/**
 * 관리자 세션이 없으면 API 요청은 401, 화면 요청은 로그인 화면으로 보낸다.
 */
class AdminAuthInterceptor(
    private val adminService: AdminService,
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (adminService.isValidAdminSession(AdminSessionCookie.read(request))) {
            return true
        }

        if (request.requestURI.startsWith("/api/")) {
            response.status = HttpStatus.UNAUTHORIZED.value()
            response.contentType = MediaType.TEXT_PLAIN_VALUE
            response.characterEncoding = Charsets.UTF_8.name()
            response.writer.write("Unauthorized")
        } else {
            response.sendRedirect("/admin/login")
        }
        return false
    }
}
