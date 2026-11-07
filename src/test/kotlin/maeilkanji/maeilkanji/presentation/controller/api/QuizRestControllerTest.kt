package maeilkanji.maeilkanji.presentation.controller.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.service.AdminService
import maeilkanji.maeilkanji.business.service.QuizService
import maeilkanji.maeilkanji.business.service.request.SubmitQuizSessionServiceRequest
import maeilkanji.maeilkanji.business.service.response.SubmitQuizSessionResponse
import maeilkanji.maeilkanji.presentation.controller.api.request.SubmitQuizSessionRequest
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@WebMvcTest(QuizRestController::class)
@Import(QuizRestControllerTest.MockConfig::class)
class QuizRestControllerTest(
    mockMvc: MockMvc,
    objectMapper: ObjectMapper,
    quizService: QuizService,
) : BehaviorSpec({
    extensions(SpringExtension)
    isolationMode = IsolationMode.InstancePerLeaf

    // 컨텍스트 캐시로 mock 빈이 공유되므로 leaf 마다 초기화
    clearMocks(quizService)

    fun postSession(body: Any) = mockMvc.post("/api/v1/quiz/sessions") {
        contentType = MediaType.APPLICATION_JSON
        content = objectMapper.writeValueAsString(body)
    }

    val memberId = "11111111-2222-3333-4444-555555555555"
    val answers = listOf(listOf(1), listOf(0), listOf(2), listOf(3), listOf(4, 0, 2, 1))

    Given("퀴즈 세션 제출에서") {
        When("필수 값이 모두 있으면") {
            every {
                quizService.submitSession(SubmitQuizSessionServiceRequest(KanjiLevel.BEGINNER, 3L, answers, memberId))
            } returns SubmitQuizSessionResponse(
                results = listOf(true, true, false, true, true), score = 4, questionCount = 5,
                participantCount = 12, averageScore = 3.4, topPercent = 25, questionCorrectRates = listOf(90, 80, 40, 60, 50),
            )
            val result = postSession(SubmitQuizSessionRequest("BEGINNER", 3L, answers, memberId))

            Then("200 과 점수, 통계를 반환한다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("\$.score") { value(4) }
                    jsonPath("\$.results[2]") { value(false) }
                    jsonPath("\$.averageScore") { value(3.4) }
                    jsonPath("\$.topPercent") { value(25) }
                    jsonPath("\$.questionCorrectRates[0]") { value(90) }
                }
            }
        }

        When("memberId 가 없으면 (공유 링크 방문자)") {
            every {
                quizService.submitSession(SubmitQuizSessionServiceRequest(KanjiLevel.BEGINNER, 3L, answers, null))
            } returns SubmitQuizSessionResponse(List(5) { true }, 5, 5, 0, null, null, List(5) { null })
            val result = postSession(mapOf("level" to "BEGINNER", "contentId" to 3, "answers" to answers))

            Then("200 을 반환한다") {
                result.andExpect { status { isOk() } }
            }
        }

        When("레벨이 소문자이면 (경계값)") {
            val result = postSession(SubmitQuizSessionRequest("beginner", 3L, answers, memberId))

            Then("400 을 반환하고 서비스를 호출하지 않는다") {
                result.andExpect { status { isBadRequest() } }
                verify(exactly = 0) { quizService.submitSession(any()) }
            }
        }

        When("답안이 없으면") {
            val result = postSession(mapOf("level" to "BEGINNER", "contentId" to 3))

            Then("400 과 필수 메시지를 반환한다") {
                result.andExpect {
                    status { isBadRequest() }
                    content { string("답안은 필수입니다.") }
                }
                verify(exactly = 0) { quizService.submitSession(any()) }
            }
        }
    }
}) {
    @TestConfiguration
    class MockConfig {
        @Bean
        fun quizService(): QuizService = mockk()

        // WebMvcConfig(관리자 인증 인터셉터)가 슬라이스에 포함되므로 필요
        @Bean
        fun adminService(): AdminService = mockk()
    }
}
