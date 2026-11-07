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
import maeilkanji.maeilkanji.business.service.request.SubmitQuizAnswerServiceRequest
import maeilkanji.maeilkanji.business.service.response.SubmitQuizAnswerResponse
import maeilkanji.maeilkanji.presentation.controller.api.request.SubmitQuizAnswerRequest
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

    fun postAnswer(body: Any) = mockMvc.post("/api/v1/quiz/answers") {
        contentType = MediaType.APPLICATION_JSON
        content = objectMapper.writeValueAsString(body)
    }

    val memberId = "11111111-2222-3333-4444-555555555555"

    Given("퀴즈 응답 요청에서") {
        When("필수 값이 모두 있으면") {
            every {
                quizService.submitAnswer(SubmitQuizAnswerServiceRequest(KanjiLevel.BEGINNER, 3L, 2, memberId))
            } returns SubmitQuizAnswerResponse(correct = true, answerIndex = 2, totalCount = 10, correctCount = 6, correctRate = 60)
            val result = postAnswer(SubmitQuizAnswerRequest("BEGINNER", 3L, 2, memberId))

            Then("200 과 채점 결과, 정답률을 반환한다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("\$.correct") { value(true) }
                    jsonPath("\$.answerIndex") { value(2) }
                    jsonPath("\$.totalCount") { value(10) }
                    jsonPath("\$.correctRate") { value(60) }
                }
            }
        }

        When("memberId 가 없으면 (공유 링크 방문자)") {
            every {
                quizService.submitAnswer(SubmitQuizAnswerServiceRequest(KanjiLevel.BEGINNER, 3L, 0, null))
            } returns SubmitQuizAnswerResponse(false, 2, 0, 0, null)
            val result = postAnswer(mapOf("level" to "BEGINNER", "contentId" to 3, "choice" to 0))

            Then("200 을 반환한다") {
                result.andExpect { status { isOk() } }
            }
        }

        When("레벨이 소문자이면 (경계값)") {
            val result = postAnswer(SubmitQuizAnswerRequest("beginner", 3L, 0, memberId))

            Then("400 을 반환하고 서비스를 호출하지 않는다") {
                result.andExpect { status { isBadRequest() } }
                verify(exactly = 0) { quizService.submitAnswer(any()) }
            }
        }

        When("보기 번호가 없으면") {
            val result = postAnswer(mapOf("level" to "BEGINNER", "contentId" to 3))

            Then("400 과 필수 메시지를 반환한다") {
                result.andExpect {
                    status { isBadRequest() }
                    content { string("보기 번호는 필수입니다.") }
                }
                verify(exactly = 0) { quizService.submitAnswer(any()) }
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
