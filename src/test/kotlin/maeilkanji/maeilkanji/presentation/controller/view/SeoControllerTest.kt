package maeilkanji.maeilkanji.presentation.controller.view

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.service.KanjiLibraryService
import maeilkanji.maeilkanji.fixture.dailyContentDto
import maeilkanji.maeilkanji.business.service.AdminService
import org.hamcrest.Matchers.containsString
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(SeoController::class, properties = ["service.base-url=https://example.com/", "adsense.client-id=ca-pub-1234567890"])
@Import(SeoControllerTest.MockConfig::class)
class SeoControllerTest(
    mockMvc: MockMvc,
    kanjiLibraryService: KanjiLibraryService,
) : BehaviorSpec({
    extensions(SpringExtension)

    every { kanjiLibraryService.allLessons() } returns mapOf(
        KanjiLevel.BEGINNER to listOf(dailyContentDto(KanjiLevel.BEGINNER, id = 1L), dailyContentDto(KanjiLevel.BEGINNER, id = 2L)),
        KanjiLevel.INTERMEDIATE to emptyList(),
        KanjiLevel.ADVANCED to listOf(dailyContentDto(KanjiLevel.ADVANCED, id = 7L)),
    )
    every { kanjiLibraryService.allKanji() } returns listOf("増", "月")

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

        Then("목록·학습·한자(URL 인코딩)·소개·개인정보처리방침 페이지를 모두 넣는다") {
            result.andExpect {
                content { string(containsString("<loc>https://example.com/lessons</loc>")) }
                content { string(containsString("<loc>https://example.com/lessons/intermediate</loc>")) }
                content { string(containsString("<loc>https://example.com/daily-content/beginner/1</loc>")) }
                content { string(containsString("<loc>https://example.com/daily-content/beginner/2</loc>")) }
                content { string(containsString("<loc>https://example.com/daily-content/advanced/7</loc>")) }
                content { string(containsString("<loc>https://example.com/kanji/%E5%A2%97</loc>")) }
                content { string(containsString("<loc>https://example.com/about</loc>")) }
                content { string(containsString("<loc>https://example.com/privacy</loc>")) }
            }
        }
    }

    Given("애드센스 게시자 ID 가 없을 때") {
        val controller = SeoController("https://example.com", "", kanjiLibraryService)

        Then("ads.txt 는 404 다") {
            controller.adsTxt().statusCode.value() shouldBe 404
        }
    }

    Given("애드센스 게시자 ID 가 설정된 ads.txt 요청에서") {
        val result = mockMvc.get("/ads.txt")

        Then("ca- 를 뗀 게시자 ID 로 Google 판매자 줄을 반환한다") {
            result.andExpect {
                status { isOk() }
                content { string("google.com, pub-1234567890, DIRECT, f08c47fec0942fa0\n") }
            }
        }
    }
}) {
    @TestConfiguration
    class MockConfig {
        // WebMvcConfig 의 AdminAuthInterceptor 가 요구
        @Bean
        fun adminService(): AdminService = mockk()

        @Bean
        fun kanjiLibraryService(): KanjiLibraryService = mockk()
    }
}
