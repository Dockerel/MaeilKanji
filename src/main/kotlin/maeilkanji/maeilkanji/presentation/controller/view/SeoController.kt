package maeilkanji.maeilkanji.presentation.controller.view

import maeilkanji.maeilkanji.business.service.KanjiLibraryService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.UriUtils

/**
 * 검색엔진 크롤러용 robots.txt / sitemap.xml, 애드센스용 ads.txt.
 * 절대 URL 이 필요하므로 service.base-url 을 기준으로 동적으로 생성한다.
 * sitemap 은 요청마다 DB 를 읽으므로 매일 자동 생성되는 학습도 바로 포함된다.
 */
@RestController
class SeoController(
    @Value("\${service.base-url}") baseUrl: String,
    @Value("\${adsense.client-id:}") private val adsenseClientId: String,
    private val kanjiLibraryService: KanjiLibraryService,
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
        Disallow: /me

        Sitemap: $baseUrl/sitemap.xml
    """.trimIndent() + "\n"

    @GetMapping("/sitemap.xml", produces = [MediaType.APPLICATION_XML_VALUE])
    fun sitemap(): String {
        val urls = mutableListOf<SitemapUrl>()
        urls += SitemapUrl("/", "weekly", "1.0")
        urls += SitemapUrl("/lessons", "daily", "0.9")
        urls += SitemapUrl("/about", "monthly", "0.3")
        urls += SitemapUrl("/privacy", "yearly", "0.1")

        kanjiLibraryService.allLessons().forEach { (level, lessons) ->
            urls += SitemapUrl("/lessons/${level.path}", "daily", "0.8")
            lessons.mapNotNull { it.id }.forEach { id ->
                urls += SitemapUrl("/daily-content/${level.path}/$id", "monthly", "0.7")
            }
        }
        kanjiLibraryService.allKanji().forEach { kanji ->
            urls += SitemapUrl("/kanji/${UriUtils.encodePathSegment(kanji, Charsets.UTF_8)}", "weekly", "0.6")
        }

        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">""")
            urls.forEach {
                appendLine("  <url>")
                appendLine("    <loc>$baseUrl${it.path}</loc>")
                appendLine("    <changefreq>${it.changefreq}</changefreq>")
                appendLine("    <priority>${it.priority}</priority>")
                appendLine("  </url>")
            }
            appendLine("</urlset>")
        }
    }

    /**
     * 애드센스 승인 후 ADSENSE_CLIENT_ID(ca-pub-...)를 설정하면 ads.txt 를 제공한다. 없으면 404.
     * f08c47fec0942fa0 는 Google 의 인증 기관 ID 다.
     */
    @GetMapping("/ads.txt", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun adsTxt(): ResponseEntity<String> {
        val publisherId = adsenseClientId.trim().removePrefix("ca-")
        if (!publisherId.startsWith("pub-")) return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        return ResponseEntity.ok("google.com, $publisherId, DIRECT, f08c47fec0942fa0\n")
    }

    private data class SitemapUrl(val path: String, val changefreq: String, val priority: String)
}
