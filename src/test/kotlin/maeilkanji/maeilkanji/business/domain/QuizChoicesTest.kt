package maeilkanji.maeilkanji.business.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import maeilkanji.maeilkanji.fixture.dailyContentDto
import maeilkanji.maeilkanji.fixture.multipleChoiceContentDto

class QuizChoicesTest : BehaviorSpec({

    Given("오답 보기 3개가 있는 콘텐츠로") {
        val content = multipleChoiceContentDto(KanjiLevel.BEGINNER, id = 7L)

        When("보기를 만들면") {
            val quiz = QuizChoices.of(content).shouldNotBeNull()

            Then("정답과 오답 3개를 섞은 4개 보기가 되고, 정답 위치가 정답을 가리킨다") {
                quiz.choices shouldContainExactlyInAnyOrder listOf("にち", "げつ", "ひ", "か")
                quiz.choices[quiz.answerIndex] shouldBe "にち"
                quiz.isCorrect(quiz.answerIndex) shouldBe true
                quiz.isCorrect((quiz.answerIndex + 1) % 4) shouldBe false
            }

            Then("같은 콘텐츠는 항상 같은 순서다 (메일과 채점이 일치)") {
                QuizChoices.of(content) shouldBe quiz
            }
        }

        When("보기 번호를 검증하면 (경계값)") {
            val quiz = QuizChoices.of(content).shouldNotBeNull()

            Then("0~3 만 유효하다") {
                quiz.isValidChoice(0) shouldBe true
                quiz.isValidChoice(3) shouldBe true
                quiz.isValidChoice(-1) shouldBe false
                quiz.isValidChoice(4) shouldBe false
            }
        }
    }

    Given("여러 콘텐츠의 보기를 만들면") {
        val answerIndexes = (1L..40L).map { QuizChoices.of(multipleChoiceContentDto(id = it))!!.answerIndex }.toSet()

        Then("정답 위치가 한 곳에 고정되지 않는다") {
            (answerIndexes.size > 1) shouldBe true
        }
    }

    Given("오답 보기가 비어 있는 콘텐츠로") {
        When("보기를 만들면") {
            Then("null 을 반환한다") {
                QuizChoices.of(dailyContentDto()).shouldBeNull()
                QuizChoices.of(dailyContentDto(wrongAnswer1 = "a", wrongAnswer2 = "b", wrongAnswer3 = " ")).shouldBeNull()
            }
        }
    }

    Given("id 가 없는 콘텐츠로") {
        Then("null 을 반환한다") {
            QuizChoices.of(multipleChoiceContentDto().copy(id = null)).shouldBeNull()
        }
    }
})
