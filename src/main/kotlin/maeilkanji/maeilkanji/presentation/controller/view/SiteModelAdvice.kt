package maeilkanji.maeilkanji.presentation.controller.view

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute

/**
 * 공개 페이지 SEO·광고용 공통 모델 값.
 * - siteUrl: canonical / og:url 절대 주소
 * - adsenseClientId: "ca-pub-..." 형식. 비어 있으면 google-adsense-account 메타를 출력하지 않는다.
 * - contactEmail: 개인정보처리방침·소개 페이지 문의처. 비어 있으면 피드백 페이지로 안내한다.
 */
@ControllerAdvice(basePackageClasses = [SiteModelAdvice::class])
class SiteModelAdvice(
    @Value("\${service.base-url:}") baseUrl: String,
    @Value("\${adsense.client-id:}") private val adsenseClientId: String,
    @Value("\${site.contact-email:}") private val contactEmail: String,
) {

    private val siteUrl = baseUrl.trimEnd('/')

    @ModelAttribute("siteUrl")
    fun siteUrl(): String = siteUrl

    @ModelAttribute("adsenseClientId")
    fun adsenseClientId(): String = adsenseClientId.trim()

    @ModelAttribute("contactEmail")
    fun contactEmail(): String = contactEmail.trim()
}
