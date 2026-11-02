package maeilkanji.maeilkanji.presentation.controller.view

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 검색엔진 크롤러용 robots.txt / sitemap.xml.
 * 절대 URL 이 필요하므로 service.base-url 을 기준으로 동적으로 생성한다.
 */
@RestController
class SeoController(
    @Value("\${service.base-url}") baseUrl: String,
) {

    private val baseUrl = baseUrl.trimEnd('/')

    @GetMapping("/robots.txt", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun robots(): String = """
        User-agent: *
        Allow: /
        Disallow: /admin
        Disallow: /api/
        Disallow: /unsubscribe
        Disallow: /unsubscribe-confirm
        Disallow: /change-level-confirm
        Disallow: /successfully-changed-level
        Disallow: /bye
        Disallow: /feedback
        Disallow: /thank-you-for-feedback

        Sitemap: $baseUrl/sitemap.xml
    """.trimIndent() + "\n"

    @GetMapping("/sitemap.xml", produces = [MediaType.APPLICATION_XML_VALUE])
    fun sitemap(): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
          <url>
            <loc>$baseUrl/</loc>
            <changefreq>weekly</changefreq>
            <priority>1.0</priority>
          </url>
        </urlset>
    """.trimIndent() + "\n"
}
