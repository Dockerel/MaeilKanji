package maeilkanji.maeilkanji.presentation.auth

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseCookie
import java.time.Duration
import java.util.*

/**
 * 메일 링크(?memberId=...)로 들어온 회원을 기억하는 쿠키. 내 프로필(/me)·프로필 수정·랭킹의 "내 순위"에 쓴다.
 * memberId 는 메일 링크에 이미 담겨 다니는 값이라 별도 토큰 없이 그대로 저장한다.
 * SameSite=Lax 라 다른 사이트에서 보낸 POST/PUT 에는 실리지 않는다.
 */
object MemberSessionCookie {

    const val NAME = "MK_MEMBER"
    private val MAX_AGE: Duration = Duration.ofDays(365)

    /** 쿠키의 memberId. 없거나 UUID 형식이 아니면 null */
    fun read(request: HttpServletRequest): UUID? =
        request.cookies?.firstOrNull { it.name == NAME }?.value
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    fun issue(memberId: UUID, request: HttpServletRequest): ResponseCookie = build(memberId.toString(), MAX_AGE, request)

    fun expire(request: HttpServletRequest): ResponseCookie = build("", Duration.ZERO, request)

    private fun build(value: String, maxAge: Duration, request: HttpServletRequest): ResponseCookie =
        ResponseCookie.from(NAME, value)
            .httpOnly(true)
            .secure(isHttps(request))
            .sameSite("Lax")
            .path("/")
            .maxAge(maxAge)
            .build()

    // Ingress 에서 TLS 가 종료되므로 X-Forwarded-Proto 도 확인
    private fun isHttps(request: HttpServletRequest): Boolean =
        request.isSecure || request.getHeader("X-Forwarded-Proto").equals("https", ignoreCase = true)
}
