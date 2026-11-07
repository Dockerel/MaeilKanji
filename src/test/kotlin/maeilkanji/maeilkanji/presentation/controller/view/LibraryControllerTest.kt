package maeilkanji.maeilkanji.presentation.controller.view

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.service.AdminService
import maeilkanji.maeilkanji.business.service.KanjiLibraryService
import maeilkanji.maeilkanji.business.service.dto.KanjiEntryDto
import maeilkanji.maeilkanji.business.service.dto.KanjiLessonRef
import maeilkanji.maeilkanji.business.service.dto.KanjiSentence
import maeilkanji.maeilkanji.business.service.dto.KanjiWord
import maeilkanji.maeilkanji.fixture.dailyContentDto
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(LibraryController::class, InfoController::class, properties = ["service.base-url=https://example.com"])
@Import(LibraryControllerTest.MockConfig::class)
class LibraryControllerTest(
    mockMvc: MockMvc,
    kanjiLibraryService: KanjiLibraryService,
) : BehaviorSpec({
    extensions(SpringExtension)
    isolationMode = IsolationMode.InstancePerLeaf

    clearMocks(kanjiLibraryService)
    val beginner = listOf(dailyContentDto(KanjiLevel.BEGINNER, id = 1L), dailyContentDto(KanjiLevel.BEGINNER, id = 2L))
    every { kanjiLibraryService.lessons(KanjiLevel.BEGINNER) } returns beginner
    every { kanjiLibraryService.allLessons() } returns mapOf(
        KanjiLevel.BEGINNER to beginner,
        KanjiLevel.INTERMEDIATE to emptyList(),
        KanjiLevel.ADVANCED to emptyList(),
    )

    Given("레벨별 학습 목록 페이지에서") {
        val result = mockMvc.get("/lessons/beginner")

        Then("검색 노출용 head 와 학습 링크를 보여준다") {
            result.andExpect {
                status { isOk() }
                content { string(containsString("<meta name=\"robots\" content=\"index, follow\">")) }
                content { string(containsString("<link rel=\"canonical\" href=\"https://example.com/lessons/beginner\">")) }
                content { string(containsString("초급 일본어 한자 학습 목록")) }
                content { string(containsString("/daily-content/beginner/1")) }
                content { string(containsString("/daily-content/beginner/2")) }
                content { string(not(containsString("noindex"))) }
            }
        }
    }

    Given("전체 학습 목록 페이지에서") {
        val result = mockMvc.get("/lessons")

        Then("모든 레벨 섹션을 보여준다") {
            result.andExpect {
                status { isOk() }
                content { string(containsString("<link rel=\"canonical\" href=\"https://example.com/lessons\">")) }
                content { string(containsString("JLPT N3~N2")) }
                content { string(containsString("JLPT N1")) }
            }
        }
    }

    Given("없는 레벨 목록은") {
        Then("404") {
            mockMvc.get("/lessons/expert").andExpect { status { isNotFound() } }
        }
    }

    Given("한자 페이지에서") {
        every { kanjiLibraryService.kanji("増") } returns KanjiEntryDto(
            kanji = "増",
            readings = listOf("(ぞう, ふ)"),
            meanings = listOf("늘다, 증가하다"),
            pairedKanji = listOf("減"),
            lessons = listOf(KanjiLessonRef(KanjiLevel.INTERMEDIATE, 5L, "増", "減", "늘다", "줄다")),
            words = listOf(KanjiWord("増える(ふえる)", "늘다", KanjiLevel.INTERMEDIATE)),
            sentences = listOf(KanjiSentence("<ruby><rb>人口</rb><rt>じんこう</rt></ruby>が<ruby><rb>増</rb><rt>ふ</rt></ruby>えた。<script>", "인구가 늘었다.", KanjiLevel.INTERMEDIATE, 5L)),
        )
        val result = mockMvc.get("/kanji/増")

        Then("읽기·단어·예문·학습 링크를 보여주고 예문 마크업은 이스케이프한다") {
            result.andExpect {
                status { isOk() }
                content { string(containsString("増 읽는 법, 뜻, 단어")) }
                content { string(containsString("(ぞう, ふ)")) }
                content { string(containsString("増える")) }
                content { string(containsString("<ruby>増<rt>ふ</rt></ruby>")) }
                content { string(containsString("&lt;script&gt;")) }
                content { string(containsString("/daily-content/intermediate/5")) }
                content { string(containsString("/kanji/%E6%B8%9B")) }
            }
        }
    }

    Given("학습에 없는 한자 페이지는") {
        every { kanjiLibraryService.kanji("猫") } returns null

        Then("404") {
            mockMvc.get("/kanji/猫").andExpect { status { isNotFound() } }
        }
    }

    Given("소개·개인정보처리방침 페이지에서") {
        Then("검색 노출용으로 렌더링하고 문의처가 없으면 피드백 페이지로 안내한다") {
            mockMvc.get("/about").andExpect {
                status { isOk() }
                content { string(containsString("매일 칸지 소개")) }
                content { string(containsString("/feedback")) }
            }
            mockMvc.get("/privacy").andExpect {
                status { isOk() }
                content { string(containsString("개인정보처리방침")) }
                content { string(containsString("Resend, Inc.")) }
                content { string(containsString("<link rel=\"canonical\" href=\"https://example.com/privacy\">")) }
            }
        }
    }
}) {
    @TestConfiguration
    class MockConfig {
        @Bean
        fun kanjiLibraryService(): KanjiLibraryService = mockk()

        // WebMvcConfig 의 AdminAuthInterceptor 가 요구
        @Bean
        fun adminService(): AdminService = mockk()
    }
}
