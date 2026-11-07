package maeilkanji.maeilkanji.business.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.domain.QuizChoices
import maeilkanji.maeilkanji.business.repository.DailyContentRepository
import maeilkanji.maeilkanji.fixture.dailyContentDto
import maeilkanji.maeilkanji.fixture.multipleChoiceContentDto
import org.springframework.core.io.ByteArrayResource
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

private const val BASE_URL = "https://maeilkanji.test"

private const val DAILY_TEMPLATE =
    "{{level}}|{{kanji1}}|{{yomi1}}|{{meaning1}}|{{kanji2}}|{{yomi2}}|{{meaning2}}|{{quiz}}|" +
        "{{SITE_ACCESS_URL}}|{{UNSUBSCRIBE_URL}}|{{LEVEL_CHANGE_URL}}|{{LOGO_URL}}|{{FEEDBACK_URL}}"

class EmailTemplateServiceTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerLeaf

    val dailyContentRepository = mockk<DailyContentRepository>()
    val emailTemplateService = EmailTemplateService(
        dailyContentRepository,
        BASE_URL,
        ByteArrayResource("{{LOGO_URL}}|인증 코드: {{CODE}}".toByteArray()),
        ByteArrayResource(DAILY_TEMPLATE.toByteArray()),
        ByteArrayResource("{{LOGO_URL}}|관리자 코드: {{CODE}}".toByteArray()),
    ).apply { init() }

    val memberId = UUID.fromString("11111111-2222-3333-4444-555555555555")

    Given("인증 메일 템플릿이 로드되어 있을 때") {
        When("코드 1234 로 템플릿을 생성하면") {
            val result = emailTemplateService.generateEmailVerificationTemplate("1234")

            Then("{{LOGO_URL}} 을 로고 절대 URL 로, {{CODE}} 를 코드로 치환한다") {
                result shouldBe "$BASE_URL/maeilkanji.png|인증 코드: 1234"
            }
        }
    }

    Given("한국 시간으로 막 새해가 된 시점일 때 (UTC 기준으로는 아직 12월 31일)") {
        val newYearInKst = Clock.fixed(Instant.parse("2026-12-31T15:00:00Z"), ZoneId.of("Asia/Seoul"))
        val service = EmailTemplateService(
            dailyContentRepository,
            BASE_URL,
            ByteArrayResource("© {{YEAR}} 인증 코드: {{CODE}}".toByteArray()),
            ByteArrayResource("© {{YEAR}}".toByteArray()),
            ByteArrayResource("© {{YEAR}} 관리자 코드: {{CODE}}".toByteArray()),
            newYearInKst,
        ).apply { init() }
        every { dailyContentRepository.getBeginnerDailyContentById(1L) } returns dailyContentDto(KanjiLevel.BEGINNER, id = 1L)

        When("메일 템플릿들을 생성하면") {
            val verification = service.generateEmailVerificationTemplate("1234")
            val adminLogin = service.generateAdminLoginCodeTemplate("code")
            val daily = service.generateDailyContentEmailTemplate(KanjiLevel.BEGINNER, 1L, memberId)["template"]!!

            Then("{{YEAR}} 를 한국 시간 기준 연도(2027)로 치환한다") {
                verification shouldBe "© 2027 인증 코드: 1234"
                adminLogin shouldBe "© 2027 관리자 코드: code"
                daily shouldBe "© 2027"
            }
        }
    }

    Given("관리자 로그인 메일 템플릿이 로드되어 있을 때") {
        When("코드로 템플릿을 생성하면") {
            val result = emailTemplateService.generateAdminLoginCodeTemplate("Ab3dE5gH9k")

            Then("{{LOGO_URL}} 을 로고 절대 URL 로, {{CODE}} 를 코드로 치환한다") {
                result shouldBe "$BASE_URL/maeilkanji.png|관리자 코드: Ab3dE5gH9k"
            }
        }
    }

    Given("초급 3번 콘텐츠가 있을 때") {
        every { dailyContentRepository.getBeginnerDailyContentById(3L) } returns dailyContentDto(KanjiLevel.BEGINNER, id = 3L, quiz = "Q3")

        When("초급 데일리 메일 템플릿을 생성하면") {
            val result = emailTemplateService.generateDailyContentEmailTemplate(KanjiLevel.BEGINNER, 3L, memberId)

            Then("모든 플레이스홀더를 치환하고 quiz 를 함께 반환한다") {
                result["quiz"] shouldBe "Q3"
                result["template"] shouldBe listOf(
                    "초급", "日", "にち", "날 일", "月", "げつ", "달 월", "Q3",
                    "$BASE_URL/daily-content/beginner/3?memberId=$memberId",
                    "$BASE_URL/unsubscribe-confirm?memberId=$memberId",
                    "$BASE_URL/change-level-confirm?memberId=$memberId",
                    "$BASE_URL/maeilkanji.png",
                    "$BASE_URL/feedback?memberId=$memberId&context=%EC%B4%88%EA%B8%89+%233",
                ).joinToString("|")
                result["template"]!! shouldNotContain "{{"
            }
        }
    }

    Given("오답 보기가 있는 고급 2번 콘텐츠가 있을 때") {
        val content = multipleChoiceContentDto(KanjiLevel.ADVANCED, id = 2L)
        every { dailyContentRepository.getAdvancedDailyContentById(2L) } returns content
        val service = EmailTemplateService(
            dailyContentRepository,
            BASE_URL,
            ByteArrayResource("".toByteArray()),
            ByteArrayResource("{{QUIZ_CHOICES}}".toByteArray()),
            ByteArrayResource("".toByteArray()),
        ).apply { init() }

        When("데일리 메일 템플릿을 생성하면") {
            val template = service.generateDailyContentEmailTemplate(KanjiLevel.ADVANCED, 2L, memberId)["template"]!!
            val quiz = QuizChoices.of(content)!!

            Then("보기 4개를 결과 페이지 링크(보기 번호 + memberId)로 그린다") {
                quiz.choices.forEachIndexed { index, choice ->
                    template shouldContain "$BASE_URL/daily-content/advanced/2/quiz?choice=$index&memberId=$memberId"
                    template shouldContain choice
                }
                template shouldNotContain "choice=4"
            }
        }
    }

    Given("오답 보기가 없는 콘텐츠가 있을 때") {
        every { dailyContentRepository.getBeginnerDailyContentById(9L) } returns dailyContentDto(KanjiLevel.BEGINNER, id = 9L)
        val service = EmailTemplateService(
            dailyContentRepository,
            BASE_URL,
            ByteArrayResource("".toByteArray()),
            ByteArrayResource("[{{QUIZ_CHOICES}}]".toByteArray()),
            ByteArrayResource("".toByteArray()),
        ).apply { init() }

        When("데일리 메일 템플릿을 생성하면") {
            val template = service.generateDailyContentEmailTemplate(KanjiLevel.BEGINNER, 9L, memberId)["template"]!!

            Then("보기 영역을 비운다") {
                template shouldBe "[]"
            }
        }
    }

    Given("읽기 표기에 괄호가 포함된 콘텐츠가 있을 때") {
        every { dailyContentRepository.getBeginnerDailyContentById(5L) } returns
            dailyContentDto(KanjiLevel.BEGINNER, id = 5L).copy(yomi1 = "(ぞう, ふ)", yomi2 = "（げん, へ）")

        When("데일리 메일 템플릿을 생성하면") {
            val parts = emailTemplateService.generateDailyContentEmailTemplate(KanjiLevel.BEGINNER, 5L, memberId)["template"]!!.split("|")

            Then("학습 페이지와 같이 괄호를 제거한다") {
                parts[2] shouldBe "ぞう, ふ"
                parts[5] shouldBe "げん, へ"
            }
        }
    }

    Given("중급/고급 콘텐츠가 있을 때") {
        every { dailyContentRepository.getIntermediateDailyContentById(1L) } returns dailyContentDto(KanjiLevel.INTERMEDIATE, id = 1L)
        every { dailyContentRepository.getAdvancedDailyContentById(1L) } returns dailyContentDto(KanjiLevel.ADVANCED, id = 1L)

        When("중급 템플릿을 생성하면") {
            val template = emailTemplateService.generateDailyContentEmailTemplate(KanjiLevel.INTERMEDIATE, 1L, memberId)["template"]!!

            Then("중급 접근 URL 과 레벨명을 사용한다") {
                template.split("|")[0] shouldBe "중급"
                template.split("|")[8] shouldBe "$BASE_URL/daily-content/intermediate/1?memberId=$memberId"
            }
        }

        When("고급 템플릿을 생성하면") {
            val template = emailTemplateService.generateDailyContentEmailTemplate(KanjiLevel.ADVANCED, 1L, memberId)["template"]!!

            Then("고급 저장소를 조회하고 고급 접근 URL 을 사용한다") {
                template.split("|")[8] shouldBe "$BASE_URL/daily-content/advanced/1?memberId=$memberId"
                verify(exactly = 1) { dailyContentRepository.getAdvancedDailyContentById(1L) }
            }
        }
    }

    Given("콘텐츠가 존재하지 않을 때") {
        every { dailyContentRepository.getBeginnerDailyContentById(any()) } throws NoSuchElementException("not found")

        When("템플릿을 생성하면") {
            Then("저장소 예외를 그대로 전파한다") {
                shouldThrow<NoSuchElementException> {
                    emailTemplateService.generateDailyContentEmailTemplate(KanjiLevel.BEGINNER, 0L, memberId)
                }
            }
        }
    }
})
