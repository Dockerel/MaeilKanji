package maeilkanji.maeilkanji.presentation.controller.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import maeilkanji.maeilkanji.business.service.MemberService
import maeilkanji.maeilkanji.business.service.AdminService
import maeilkanji.maeilkanji.business.service.request.ChangeLevelServiceRequest
import maeilkanji.maeilkanji.business.service.request.FeedbackServiceRequest
import maeilkanji.maeilkanji.business.service.request.SendVerificationCodeServiceRequest
import maeilkanji.maeilkanji.business.service.request.SignupServiceRequest
import maeilkanji.maeilkanji.business.service.request.VerifyVerificationCodeServiceRequest
import maeilkanji.maeilkanji.business.service.response.ChangeLevelResponse
import maeilkanji.maeilkanji.business.service.response.FeedbackResponse
import maeilkanji.maeilkanji.business.service.response.SendVerificationCodeResponse
import maeilkanji.maeilkanji.business.service.response.SignupResponse
import maeilkanji.maeilkanji.business.service.response.StopMailResponse
import maeilkanji.maeilkanji.business.service.response.StreakResponse
import maeilkanji.maeilkanji.business.service.response.VerifyEmailVerificationCodeResponse
import maeilkanji.maeilkanji.presentation.controller.api.request.ChangeLevelRequest
import maeilkanji.maeilkanji.presentation.controller.api.request.FeedbackRequest
import maeilkanji.maeilkanji.presentation.controller.api.request.SendVerificationCodeRequest
import maeilkanji.maeilkanji.presentation.controller.api.request.SignupRequest
import maeilkanji.maeilkanji.presentation.controller.api.request.StopMailRequest
import maeilkanji.maeilkanji.presentation.controller.api.request.StreakRequest
import maeilkanji.maeilkanji.presentation.controller.api.request.VerifyVerificationCodeRequest
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

private const val BASE = "/api/v1/members"

@WebMvcTest(MemberRestController::class)
@Import(MemberRestControllerTest.MockConfig::class)
class MemberRestControllerTest(
    mockMvc: MockMvc,
    objectMapper: ObjectMapper,
    memberService: MemberService,
) : BehaviorSpec({
    extensions(SpringExtension)
    isolationMode = IsolationMode.InstancePerLeaf

    // 컨텍스트 캐시로 mock 빈이 공유되므로 leaf 마다 초기화
    clearMocks(memberService)

    fun postJson(path: String, body: Any) = mockMvc.post("$BASE$path") {
        contentType = MediaType.APPLICATION_JSON
        content = objectMapper.writeValueAsString(body)
    }

    val email = "user@test.com"

    Given("올바른 이메일로") {
        every { memberService.sendVerificationCode(SendVerificationCodeServiceRequest(email)) } returns
            SendVerificationCodeResponse("확인 코드가 전송되었습니다.")

        When("확인 코드 전송을 요청하면") {
            val result = postJson("/send-verification-code", SendVerificationCodeRequest(email))

            Then("200 과 안내 메시지를 반환한다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("\$.message") { value("확인 코드가 전송되었습니다.") }
                }
            }
        }
    }

    Given("이미 가입된 이메일로") {
        every { memberService.sendVerificationCode(any()) } throws
            MaeilKanjiException("Email already exists", HttpStatus.BAD_REQUEST)

        When("확인 코드 전송을 요청하면") {
            val result = postJson("/send-verification-code", SendVerificationCodeRequest(email))

            Then("400 과 예외 메시지를 반환한다") {
                result.andExpect {
                    status { isBadRequest() }
                    content { string("Email already exists") }
                }
            }
        }
    }

    Given("이메일 형식 검증에서") {
        When("빈 이메일로 요청하면 (경계값)") {
            val result = postJson("/send-verification-code", SendVerificationCodeRequest(""))

            Then("400 과 필수 메시지를 반환하고 서비스를 호출하지 않는다") {
                result.andExpect {
                    status { isBadRequest() }
                    content { string("이메일은 필수입니다.") }
                }
                verify(exactly = 0) { memberService.sendVerificationCode(any()) }
            }
        }

        When("이메일 형식이 아닌 값으로 요청하면") {
            val result = postJson("/send-verification-code", SendVerificationCodeRequest("not-an-email"))

            Then("400 과 형식 메시지를 반환한다") {
                result.andExpect {
                    status { isBadRequest() }
                    content { string("이메일 형태는 필수입니다.") }
                }
            }
        }
    }

    Given("확인 코드 검증 요청에서") {
        When("코드가 일치하면") {
            every { memberService.verifyEmailVerificationCode(VerifyVerificationCodeServiceRequest(email, "1234")) } returns
                VerifyEmailVerificationCodeResponse(true)
            val result = postJson("/verify-verification-code", VerifyVerificationCodeRequest(email, "1234"))

            Then("200 과 verified=true 를 반환한다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("\$.verified") { value(true) }
                }
            }
        }

        When("코드가 일치하지 않으면") {
            every { memberService.verifyEmailVerificationCode(any()) } returns VerifyEmailVerificationCodeResponse(false)
            val result = postJson("/verify-verification-code", VerifyVerificationCodeRequest(email, "0000"))

            Then("200 과 verified=false 를 반환한다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("\$.verified") { value(false) }
                }
            }
        }

        When("코드가 만료되었으면") {
            every { memberService.verifyEmailVerificationCode(any()) } throws
                MaeilKanjiException("Email verification code not found", HttpStatus.BAD_REQUEST)
            val result = postJson("/verify-verification-code", VerifyVerificationCodeRequest(email, "1234"))

            Then("400 을 반환한다") {
                result.andExpect {
                    status { isBadRequest() }
                    content { string("Email verification code not found") }
                }
            }
        }

        When("코드가 공백이면 (경계값)") {
            val result = postJson("/verify-verification-code", VerifyVerificationCodeRequest(email, " "))

            Then("400 과 필수 메시지를 반환한다") {
                result.andExpect {
                    status { isBadRequest() }
                    content { string("확인 코드는 필수입니다.") }
                }
            }
        }
    }

    Given("회원가입 요청에서") {
        When("인증된 이메일과 올바른 레벨이면") {
            every { memberService.signup(SignupServiceRequest(email, "BEGINNER")) } returns SignupResponse("Successfully signed up.")
            val result = postJson("/signup", SignupRequest(email, "BEGINNER"))

            Then("200 과 성공 메시지를 반환한다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("\$.message") { value("Successfully signed up.") }
                }
            }
        }

        When("이메일 인증을 하지 않았으면") {
            every { memberService.signup(any()) } throws MaeilKanjiException("Email is not verified", HttpStatus.FORBIDDEN)
            val result = postJson("/signup", SignupRequest(email, "BEGINNER"))

            Then("403 을 반환한다") {
                result.andExpect {
                    status { isForbidden() }
                    content { string("Email is not verified") }
                }
            }
        }

        When("허용되지 않은 레벨이면") {
            val result = postJson("/signup", SignupRequest(email, "EXPERT"))

            Then("400 과 레벨 메시지를 반환하고 서비스를 호출하지 않는다") {
                result.andExpect {
                    status { isBadRequest() }
                    content { string("레벨은 BEGINNER, INTERMEDIATE, ADVANCED 중 하나여야 합니다.") }
                }
                verify(exactly = 0) { memberService.signup(any()) }
            }
        }
    }

    Given("레벨 변경 요청에서") {
        val memberId = "11111111-2222-3333-4444-555555555555"

        When("올바른 레벨이면") {
            every { memberService.changeLevel(ChangeLevelServiceRequest(memberId, "ADVANCED")) } returns
                ChangeLevelResponse("Successfully changed level.")
            val result = postJson("/change-level", ChangeLevelRequest(memberId, "ADVANCED"))

            Then("200 과 성공 메시지를 반환한다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("\$.message") { value("Successfully changed level.") }
                }
            }
        }

        When("memberId 가 비어 있으면") {
            val result = postJson("/change-level", ChangeLevelRequest("", "ADVANCED"))

            Then("400 과 필수 메시지를 반환한다") {
                result.andExpect {
                    status { isBadRequest() }
                    content { string("멤버 Id는 필수입니다.") }
                }
            }
        }

        When("레벨이 소문자이면 (경계값)") {
            val result = postJson("/change-level", ChangeLevelRequest(memberId, "advanced"))

            Then("400 을 반환한다") {
                result.andExpect { status { isBadRequest() } }
                verify(exactly = 0) { memberService.changeLevel(any()) }
            }
        }
    }

    Given("수신 거부 요청에서") {
        val memberId = "11111111-2222-3333-4444-555555555555"

        When("memberId 가 있으면") {
            every { memberService.stopMail(memberId) } returns StopMailResponse("Successfully stopped mail.")
            val result = postJson("/unsubscribe", StopMailRequest(memberId))

            Then("200 과 성공 메시지를 반환한다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("\$.message") { value("Successfully stopped mail.") }
                }
            }
        }

        When("memberId 가 비어 있으면") {
            val result = postJson("/unsubscribe", StopMailRequest(""))

            Then("400 을 반환하고 서비스를 호출하지 않는다") {
                result.andExpect {
                    status { isBadRequest() }
                    content { string("멤버 Id는 필수입니다.") }
                }
                verify(exactly = 0) { memberService.stopMail(any()) }
            }
        }
    }

    Given("스트릭 기록 요청에서") {
        val memberId = "11111111-2222-3333-4444-555555555555"

        When("memberId 가 있으면") {
            every { memberService.recordStudyAccess(memberId) } returns StreakResponse(4)
            val result = postJson("/streak", StreakRequest(memberId))

            Then("200 과 갱신된 스트릭을 반환한다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("\$.streak") { value(4) }
                }
            }
        }

        When("memberId 가 비어 있으면") {
            val result = postJson("/streak", StreakRequest(""))

            Then("400 을 반환하고 서비스를 호출하지 않는다") {
                result.andExpect {
                    status { isBadRequest() }
                    content { string("멤버 Id는 필수입니다.") }
                }
                verify(exactly = 0) { memberService.recordStudyAccess(any()) }
            }
        }
    }

    Given("피드백 요청에서") {
        When("피드백을 보내면") {
            every { memberService.feedback(FeedbackServiceRequest("member-1", "TOO_HARD", "어려워요")) } returns
                FeedbackResponse("Successfully feedback.")
            val result = postJson("/feedback", FeedbackRequest("member-1", "TOO_HARD", "어려워요"))

            Then("200 과 성공 메시지를 반환한다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("\$.message") { value("Successfully feedback.") }
                }
            }
        }
    }
}) {
    @TestConfiguration
    class MockConfig {
        @Bean
        fun memberService(): MemberService = mockk()

        // WebMvcConfig(관리자 인증 인터셉터)가 슬라이스에 포함되므로 필요
        @Bean
        fun adminService(): AdminService = mockk()
    }
}
