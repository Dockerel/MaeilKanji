package maeilkanji.maeilkanji.presentation.controller.view

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.domain.QuizSessionFactory
import maeilkanji.maeilkanji.business.service.AdminService
import maeilkanji.maeilkanji.business.service.DailyContentService
import maeilkanji.maeilkanji.business.service.QuizService
import maeilkanji.maeilkanji.fixture.dailyContentDto
import maeilkanji.maeilkanji.fixture.multipleChoiceContentDto
import maeilkanji.maeilkanji.fixture.quizContentDto
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(DailyContentController::class)
@Import(DailyContentControllerTest.MockConfig::class)
class DailyContentControllerTest(
    mockMvc: MockMvc,
    dailyContentService: DailyContentService,
    quizService: QuizService,
) : BehaviorSpec({
    extensions(SpringExtension)
    isolationMode = IsolationMode.InstancePerLeaf

    // 컨텍스트 캐시로 mock 빈이 공유되므로 leaf 마다 초기화
    clearMocks(dailyContentService, quizService)

    val content = multipleChoiceContentDto(KanjiLevel.BEGINNER, id = 3L)

    Given("오답 보기가 있는 학습 페이지에서") {
        every { dailyContentService.findBeginnerDailyContent(3L) } returns content
        val result = mockMvc.get("/daily-content/beginner/3")

        Then("보기 4개를 결과 페이지 링크로 보여주고 정답 공개 영역은 없다") {
            result.andExpect {
                status { isOk() }
                (0..3).forEach { content { string(containsString("/daily-content/beginner/3/quiz?choice=$it")) } }
                content { string(not(containsString("answer-reveal\""))) }
            }
        }
    }

    Given("오답 보기가 없는 학습 페이지에서") {
        every { dailyContentService.findBeginnerDailyContent(4L) } returns dailyContentDto(KanjiLevel.BEGINNER, id = 4L)
        val result = mockMvc.get("/daily-content/beginner/4")

        Then("기존 정답 공개 방식을 보여준다") {
            result.andExpect {
                status { isOk() }
                content { string(containsString("정답 보기")) }
                content { string(not(containsString("/quiz?choice="))) }
            }
        }
    }

    Given("오늘의 5문제 페이지에서") {
        val pool = (1L..6L).map { quizContentDto(it) }
        val session = QuizSessionFactory.create(pool[2], pool)!!
        every { quizService.createSession(KanjiLevel.BEGINNER, 3L) } returns session

        When("메일에서 1번 보기를 누르고 들어오면") {
            val result = mockMvc.get("/daily-content/beginner/3/quiz?choice=2")

            Then("5문제 데이터와 고른 보기를 페이지에 넣는다") {
                result.andExpect {
                    status { isOk() }
                    content { string(containsString("var initialChoice = 2;")) }
                    content { string(containsString("WORD_TO_KOREAN")) }
                    content { string(containsString("ARRANGE")) }
                    content { string(containsString("\"contentId\":3")) }
                }
            }
        }

        When("보기 번호 없이 들어오면") {
            val result = mockMvc.get("/daily-content/beginner/3/quiz")

            Then("1번 문제부터 시작한다") {
                result.andExpect {
                    status { isOk() }
                    content { string(containsString("var initialChoice = null;")) }
                }
            }
        }

        When("보기 번호가 범위를 벗어나면 (경계값)") {
            val result = mockMvc.get("/daily-content/beginner/3/quiz?choice=4")

            Then("고른 보기 없이 시작한다") {
                result.andExpect { content { string(containsString("var initialChoice = null;")) } }
            }
        }

        When("세션을 만들 수 없는 콘텐츠면") {
            every { quizService.createSession(KanjiLevel.BEGINNER, 4L) } returns null
            val result = mockMvc.get("/daily-content/beginner/4/quiz?choice=0")

            Then("학습 페이지로 보낸다") {
                result.andExpect { redirectedUrl("/daily-content/beginner/4") }
            }
        }
    }

    Given("없는 레벨 경로로") {
        val result = mockMvc.get("/daily-content/expert/3/quiz?choice=0")

        Then("404 를 반환한다") {
            result.andExpect { status { isNotFound() } }
        }
    }
}) {
    @TestConfiguration
    class MockConfig {
        @Bean
        fun dailyContentService(): DailyContentService = mockk()

        @Bean
        fun quizService(): QuizService = mockk()

        // WebMvcConfig(관리자 인증 인터셉터)가 슬라이스에 포함되므로 필요
        @Bean
        fun adminService(): AdminService = mockk()
    }
}
