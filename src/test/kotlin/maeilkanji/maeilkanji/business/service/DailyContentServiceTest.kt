package maeilkanji.maeilkanji.business.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.domain.MemberStatus
import maeilkanji.maeilkanji.business.publisher.DailyContentEmailPublisher
import maeilkanji.maeilkanji.business.repository.DailyContentRepository
import maeilkanji.maeilkanji.business.repository.MemberRepository
import maeilkanji.maeilkanji.fixture.dailyContentDto
import maeilkanji.maeilkanji.fixture.memberDto

class DailyContentServiceTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerLeaf

    val publisher = mockk<DailyContentEmailPublisher>(relaxUnitFun = true)
    val dailyContentRepository = mockk<DailyContentRepository>()
    val memberRepository = mockk<MemberRepository>()

    val dailyContentService = DailyContentService(publisher, dailyContentRepository, memberRepository)

    Given("레벨별 콘텐츠가 저장되어 있을 때") {
        val beginner = dailyContentDto(KanjiLevel.BEGINNER, id = 1L)
        val intermediate = dailyContentDto(KanjiLevel.INTERMEDIATE, id = 2L)
        val advanced = dailyContentDto(KanjiLevel.ADVANCED, id = 3L)
        every { dailyContentRepository.getBeginnerDailyContentById(1L) } returns beginner
        every { dailyContentRepository.getIntermediateDailyContentById(2L) } returns intermediate
        every { dailyContentRepository.getAdvancedDailyContentById(3L) } returns advanced

        When("각 레벨의 콘텐츠를 id 로 조회하면") {
            val results = listOf(
                dailyContentService.findBeginnerDailyContent(1L),
                dailyContentService.findIntermediateDailyContent(2L),
                dailyContentService.findAdvancedDailyContent(3L),
            )

            Then("해당 레벨 저장소의 콘텐츠를 반환한다") {
                results shouldBe listOf(beginner, intermediate, advanced)
            }
        }
    }

    Given("존재하지 않는 콘텐츠 id 로") {
        every { dailyContentRepository.getBeginnerDailyContentById(999L) } throws NoSuchElementException("not found")

        When("조회하면") {
            Then("저장소 예외를 그대로 전파한다") {
                shouldThrow<NoSuchElementException> { dailyContentService.findBeginnerDailyContent(999L) }
            }
        }
    }

    Given("초급 활성 회원이 2명 있을 때") {
        val m1 = memberDto(email = "a@test.com")
        val m2 = memberDto(email = "b@test.com")
        every { memberRepository.findAllByMemberStatusAndLevel(MemberStatus.ACTIVE, KanjiLevel.BEGINNER) } returns listOf(m1, m2)

        When("초급 메일 발송을 요청하면") {
            dailyContentService.sendBeginnerDailyContentEmails()

            Then("회원마다 초급 메일 메시지를 발행한다") {
                verify(exactly = 1) {
                    publisher.publishSendDailyContentEmailMessage(KanjiLevel.BEGINNER, m1.id, m1.email)
                    publisher.publishSendDailyContentEmailMessage(KanjiLevel.BEGINNER, m2.id, m2.email)
                }
                verify(exactly = 2) { publisher.publishSendDailyContentEmailMessage(any(), any(), any()) }
            }
        }
    }

    Given("중급 활성 회원이 1명 있을 때") {
        val m = memberDto(level = KanjiLevel.INTERMEDIATE)
        every { memberRepository.findAllByMemberStatusAndLevel(MemberStatus.ACTIVE, KanjiLevel.INTERMEDIATE) } returns listOf(m)

        When("중급 메일 발송을 요청하면") {
            dailyContentService.sendIntermediateDailyContentEmails()

            Then("중급 레벨로 메시지를 발행한다") {
                verify(exactly = 1) { publisher.publishSendDailyContentEmailMessage(KanjiLevel.INTERMEDIATE, m.id, m.email) }
            }
        }
    }

    Given("고급 활성 회원이 없을 때 (경계값)") {
        every { memberRepository.findAllByMemberStatusAndLevel(MemberStatus.ACTIVE, KanjiLevel.ADVANCED) } returns emptyList()

        When("고급 메일 발송을 요청하면") {
            dailyContentService.sendAdvancedDailyContentEmails()

            Then("메시지를 발행하지 않는다") {
                verify(exactly = 0) { publisher.publishSendDailyContentEmailMessage(any(), any(), any()) }
            }
        }
    }

    Given("초급 2명, 중급 1명, 고급 0명의 활성 회원이 있을 때") {
        val b1 = memberDto(email = "a@test.com")
        val b2 = memberDto(email = "b@test.com")
        val i1 = memberDto(email = "c@test.com", level = KanjiLevel.INTERMEDIATE)
        every { memberRepository.findAllByMemberStatusAndLevel(MemberStatus.ACTIVE, KanjiLevel.BEGINNER) } returns listOf(b1, b2)
        every { memberRepository.findAllByMemberStatusAndLevel(MemberStatus.ACTIVE, KanjiLevel.INTERMEDIATE) } returns listOf(i1)
        every { memberRepository.findAllByMemberStatusAndLevel(MemberStatus.ACTIVE, KanjiLevel.ADVANCED) } returns emptyList()

        When("오늘 학습 메일 전체 재발송을 요청하면") {
            val count = dailyContentService.resendTodayDailyContentEmails()

            Then("모든 레벨의 회원에게 각자 레벨로 메시지를 발행하고 발행 수를 반환한다") {
                count shouldBe 3
                verify(exactly = 1) {
                    publisher.publishSendDailyContentEmailMessage(KanjiLevel.BEGINNER, b1.id, b1.email)
                    publisher.publishSendDailyContentEmailMessage(KanjiLevel.BEGINNER, b2.id, b2.email)
                    publisher.publishSendDailyContentEmailMessage(KanjiLevel.INTERMEDIATE, i1.id, i1.email)
                }
                verify(exactly = 3) { publisher.publishSendDailyContentEmailMessage(any(), any(), any()) }
            }
        }
    }
})
