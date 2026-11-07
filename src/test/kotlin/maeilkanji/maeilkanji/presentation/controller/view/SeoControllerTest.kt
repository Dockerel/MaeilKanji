package maeilkanji.maeilkanji.presentation.controller.view

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.mockk
import maeilkanji.maeilkanji.business.service.AdminService
import org.hamcrest.Matchers.containsString
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(SeoController::class, properties = ["service.base-url=https://example.com/"])
@Import(SeoControllerTest.MockConfig::class)
class SeoControllerTest(
    mockMvc: MockMvc,
) : BehaviorSpec({
    extensions(SpringExtension)

    Given("robots.txt 요청에서") {
        val result = mockMvc.get("/robots.txt")

        Then("관리자/API 경로를 막고 sitemap 절대 URL 을 알려준다") {
            result.andExpect {
                status { isOk() }
                content { contentTypeCompatibleWith("text/plain") }
                content { string(containsString("Disallow: /admin")) }
                content { string(containsString("Disallow: /api/")) }
                content { string(containsString("Disallow: /me")) }
                content { string(containsString("Sitemap: https://example.com/sitemap.xml")) }
            }
        }
    }

    Given("sitemap.xml 요청에서") {
        val result = mockMvc.get("/sitemap.xml")

        Then("랜딩 페이지 절대 URL 을 포함한 XML 을 반환한다") {
            result.andExpect {
                status { isOk() }
                content { contentTypeCompatibleWith("application/xml") }
                content { string(containsString("<loc>https://example.com/</loc>")) }
            }
        }
    }
}) {
    @TestConfiguration
    class MockConfig {
        // WebMvcConfig 의 AdminAuthInterceptor 가 요구
        @Bean
        fun adminService(): AdminService = mockk()
    }
}
