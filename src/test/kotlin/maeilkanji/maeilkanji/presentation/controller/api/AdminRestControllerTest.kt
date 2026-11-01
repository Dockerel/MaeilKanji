package maeilkanji.maeilkanji.presentation.controller.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
import jakarta.servlet.http.Cookie
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.domain.MemberStatus
import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import maeilkanji.maeilkanji.business.service.AdminService
import maeilkanji.maeilkanji.business.service.SendAdminLoginCodeResponse
import maeilkanji.maeilkanji.business.service.dto.FeedbackDto
import maeilkanji.maeilkanji.business.service.request.SendAdminLoginCodeServiceRequest
import maeilkanji.maeilkanji.business.service.request.VerifyAdminLoginCodeServiceRequest
import maeilkanji.maeilkanji.fixture.contentRequest
import maeilkanji.maeilkanji.fixture.dailyContentDto
import maeilkanji.maeilkanji.fixture.memberDto
import maeilkanji.maeilkanji.presentation.auth.AdminSessionCookie
import maeilkanji.maeilkanji.presentation.controller.api.request.BulkContentRequest
import maeilkanji.maeilkanji.presentation.controller.api.request.SendAdminLoginCodeRequest
import maeilkanji.maeilkanji.presentation.controller.api.request.VerifyAdminLoginCodeRequest
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.LocalDateTime
import java.util.UUID

private const val BASE = "/api/v1/admin"

@WebMvcTest(AdminRestController::class)
@Import(AdminRestControllerTest.MockConfig::class)
class AdminRestControllerTest(
    mockMvc: MockMvc,
    objectMapper: ObjectMapper,
    adminService: AdminService,
) : BehaviorSpec({
    extensions(SpringExtension)
    isolationMode = IsolationMode.InstancePerLeaf

    // 컨텍스트 캐시로 mock 빈이 공유되므로 leaf 마다 초기화
    clearMocks(adminService)
    // 기본은 로그인된 관리자. 인증 실패 케이스는 leaf 에서 덮어쓴다
    every { adminService.isValidAdminSession(any()) } returns true

    fun postJson(path: String, body: Any) = mockMvc.post("$BASE$path") {
        contentType = MediaType.APPLICATION_JSON
        content = objectMapper.writeValueAsString(body)
    }

    val adminEmail = "admin@test.local"

    Given("로그인 코드 전송 요청에서") {
        When("관리자 이메일이면") {
            every { adminService.sendAdminLoginCode(SendAdminLoginCodeServiceRequest(adminEmail)) } returns
                SendAdminLoginCodeResponse("관리자 페이지 로그인 코드가 이메일로 전송되었습니다.")
            val result = postJson("/send-login-code", SendAdminLoginCodeRequest(adminEmail))

            Then("200 과 안내 메시지를 반환한다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("\$.message") { value("관리자 페이지 로그인 코드가 이메일로 전송되었습니다.") }
                }
            }
        }

        When("관리자가 아닌 이메일이면") {
            every { adminService.sendAdminLoginCode(any()) } throws MaeilKanjiException("Unauthorized", HttpStatus.UNAUTHORIZED)
            val result = postJson("/send-login-code", SendAdminLoginCodeRequest("user@test.com"))

            Then("401 을 반환한다") {
                result.andExpect {
                    status { isUnauthorized() }
                    content { string("Unauthorized") }
                }
            }
        }
    }

    Given("로그인 코드 검증 요청에서") {
        When("코드가 일치하면") {
            every { adminService.verifyAdminLoginCode(VerifyAdminLoginCodeServiceRequest(adminEmail, "Ab3dE5gH9k")) } returns "token-123"
            val result = postJson("/verify-login", VerifyAdminLoginCodeRequest(adminEmail, "Ab3dE5gH9k"))

            Then("200 과 success=true, HttpOnly 세션 쿠키를 반환한다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("\$.success") { value(true) }
                    cookie {
                        value(AdminSessionCookie.NAME, "token-123")
                        httpOnly(AdminSessionCookie.NAME, true)
                        maxAge(AdminSessionCookie.NAME, 8 * 60 * 60)
                    }
                }
            }
        }

        When("코드가 일치하지 않으면") {
            every { adminService.verifyAdminLoginCode(any()) } returns null
            val result = postJson("/verify-login", VerifyAdminLoginCodeRequest(adminEmail, "wrong"))

            Then("200 과 success=false 를 반환하고 쿠키를 발급하지 않는다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("\$.success") { value(false) }
                    cookie { doesNotExist(AdminSessionCookie.NAME) }
                }
            }
        }

        When("시도 횟수를 초과했으면") {
            every { adminService.verifyAdminLoginCode(any()) } throws
                MaeilKanjiException("Too many attempts. Request a new code.", HttpStatus.TOO_MANY_REQUESTS)
            val result = postJson("/verify-login", VerifyAdminLoginCodeRequest(adminEmail, "Ab3dE5gH9k"))

            Then("429 를 반환한다") {
                result.andExpect { status { isTooManyRequests() } }
            }
        }

        When("코드가 만료되었으면") {
            every { adminService.verifyAdminLoginCode(any()) } throws
                MaeilKanjiException("Code expired or not found", HttpStatus.BAD_REQUEST)
            val result = postJson("/verify-login", VerifyAdminLoginCodeRequest(adminEmail, "Ab3dE5gH9k"))

            Then("400 을 반환한다") {
                result.andExpect {
                    status { isBadRequest() }
                    content { string("Code expired or not found") }
                }
            }
        }
    }

    Given("관리자 세션이 없을 때") {
        every { adminService.isValidAdminSession(any()) } returns false

        When("관리자 API 를 호출하면") {
            val result = mockMvc.get("$BASE/members")

            Then("401 을 반환하고 서비스를 호출하지 않는다") {
                result.andExpect {
                    status { isUnauthorized() }
                    content { string("Unauthorized") }
                }
                verify(exactly = 0) { adminService.getMembers() }
            }
        }

        When("로그인 코드 전송 API 를 호출하면") {
            every { adminService.sendAdminLoginCode(any()) } returns SendAdminLoginCodeResponse("ok")
            val result = postJson("/send-login-code", SendAdminLoginCodeRequest(adminEmail))

            Then("인증 없이 통과한다") {
                result.andExpect { status { isOk() } }
            }
        }
    }

    Given("세션 쿠키가 있을 때") {
        When("관리자 API 를 호출하면") {
            every { adminService.isValidAdminSession("tok") } returns true
            every { adminService.getMembers() } returns emptyList()
            val result = mockMvc.get("$BASE/members") { cookie(Cookie(AdminSessionCookie.NAME, "tok")) }

            Then("쿠키의 토큰으로 세션을 확인한다") {
                result.andExpect { status { isOk() } }
                verify(exactly = 1) { adminService.isValidAdminSession("tok") }
            }
        }

        When("로그아웃하면") {
            every { adminService.logoutAdmin("tok") } just Runs
            val result = mockMvc.post("$BASE/logout") { cookie(Cookie(AdminSessionCookie.NAME, "tok")) }

            Then("204 와 만료된 쿠키를 반환하고 세션을 삭제한다") {
                result.andExpect {
                    status { isNoContent() }
                    cookie { maxAge(AdminSessionCookie.NAME, 0) }
                }
                verify(exactly = 1) { adminService.logoutAdmin("tok") }
            }
        }
    }

    Given("콘텐츠 저장 요청에서") {
        When("모든 필드가 채워져 있으면") {
            every { adminService.saveDailyContent(KanjiLevel.INTERMEDIATE, any()) } returns 42L
            val result = postJson("/content", contentRequest(level = "INTERMEDIATE"))

            Then("200 과 생성된 id 를 반환한다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("\$.id") { value(42) }
                }
                verify(exactly = 1) {
                    adminService.saveDailyContent(KanjiLevel.INTERMEDIATE, match { it.kanji1 == "日" && it.id == null })
                }
            }
        }

        When("필수 필드가 공백이면 (경계값)") {
            val result = postJson("/content", contentRequest(kanji1 = " "))

            Then("400 을 반환하고 서비스를 호출하지 않는다") {
                result.andExpect { status { isBadRequest() } }
                verify(exactly = 0) { adminService.saveDailyContent(any(), any()) }
            }
        }

        // 현재 동작 고정: level 에 패턴 검증이 없어 KanjiLevel.valueOf 예외가 500 으로 응답됨
        When("존재하지 않는 레벨이면") {
            val result = postJson("/content", contentRequest(level = "EXPERT"))

            Then("500 을 반환한다") {
                result.andExpect { status { isInternalServerError() } }
                verify(exactly = 0) { adminService.saveDailyContent(any(), any()) }
            }
        }
    }

    Given("콘텐츠 일괄 저장 요청에서") {
        When("모든 행이 유효하면") {
            every { adminService.saveDailyContents(any()) } returns listOf(1L, 2L)
            val result = postJson(
                "/content/bulk",
                BulkContentRequest(listOf(contentRequest(level = "BEGINNER"), contentRequest(level = "ADVANCED"))),
            )

            Then("200 과 저장 개수, id 목록을 반환한다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("\$.count") { value(2) }
                    jsonPath("\$.ids[1]") { value(2) }
                }
                verify(exactly = 1) {
                    adminService.saveDailyContents(match {
                        it.map { c -> c.level } == listOf(KanjiLevel.BEGINNER, KanjiLevel.ADVANCED)
                    })
                }
            }
        }

        When("목록이 비어 있으면 (경계값)") {
            val result = postJson("/content/bulk", BulkContentRequest(emptyList()))

            Then("400 을 반환하고 서비스를 호출하지 않는다") {
                result.andExpect {
                    status { isBadRequest() }
                    content { string("등록할 콘텐츠가 없습니다.") }
                }
                verify(exactly = 0) { adminService.saveDailyContents(any()) }
            }
        }

        When("한 행이라도 필수 필드가 공백이면") {
            val result = postJson(
                "/content/bulk",
                BulkContentRequest(listOf(contentRequest(), contentRequest(kanji1 = ""))),
            )

            Then("400 을 반환하고 아무것도 저장하지 않는다") {
                result.andExpect { status { isBadRequest() } }
                verify(exactly = 0) { adminService.saveDailyContents(any()) }
            }
        }
    }

    Given("레벨별 콘텐츠 조회 요청에서") {
        When("콘텐츠가 있는 레벨이면") {
            every { adminService.getDailyContents(KanjiLevel.BEGINNER) } returns
                listOf(dailyContentDto(id = 1L), dailyContentDto(id = 2L))
            val result = mockMvc.get("$BASE/content/BEGINNER")

            Then("200 과 콘텐츠 목록을 반환한다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("\$.length()") { value(2) }
                    jsonPath("\$[0].id") { value(1) }
                    jsonPath("\$[0].level") { value("BEGINNER") }
                }
            }
        }

        When("콘텐츠가 없는 레벨이면 (경계값)") {
            every { adminService.getDailyContents(KanjiLevel.ADVANCED) } returns emptyList()
            val result = mockMvc.get("$BASE/content/ADVANCED")

            Then("200 과 빈 배열을 반환한다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("\$.length()") { value(0) }
                }
            }
        }

        // 현재 동작 고정: path 의 level 미검증 → 500
        When("존재하지 않는 레벨이면") {
            val result = mockMvc.get("$BASE/content/beginner")

            Then("500 을 반환한다") {
                result.andExpect { status { isInternalServerError() } }
            }
        }
    }

    Given("콘텐츠 삭제 요청에서") {
        When("레벨과 id 가 올바르면") {
            every { adminService.deleteDailyContent(KanjiLevel.ADVANCED, 7L) } just Runs
            val result = mockMvc.delete("$BASE/content/ADVANCED/7")

            Then("204 를 반환하고 삭제를 위임한다") {
                result.andExpect { status { isNoContent() } }
                verify(exactly = 1) { adminService.deleteDailyContent(KanjiLevel.ADVANCED, 7L) }
            }
        }
    }

    Given("회원 관리 요청에서") {
        val memberId = UUID.fromString("11111111-2222-3333-4444-555555555555")

        When("회원 목록을 조회하면") {
            every { adminService.getMembers() } returns listOf(memberDto(id = memberId, memberStatus = MemberStatus.BOUNCED))
            val result = mockMvc.get("$BASE/members")

            Then("200 과 회원 목록을 반환한다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("\$[0].id") { value(memberId.toString()) }
                    jsonPath("\$[0].memberStatus") { value("BOUNCED") }
                }
            }
        }

        When("회원을 재활성화하면") {
            every { adminService.reactivateMember(memberId) } just Runs
            val result = mockMvc.post("$BASE/members/$memberId/reactivate")

            Then("204 를 반환한다") {
                result.andExpect { status { isNoContent() } }
                verify(exactly = 1) { adminService.reactivateMember(memberId) }
            }
        }

        // 현재 동작 고정: UUID 변환 실패(MethodArgumentTypeMismatchException)가 500 으로 응답됨
        When("UUID 형식이 아닌 id 로 재활성화하면") {
            val result = mockMvc.post("$BASE/members/not-a-uuid/reactivate")

            Then("500 을 반환하고 서비스를 호출하지 않는다") {
                result.andExpect { status { isInternalServerError() } }
                verify(exactly = 0) { adminService.reactivateMember(any()) }
            }
        }
    }

    Given("피드백 목록 조회 요청에서") {
        When("피드백이 있으면") {
            every { adminService.getFeedbacks() } returns listOf(
                FeedbackDto(UUID.randomUUID(), "member-1", "TOO_HARD", "어려워요", LocalDateTime.of(2026, 10, 31, 9, 0)),
            )
            val result = mockMvc.get("$BASE/feedbacks")

            Then("200 과 피드백 목록을 반환한다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("\$[0].memberId") { value("member-1") }
                    jsonPath("\$[0].comment") { value("어려워요") }
                }
            }
        }
    }
}) {
    @TestConfiguration
    class MockConfig {
        @Bean
        fun adminService(): AdminService = mockk()
    }
}
