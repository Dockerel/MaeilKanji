package maeilkanji.maeilkanji.business.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.domain.QuizChoices
import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import maeilkanji.maeilkanji.business.repository.DailyContentRepository
import maeilkanji.maeilkanji.business.repository.MemberRepository
import maeilkanji.maeilkanji.business.repository.QuizAnswerRepository
import maeilkanji.maeilkanji.business.service.dto.QuizStatsDto
import maeilkanji.maeilkanji.business.service.request.SubmitQuizAnswerServiceRequest
import maeilkanji.maeilkanji.fixture.dailyContentDto
import maeilkanji.maeilkanji.fixture.memberDto
import maeilkanji.maeilkanji.fixture.multipleChoiceContentDto
import org.springframework.http.HttpStatus
import java.util.UUID

class QuizServiceTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerLeaf

    val dailyContentRepository = mockk<DailyContentRepository>()
    val memberRepository = mockk<MemberRepository>()
    val quizAnswerRepository = mockk<QuizAnswerRepository>(relaxUnitFun = true)
    val quizService = QuizService(dailyContentRepository, memberRepository, quizAnswerRepository)

    val level = KanjiLevel.INTERMEDIATE
    val content = multipleChoiceContentDto(level, id = 5L)
    val answerIndex = QuizChoices.of(content)!!.answerIndex
    val wrongIndex = (answerIndex + 1) % 4
    val memberId = UUID.randomUUID()

    every { dailyContentRepository.getIntermediateDailyContentById(5L) } returns content
    every { quizAnswerRepository.getStats(level, 5L) } returns QuizStatsDto(totalCount = 8, correctCount = 5)
    every { memberRepository.findById(memberId) } returns memberDto(id = memberId)

    Given("회원이 처음 응답할 때") {
        every { quizAnswerRepository.exists(memberId, level, 5L) } returns false

        When("정답을 고르면") {
            val response = quizService.submitAnswer(SubmitQuizAnswerServiceRequest(level, 5L, answerIndex, memberId.toString()))

            Then("정답으로 채점해 기록하고 정답률(반올림)을 반환한다") {
                response.correct shouldBe true
                response.answerIndex shouldBe answerIndex
                response.totalCount shouldBe 8
                response.correctRate shouldBe 63
                verify(exactly = 1) { quizAnswerRepository.save(memberId, level, 5L, answerIndex, true) }
            }
        }

        When("오답을 고르면") {
            val response = quizService.submitAnswer(SubmitQuizAnswerServiceRequest(level, 5L, wrongIndex, memberId.toString()))

            Then("오답으로 기록한다") {
                response.correct shouldBe false
                verify(exactly = 1) { quizAnswerRepository.save(memberId, level, 5L, wrongIndex, false) }
            }
        }
    }

    Given("이미 응답한 회원이") {
        every { quizAnswerRepository.exists(memberId, level, 5L) } returns true

        When("다시 풀면") {
            val response = quizService.submitAnswer(SubmitQuizAnswerServiceRequest(level, 5L, answerIndex, memberId.toString()))

            Then("채점은 하지만 집계에 다시 기록하지 않는다") {
                response.correct shouldBe true
                verify(exactly = 0) { quizAnswerRepository.save(any(), any(), any(), any(), any()) }
            }
        }
    }

    Given("memberId 없이 (공유 링크 방문자)") {
        When("응답하면") {
            val response = quizService.submitAnswer(SubmitQuizAnswerServiceRequest(level, 5L, answerIndex, null))

            Then("기록하지 않고 채점과 정답률만 반환한다") {
                response.correct shouldBe true
                response.correctRate shouldBe 63
                verify(exactly = 0) { memberRepository.findById(any()) }
                verify(exactly = 0) { quizAnswerRepository.save(any(), any(), any(), any(), any()) }
            }
        }
    }

    Given("아직 아무도 응답하지 않았을 때") {
        every { quizAnswerRepository.getStats(level, 5L) } returns QuizStatsDto(0, 0)

        When("방문자가 응답하면") {
            val response = quizService.submitAnswer(SubmitQuizAnswerServiceRequest(level, 5L, answerIndex, null))

            Then("정답률은 null 이다") {
                response.correctRate shouldBe null
            }
        }
    }

    Given("잘못된 요청으로") {
        When("보기 번호가 범위를 벗어나면 (경계값)") {
            Then("400 예외가 발생하고 기록하지 않는다") {
                val e = shouldThrow<MaeilKanjiException> {
                    quizService.submitAnswer(SubmitQuizAnswerServiceRequest(level, 5L, 4, memberId.toString()))
                }
                e.status shouldBe HttpStatus.BAD_REQUEST
                verify(exactly = 0) { quizAnswerRepository.save(any(), any(), any(), any(), any()) }
            }
        }

        When("오답 보기가 없는 콘텐츠이면") {
            every { dailyContentRepository.getIntermediateDailyContentById(6L) } returns dailyContentDto(level, id = 6L)

            Then("400 예외가 발생한다") {
                val e = shouldThrow<MaeilKanjiException> {
                    quizService.submitAnswer(SubmitQuizAnswerServiceRequest(level, 6L, 0, null))
                }
                e.status shouldBe HttpStatus.BAD_REQUEST
            }
        }
    }
})
