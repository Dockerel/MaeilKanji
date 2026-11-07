package maeilkanji.maeilkanji.business.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.domain.LearningStats
import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import maeilkanji.maeilkanji.business.repository.DailyContentRepository
import maeilkanji.maeilkanji.business.repository.MemberRepository
import maeilkanji.maeilkanji.business.repository.QuizQuestionAnswerRepository
import maeilkanji.maeilkanji.business.service.dto.QuizSessionStatsDto
import maeilkanji.maeilkanji.business.service.request.SubmitQuizSessionServiceRequest
import maeilkanji.maeilkanji.fixture.memberDto
import maeilkanji.maeilkanji.fixture.quizContentDto
import org.springframework.http.HttpStatus
import java.util.UUID

class QuizServiceTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerLeaf

    val dailyContentRepository = mockk<DailyContentRepository>()
    val memberRepository = mockk<MemberRepository>()
    val quizQuestionAnswerRepository = mockk<QuizQuestionAnswerRepository>(relaxUnitFun = true)
    val profileService = mockk<ProfileService>()
    val quizService = QuizService(dailyContentRepository, memberRepository, quizQuestionAnswerRepository, profileService)

    val level = KanjiLevel.INTERMEDIATE
    val pool = (1L..10L).map { quizContentDto(it, level) }
    val memberId = UUID.randomUUID()

    every { dailyContentRepository.findAllIntermediate() } returns pool
    every { memberRepository.findById(memberId) } returns memberDto(id = memberId)
    // 3명 참여: 5점, 3점, 2점 / 문제별 정답자 수
    every { quizQuestionAnswerRepository.getStats(level, 5L, 5) } returns
        QuizSessionStatsDto(memberScores = listOf(5, 3, 2), questionCorrectCounts = listOf(3, 2, 2, 1, 2))

    val session = quizService.createSession(level, 5L)!!
    val q = session.questions
    val arrangeAnswer = q[4].answerTexts!!.map { text -> q[4].options.indexOfFirst { it.text == text } }
    val perfect = listOf(listOf(q[0].answerIndex!!), listOf(q[1].answerIndex!!), listOf(q[2].answerIndex!!), listOf(q[3].answerIndex!!), arrangeAnswer)
    val threeCorrect = perfect.take(3) + listOf(listOf((q[3].answerIndex!! + 1) % 4), arrangeAnswer.reversed())

    Given("세션 생성에서") {
        When("없는 콘텐츠 id 면") {
            Then("404 예외가 발생한다") {
                val e = shouldThrow<MaeilKanjiException> { quizService.createSession(level, 99L) }
                e.status shouldBe HttpStatus.NOT_FOUND
            }
        }

        When("예문 블록이 없는 콘텐츠면") {
            every { dailyContentRepository.findAllIntermediate() } returns pool.map { if (it.id == 5L) it.copy(sentenceTiles = null) else it }

            Then("null 을 반환한다") {
                quizService.createSession(level, 5L).shouldBeNull()
            }
        }
    }

    Given("회원이 처음 제출할 때") {
        every { quizQuestionAnswerRepository.exists(memberId, level, 5L) } returns false
        every { profileService.learningStats(memberId) } returns LearningStats()

        When("3문제를 맞히면") {
            val response = quizService.submitSession(SubmitQuizSessionServiceRequest(level, 5L, threeCorrect, memberId.toString()))

            Then("문제별 결과를 기록하고 점수·통계를 반환한다") {
                response.results shouldBe listOf(true, true, true, false, false)
                response.score shouldBe 3
                response.questionCount shouldBe 5
                response.participantCount shouldBe 3
                response.averageScore shouldBe 3.3
                response.topPercent shouldBe 67 // 3점 이상 2명 / 3명
                response.questionCorrectRates shouldBe listOf(100, 67, 67, 33, 67)
                verify(exactly = 1) { quizQuestionAnswerRepository.saveAll(memberId, level, 5L, listOf(true, true, true, false, false)) }
            }
        }
    }

    Given("회원이 첫 퀴즈를 만점으로 제출하면") {
        every { quizQuestionAnswerRepository.exists(memberId, level, 5L) } returns false
        every { profileService.learningStats(memberId) } returnsMany listOf(
            LearningStats(),
            LearningStats(quizSessions = 1, perfectSessions = 1, levelsPlayed = setOf(level)),
        )

        When("제출하면") {
            val response = quizService.submitSession(SubmitQuizSessionServiceRequest(level, 5L, perfect, memberId.toString()))

            Then("기록 전후로 새로 얻은 퀴즈 배지를 반환한다") {
                response.newBadges.map { it.code } shouldBe listOf("QUIZ_FIRST", "PERFECT_1")
            }
        }
    }

    Given("이미 제출한 회원의 배지는") {
        every { quizQuestionAnswerRepository.exists(memberId, level, 5L) } returns true

        When("다시 풀어도") {
            val response = quizService.submitSession(SubmitQuizSessionServiceRequest(level, 5L, perfect, memberId.toString()))

            Then("새 배지가 없고 배지 계산도 하지 않는다") {
                response.newBadges shouldBe emptyList()
                verify(exactly = 0) { profileService.learningStats(any()) }
            }
        }
    }

    Given("이미 제출한 회원이") {
        every { quizQuestionAnswerRepository.exists(memberId, level, 5L) } returns true

        When("다시 풀면") {
            val response = quizService.submitSession(SubmitQuizSessionServiceRequest(level, 5L, perfect, memberId.toString()))

            Then("채점은 하지만 다시 기록하지 않는다") {
                response.score shouldBe 5
                response.topPercent shouldBe 34 // 5점 이상 1명 / 3명, 올림
                verify(exactly = 0) { quizQuestionAnswerRepository.saveAll(any(), any(), any(), any()) }
            }
        }
    }

    Given("memberId 없이 (공유 링크 방문자)") {
        When("제출하면") {
            val response = quizService.submitSession(SubmitQuizSessionServiceRequest(level, 5L, perfect, null))

            Then("기록하지 않고 채점과 통계만 반환한다") {
                response.score shouldBe 5
                verify(exactly = 0) { memberRepository.findById(any()) }
                verify(exactly = 0) { quizQuestionAnswerRepository.saveAll(any(), any(), any(), any()) }
            }
        }
    }

    Given("아직 아무도 참여하지 않았을 때") {
        every { quizQuestionAnswerRepository.getStats(level, 5L, 5) } returns QuizSessionStatsDto(emptyList(), List(5) { 0 })

        When("방문자가 제출하면") {
            val response = quizService.submitSession(SubmitQuizSessionServiceRequest(level, 5L, perfect, null))

            Then("평균·상위%·정답률은 null 이다") {
                response.participantCount shouldBe 0
                response.averageScore.shouldBeNull()
                response.topPercent.shouldBeNull()
                response.questionCorrectRates shouldBe List(5) { null }
            }
        }
    }

    Given("답안 개수가 문제 수와 다르면") {
        Then("400 예외가 발생하고 기록하지 않는다") {
            val e = shouldThrow<MaeilKanjiException> {
                quizService.submitSession(SubmitQuizSessionServiceRequest(level, 5L, perfect.dropLast(1), memberId.toString()))
            }
            e.status shouldBe HttpStatus.BAD_REQUEST
            verify(exactly = 0) { quizQuestionAnswerRepository.saveAll(any(), any(), any(), any()) }
        }
    }

    Given("세션을 만들 수 없는 콘텐츠로 제출하면") {
        every { dailyContentRepository.findAllIntermediate() } returns pool.map { if (it.id == 5L) it.copy(wrongAnswer1 = null) else it }

        Then("400 예외가 발생한다") {
            val e = shouldThrow<MaeilKanjiException> {
                quizService.submitSession(SubmitQuizSessionServiceRequest(level, 5L, perfect, null))
            }
            e.status shouldBe HttpStatus.BAD_REQUEST
        }
    }

    Given("통계 계산에서") {
        Then("세션 점수 분포로 상위 %를 구한다") {
            val stats = QuizSessionStatsDto(listOf(5, 5, 4, 3), List(5) { 0 })
            stats.topPercent(5) shouldBe 50
            stats.topPercent(4) shouldBe 75
            stats.topPercent(0) shouldBe 100
            stats.averageScore.shouldNotBeNull() shouldBe 4.3
        }
    }
})
