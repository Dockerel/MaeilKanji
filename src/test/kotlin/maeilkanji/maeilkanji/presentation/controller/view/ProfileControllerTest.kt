package maeilkanji.maeilkanji.presentation.controller.view

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.Cookie
import maeilkanji.maeilkanji.business.domain.*
import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import maeilkanji.maeilkanji.business.service.AdminService
import maeilkanji.maeilkanji.business.service.ProfileService
import maeilkanji.maeilkanji.business.service.RankingService
import maeilkanji.maeilkanji.business.service.dto.*
import maeilkanji.maeilkanji.presentation.auth.MemberSessionCookie
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.LocalDate
import java.util.UUID

@WebMvcTest(ProfileController::class)
@Import(ProfileControllerTest.MockConfig::class)
class ProfileControllerTest(
    mockMvc: MockMvc,
    profileService: ProfileService,
    rankingService: RankingService,
) : BehaviorSpec({
    extensions(SpringExtension)
    isolationMode = IsolationMode.InstancePerLeaf

    // 컨텍스트 캐시로 mock 빈이 공유되므로 leaf 마다 초기화
    clearMocks(profileService, rankingService)

    val memberId = UUID.fromString("11111111-2222-3333-4444-555555555555")
    val memberCookie = Cookie(MemberSessionCookie.NAME, memberId.toString())
    val today = LocalDate.of(2026, 11, 7)

    fun profile(nickname: String? = "칸지왕") = ProfileDto(
        nickname = nickname,
        bio = "N2 목표",
        avatar = ProfileAvatar.PANDA,
        theme = ProfileTheme.MATCHA,
        featuredBadge = Badge.STREAK_7,
        currentStreak = 7,
        maxStreak = 7,
        studyDays = 8,
        quizSessions = 2,
        perfectSessions = 1,
        averageQuizRate = 70,
        heatmap = StudyHeatmap.of(mapOf(today to DailyActivity(visited = true, bestQuizRatio = 1.0)), today),
        badges = Badge.values().map { BadgeStatusDto(it, it == Badge.STREAK_7 || it == Badge.STREAK_3) },
        ranks = mapOf(RankingType.CURRENT_STREAK to 3, RankingType.MAX_STREAK to null, RankingType.STUDY_DAYS to 5),
        nextTheme = ProfileTheme.SUNSET,
    )

    Given("공개 프로필 페이지에서") {
        every { profileService.getPublicProfile("칸지왕") } returns profile()
        every { profileService.nicknameOf(memberId) } returns "칸지왕"

        When("다른 사람이 보면") {
            val result = mockMvc.get("/u/칸지왕")

            Then("닉네임·테마·잔디·배지를 보여주고 꾸미기 버튼은 없다") {
                result.andExpect {
                    status { isOk() }
                    content { string(containsString("칸지왕")) }
                    content { string(containsString("theme-matcha")) }
                    content { string(containsString("grass l4 today")) }
                    content { string(containsString("일주일 개근")) }
                    content { string(containsString(">3위<")) }
                    content { string(not(containsString("/me/edit"))) }
                }
            }
        }

        When("본인이 보면") {
            val result = mockMvc.get("/u/칸지왕") { cookie(memberCookie) }

            Then("꾸미기 버튼과 다음 테마 안내를 보여준다") {
                result.andExpect {
                    status { isOk() }
                    content { string(containsString("/me/edit")) }
                    content { string(containsString("노을")) }
                }
            }
        }
    }

    Given("없는 닉네임이면") {
        every { profileService.getPublicProfile("nobody") } throws MaeilKanjiException("존재하지 않는 프로필입니다.", HttpStatus.NOT_FOUND)

        Then("404 를 반환한다") {
            mockMvc.get("/u/nobody").andExpect { status { isNotFound() } }
        }
    }

    Given("내 프로필(/me)에서") {
        When("회원 쿠키가 없으면") {
            val result = mockMvc.get("/me")

            Then("메일 링크로 들어오라는 안내를 보여준다") {
                result.andExpect {
                    status { isOk() }
                    content { string(containsString("메일의 학습 링크로 들어와 주세요")) }
                }
            }
        }

        When("메일 링크(?memberId=)로 들어오면") {
            every { profileService.getProfile(memberId) } returns profile()
            val result = mockMvc.get("/me?memberId=$memberId")

            Then("회원 쿠키를 발급하고 주소창에서 memberId 를 지우도록 /me 로 보낸다") {
                result.andExpect {
                    status { is3xxRedirection() }
                    redirectedUrl("/me")
                    cookie { value(MemberSessionCookie.NAME, memberId.toString()) }
                }
            }
        }

        When("쿠키가 있고 닉네임이 없으면") {
            every { profileService.getProfile(memberId) } returns profile(nickname = null)
            val result = mockMvc.get("/me") { cookie(memberCookie) }

            Then("익명으로 보여주고 닉네임 정하기를 안내한다") {
                result.andExpect {
                    status { isOk() }
                    content { string(containsString("익명의 학습자")) }
                    content { string(containsString("닉네임 정하기")) }
                }
            }
        }

        When("쿠키의 회원이 없어졌으면") {
            every { profileService.getProfile(memberId) } throws MaeilKanjiException("없음", HttpStatus.NOT_FOUND)
            val result = mockMvc.get("/me") { cookie(memberCookie) }

            Then("쿠키를 지우고 안내 페이지를 보여준다") {
                result.andExpect {
                    status { isOk() }
                    cookie { maxAge(MemberSessionCookie.NAME, 0) }
                    content { string(containsString("메일의 학습 링크로 들어와 주세요")) }
                }
            }
        }
    }

    Given("프로필 꾸미기 페이지에서") {
        every { profileService.getProfile(memberId) } returns profile()
        val result = mockMvc.get("/me/edit") { cookie(memberCookie) }

        Then("잠긴 아바타·테마는 비활성으로, 얻은 배지만 대표 배지 후보로 보여준다") {
            result.andExpect {
                status { isOk() }
                content { string(containsString("value=\"PANDA\" data-glyph=\"熊\" data-css=\"panda\" checked=\"checked\"")) }
                content { string(containsString("value=\"OWL\" data-glyph=\"梟\" data-css=\"owl\" disabled=\"disabled\"")) }
                content { string(containsString("노을 · 14일")) }
                content { string(containsString("value=\"STREAK_7\"")) }
                content { string(not(containsString("value=\"STREAK_30\""))) }
            }
        }
    }

    Given("랭킹 페이지에서") {
        every { rankingService.getRanking(RankingType.MAX_STREAK, memberId) } returns RankingDto(
            type = RankingType.MAX_STREAK,
            entries = listOf(
                RankingEntryDto(1, "칸지왕", ProfileAvatar.DRAGON, Badge.STREAK_100, 120, me = false),
                RankingEntryDto(2, null, ProfileAvatar.SMILE, null, 30, me = true),
            ),
            myEntry = RankingEntryDto(2, null, ProfileAvatar.SMILE, null, 30, me = true),
            totalRanked = 2,
        )
        val result = mockMvc.get("/ranking?type=max_streak") { cookie(memberCookie) }

        Then("순위·공개 프로필 링크·내 순위를 보여준다") {
            result.andExpect {
                status { isOk() }
                content { string(containsString("podium-slot p1")) }
                content { string(containsString("/u/%EC%B9%B8%EC%A7%80%EC%99%95")) }
                content { string(containsString("120일 연속")) }
                content { string(containsString("내 순위")) }
                content { string(containsString("익명의 학습자")) }
            }
        }
    }

    Given("랭킹 종류 없이 들어오면") {
        every { rankingService.getRanking(RankingType.CURRENT_STREAK, null) } returns
            RankingDto(RankingType.CURRENT_STREAK, emptyList(), null, 0)

        Then("현재 스트릭 랭킹의 빈 화면을 보여준다") {
            mockMvc.get("/ranking").andExpect {
                status { isOk() }
                content { string(containsString("아직 순위에 오른 학습자가 없어요")) }
            }
        }
    }
}) {
    @TestConfiguration
    class MockConfig {
        @Bean
        fun profileService(): ProfileService = mockk()

        @Bean
        fun rankingService(): RankingService = mockk()

        // WebMvcConfig(관리자 인증 인터셉터)가 슬라이스에 포함되므로 필요
        @Bean
        fun adminService(): AdminService = mockk()
    }
}
