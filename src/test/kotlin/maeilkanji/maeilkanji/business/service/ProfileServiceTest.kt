package maeilkanji.maeilkanji.business.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import maeilkanji.maeilkanji.business.domain.*
import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import maeilkanji.maeilkanji.business.repository.MemberRepository
import maeilkanji.maeilkanji.business.repository.QuizQuestionAnswerRepository
import maeilkanji.maeilkanji.business.repository.StudyDayRepository
import maeilkanji.maeilkanji.business.service.dto.QuizSessionRecordDto
import maeilkanji.maeilkanji.business.service.request.UpdateProfileServiceRequest
import maeilkanji.maeilkanji.fixture.memberDto
import org.springframework.http.HttpStatus
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class ProfileServiceTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerLeaf

    val memberRepository = mockk<MemberRepository>(relaxUnitFun = true)
    val studyDayRepository = mockk<StudyDayRepository>()
    val quizQuestionAnswerRepository = mockk<QuizQuestionAnswerRepository>()
    val rankingService = mockk<RankingService>()
    // 2026-11-07 08:00 KST
    val clock = Clock.fixed(Instant.parse("2026-11-06T23:00:00Z"), ZoneId.of("Asia/Seoul"))
    val profileService = ProfileService(memberRepository, studyDayRepository, quizQuestionAnswerRepository, rankingService, clock)

    val memberId = UUID.randomUUID()
    val today = LocalDate.of(2026, 11, 7)
    val member = memberDto(id = memberId, streak = 7, maxStreak = 7, lastAccessedAt = LocalDateTime.of(2026, 11, 7, 7, 0))

    every { memberRepository.findById(memberId) } returns member
    every { studyDayRepository.count(memberId) } returns 8
    every { quizQuestionAnswerRepository.findSessionsByMember(memberId) } returns listOf(
        QuizSessionRecordDto(KanjiLevel.BEGINNER, 1L, 5, 5, today),
        QuizSessionRecordDto(KanjiLevel.BEGINNER, 2L, 2, 5, today.minusDays(2)),
    )
    every { studyDayRepository.findDates(memberId, any()) } returns setOf(today, today.minusDays(1), today.minusDays(2))
    every { rankingService.ranksOf(memberId) } returns mapOf(RankingType.CURRENT_STREAK to 3)

    fun request(
        nickname: String? = "칸지왕",
        bio: String? = null,
        avatar: String? = null,
        theme: String? = null,
        featuredBadge: String? = null,
    ) = UpdateProfileServiceRequest(nickname, bio, avatar, theme, featuredBadge)

    Given("프로필을 조회하면") {
        val profile = profileService.getProfile(memberId)

        Then("통계·배지·잔디를 계산한다") {
            profile.displayName shouldBe "익명의 학습자"
            profile.currentStreak shouldBe 7
            profile.studyDays shouldBe 8
            profile.quizSessions shouldBe 2
            profile.perfectSessions shouldBe 1
            profile.averageQuizRate shouldBe 70
            profile.currentStreakRank shouldBe 3
            profile.badges.filter { it.earned }.map { it.badge } shouldBe
                listOf(Badge.STREAK_3, Badge.STREAK_7, Badge.QUIZ_FIRST, Badge.PERFECT_1)
            profile.nextTheme shouldBe ProfileTheme.SUNSET
            profile.daysToNextTheme shouldBe 7
        }

        Then("잔디는 방문 1, 퀴즈 점수에 따라 2~4 단계로 칠한다") {
            val levels = profile.heatmap.weeks.flatMap { it.days }.associate { it.date to it.level }
            levels[today] shouldBe 4
            levels[today.minusDays(1)] shouldBe 1
            levels[today.minusDays(2)] shouldBe 2
            levels[today.minusDays(3)] shouldBe 0
        }
    }

    Given("없는 닉네임으로 공개 프로필을 찾으면") {
        every { memberRepository.findByNickname("nobody") } returns null

        Then("404 를 던진다") {
            shouldThrow<MaeilKanjiException> { profileService.getPublicProfile("nobody") }.status shouldBe HttpStatus.NOT_FOUND
        }
    }

    Given("닉네임 중복 확인에서") {
        every { memberRepository.findByNickname("칸지왕") } returns null
        every { memberRepository.findByNickname("taken") } returns memberDto(id = UUID.randomUUID(), nickname = "taken")
        every { memberRepository.findByNickname("mine") } returns member.copy(nickname = "mine")

        Then("규칙에 맞고 아무도 안 쓰면 쓸 수 있다") {
            profileService.checkNickname(memberId, " 칸지왕 ").available shouldBe true
        }

        Then("다른 회원이 쓰고 있으면 쓸 수 없다") {
            profileService.checkNickname(memberId, "taken").available shouldBe false
        }

        Then("내가 쓰고 있는 닉네임은 그대로 쓸 수 있다") {
            profileService.checkNickname(memberId, "mine").available shouldBe true
        }

        Then("규칙에 맞지 않으면 쓸 수 없다") {
            profileService.checkNickname(memberId, "a").message shouldBe NicknamePolicy.RULE_MESSAGE
        }
    }

    Given("최장 스트릭 7일 회원이 프로필을 꾸밀 때") {
        every { memberRepository.findByNickname("칸지왕") } returns null

        When("열린 아바타·테마와 얻은 배지를 고르면") {
            val response = profileService.updateProfile(
                memberId, request(bio = "  N2 목표  ", avatar = "PANDA", theme = "MATCHA", featuredBadge = "STREAK_7"),
            )

            Then("저장하고 공개 프로필 주소를 돌려준다") {
                response.profileUrl shouldBe "/u/%EC%B9%B8%EC%A7%80%EC%99%95"
                verify(exactly = 1) {
                    memberRepository.update(memberId, match {
                        it.nickname == "칸지왕" && it.bio == "N2 목표" && it.avatar == ProfileAvatar.PANDA &&
                            it.profileTheme == ProfileTheme.MATCHA && it.featuredBadge == Badge.STREAK_7
                    })
                }
            }
        }

        When("닉네임을 비우면") {
            val response = profileService.updateProfile(memberId, request(nickname = "  "))

            Then("익명으로 저장하고 /me 로 보낸다") {
                response.profileUrl shouldBe "/me"
                verify(exactly = 1) { memberRepository.update(memberId, match { it.nickname == null }) }
            }
        }

        When("아직 잠긴 테마(14일)를 고르면") {
            Then("400 을 던지고 저장하지 않는다") {
                shouldThrow<MaeilKanjiException> { profileService.updateProfile(memberId, request(theme = "SUNSET")) }
                    .status shouldBe HttpStatus.BAD_REQUEST
                verify(exactly = 0) { memberRepository.update(any<UUID>(), any()) }
            }
        }

        When("아직 잠긴 아바타(14일)를 고르면") {
            Then("400 을 던진다") {
                shouldThrow<MaeilKanjiException> { profileService.updateProfile(memberId, request(avatar = "OWL")) }
                    .status shouldBe HttpStatus.BAD_REQUEST
            }
        }

        When("얻지 않은 배지를 대표 배지로 고르면") {
            Then("400 을 던진다") {
                shouldThrow<MaeilKanjiException> { profileService.updateProfile(memberId, request(featuredBadge = "STREAK_30")) }
                    .message shouldBe "아직 얻지 않은 배지입니다."
            }
        }

        When("없는 테마 이름을 보내면") {
            Then("400 을 던진다") {
                shouldThrow<MaeilKanjiException> { profileService.updateProfile(memberId, request(theme = "HACK")) }
                    .status shouldBe HttpStatus.BAD_REQUEST
            }
        }

        When("소개가 100자를 넘으면 (경계값)") {
            Then("400 을 던진다") {
                shouldThrow<MaeilKanjiException> { profileService.updateProfile(memberId, request(bio = "가".repeat(101))) }
                    .status shouldBe HttpStatus.BAD_REQUEST
            }
        }

        When("다른 회원의 닉네임을 쓰려 하면") {
            every { memberRepository.findByNickname("taken") } returns memberDto(id = UUID.randomUUID(), nickname = "taken")

            Then("400 을 던진다") {
                shouldThrow<MaeilKanjiException> { profileService.updateProfile(memberId, request(nickname = "taken")) }
                    .message shouldBe "이미 사용 중인 닉네임이에요."
            }
        }
    }
})
