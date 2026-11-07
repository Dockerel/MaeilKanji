package maeilkanji.maeilkanji.business.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.string.shouldContain
import maeilkanji.maeilkanji.fixture.dailyContentDto
import maeilkanji.maeilkanji.fixture.validGeneratedContent

class GeneratedContentValidatorTest : BehaviorSpec({

    val level = KanjiLevel.INTERMEDIATE

    Given("규칙을 모두 지킨 문제") {
        val content = validGeneratedContent()

        Then("검증 오류가 없다") {
            GeneratedContentValidator.validate(level, content, emptyList()).shouldBeEmpty()
        }

        When("같은 한자 쌍을 다른 레벨에서 이미 썼으면") {
            val existing = listOf(dailyContentDto(level = KanjiLevel.ADVANCED).copy(kanji1 = "減", kanji2 = "増"))
            Then("중복 오류가 난다") {
                GeneratedContentValidator.validate(level, content, existing).joinToString() shouldContain "이미 사용"
            }
        }

        When("같은 레벨에서 kanji1 을 이미 썼으면") {
            val existing = listOf(dailyContentDto(level = level).copy(kanji1 = "増", kanji2 = "月"))
            Then("중복 오류가 난다") {
                GeneratedContentValidator.validate(level, content, existing).joinToString() shouldContain "kanji1(増)은 이미"
            }
        }
    }

    Given("규칙을 어긴 문제") {
        Then("예문 블록이 예문과 다르면 오류") {
            val content = validGeneratedContent().copy(sentenceTiles = "都市|では|人口|が|増え|。")
            GeneratedContentValidator.validate(level, content, emptyList()).joinToString() shouldContain "sentenceTiles"
        }
        Then("answer 가 word1 과 다르면 오류") {
            val content = validGeneratedContent().copy(answer = "ふえる (増える)")
            GeneratedContentValidator.validate(level, content, emptyList()).joinToString() shouldContain "answer"
        }
        Then("오답 보기가 정답과 같으면 오류") {
            val content = validGeneratedContent().copy(wrongAnswer2 = "じんこうぞうか (人口増加)")
            GeneratedContentValidator.validate(level, content, emptyList()).joinToString() shouldContain "오답 보기"
        }
        Then("word3 에 kanji2 가 없으면 오류") {
            val content = validGeneratedContent().copy(word3 = "人口(じんこう)")
            GeneratedContentValidator.validate(level, content, emptyList()).joinToString() shouldContain "word3"
        }
        Then("값에 ; 가 있으면 오류") {
            val content = validGeneratedContent().copy(meaning1 = "늘다;증가")
            GeneratedContentValidator.validate(level, content, emptyList()).shouldNotBeEmpty()
        }
        Then("explanation 형식이 다르면 오류") {
            val content = validGeneratedContent().copy(explanation = "인구 증가입니다.")
            GeneratedContentValidator.validate(level, content, emptyList()).joinToString() shouldContain "explanation"
        }
    }
})
