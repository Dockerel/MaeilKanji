package maeilkanji.maeilkanji.presentation.controller.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.Cookie
import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import maeilkanji.maeilkanji.business.service.AdminService
import maeilkanji.maeilkanji.business.service.ProfileService
import maeilkanji.maeilkanji.business.service.request.UpdateProfileServiceRequest
import maeilkanji.maeilkanji.business.service.response.NicknameAvailabilityResponse
import maeilkanji.maeilkanji.business.service.response.UpdateProfileResponse
import maeilkanji.maeilkanji.presentation.auth.MemberSessionCookie
import maeilkanji.maeilkanji.presentation.controller.api.request.UpdateProfileRequest
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import java.util.UUID

@WebMvcTest(ProfileRestController::class)
@Import(ProfileRestControllerTest.MockConfig::class)
class ProfileRestControllerTest(
    mockMvc: MockMvc,
    objectMapper: ObjectMapper,
    profileService: ProfileService,
) : BehaviorSpec({
    extensions(SpringExtension)
    isolationMode = IsolationMode.InstancePerLeaf

    // 컨텍스트 캐시로 mock 빈이 공유되므로 leaf 마다 초기화
    clearMocks(profileService)

    val memberId = UUID.fromString("11111111-2222-3333-4444-555555555555")
    val body = UpdateProfileRequest(nickname = "칸지왕", bio = "N2", avatar = "PANDA", theme = "MATCHA", featuredBadge = "STREAK_7")

    fun putProfile(cookie: Cookie?) = mockMvc.put("/api/v1/profile/me") {
        contentType = MediaType.APPLICATION_JSON
        content = objectMapper.writeValueAsString(body)
        if (cookie != null) cookie(cookie)
    }

    Given("프로필 저장 요청에서") {
        When("회원 쿠키가 있으면") {
            every { profileService.updateProfile(memberId, UpdateProfileServiceRequest("칸지왕", "N2", "PANDA", "MATCHA", "STREAK_7")) } returns
                UpdateProfileResponse("칸지왕", "/u/칸지왕")
            val result = putProfile(Cookie(MemberSessionCookie.NAME, memberId.toString()))

            Then("쿠키의 회원 프로필을 저장한다") {
                result.andExpect {
                    status { isOk() }
                    jsonPath("\$.profileUrl") { value("/u/칸지왕") }
                }
            }
        }

        When("회원 쿠키가 없으면") {
            val result = putProfile(null)

            Then("401 을 반환하고 저장하지 않는다") {
                result.andExpect { status { isUnauthorized() } }
                verify(exactly = 0) { profileService.updateProfile(any(), any()) }
            }
        }

        When("쿠키 값이 UUID 가 아니면") {
            val result = putProfile(Cookie(MemberSessionCookie.NAME, "not-a-uuid"))

            Then("401 을 반환한다") {
                result.andExpect { status { isUnauthorized() } }
            }
        }

        When("잠긴 테마를 고르면") {
            every { profileService.updateProfile(memberId, any()) } throws
                MaeilKanjiException("최장 스트릭 14일을 달성하면 쓸 수 있어요.", HttpStatus.BAD_REQUEST)
            val result = putProfile(Cookie(MemberSessionCookie.NAME, memberId.toString()))

            Then("400 과 이유를 반환한다") {
                result.andExpect {
                    status { isBadRequest() }
                    content { string("최장 스트릭 14일을 달성하면 쓸 수 있어요.") }
                }
            }
        }
    }

    Given("닉네임 확인 요청에서") {
        every { profileService.checkNickname(null, "칸지왕") } returns NicknameAvailabilityResponse(true, "사용할 수 있는 닉네임이에요.")

        Then("사용 가능 여부를 반환한다") {
            mockMvc.get("/api/v1/profile/nickname-check?nickname=칸지왕").andExpect {
                status { isOk() }
                jsonPath("\$.available") { value(true) }
            }
        }
    }
}) {
    @TestConfiguration
    class MockConfig {
        @Bean
        fun profileService(): ProfileService = mockk()

        // WebMvcConfig(관리자 인증 인터셉터)가 슬라이스에 포함되므로 필요
        @Bean
        fun adminService(): AdminService = mockk()
    }
}
