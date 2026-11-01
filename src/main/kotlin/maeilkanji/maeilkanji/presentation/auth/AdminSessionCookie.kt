package maeilkanji.maeilkanji.presentation.auth

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseCookie
import java.time.Duration

/**
 * 관리자 세션 토큰을 담는 쿠키. 토큰 자체의 유효성은 Redis 에서 AdminService 가 판단한다.
 */
object AdminSessionCookie {

    const val NAME = "ADMIN_SESSION"
    private val MAX_AGE: Duration = Duration.ofHours(8)

    fun read(request: HttpServletRequest): String? =
        request.cookies?.firstOrNull { it.name == NAME }?.value

    fun issue(token: String, request: HttpServletRequest): ResponseCookie = build(token, MAX_AGE, request)

    fun expire(request: HttpServletRequest): ResponseCookie = build("", Duration.ZERO, request)

    private fun build(value: String, maxAge: Duration, request: HttpServletRequest): ResponseCookie =
        ResponseCookie.from(NAME, value)
            .httpOnly(true)
            .secure(isHttps(request))
            .sameSite("Strict")
            .path("/")
            .maxAge(maxAge)
            .build()

    // Ingress 에서 TLS 가 종료되므로 X-Forwarded-Proto 도 확인
    private fun isHttps(request: HttpServletRequest): Boolean =
        request.isSecure || request.getHeader("X-Forwarded-Proto").equals("https", ignoreCase = true)
}
