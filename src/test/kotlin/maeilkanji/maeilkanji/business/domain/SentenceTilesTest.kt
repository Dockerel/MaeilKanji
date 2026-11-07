package maeilkanji.maeilkanji.business.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class SentenceTilesTest : BehaviorSpec({

    val sentence = "<ruby><rb>大学</rb><rt>だいがく</rt></ruby>の<ruby><rb>前</rb><rt>まえ</rt></ruby>に" +
        "<ruby><rb>小</rb><rt>ちい</rt></ruby>さい<ruby><rb>店</rb><rt>みせ</rt></ruby>があります。"

    Given("예문과 맞는 블록이 있을 때") {
        val tiles = "大学|の|前|に|小さい|店|が|あります|。"

        When("파싱하면") {
            val parsed = SentenceTiles.parse(sentence, tiles).shouldNotBeNull()

            Then("블록마다 루비를 살린 HTML 을 만든다") {
                parsed.map { it.text } shouldBe tiles.split("|")
                parsed[0].html shouldBe "<ruby>大学<rt>だいがく</rt></ruby>"
                parsed[4].html shouldBe "<ruby>小<rt>ちい</rt></ruby>さい"
                parsed[7].html shouldBe "あります"
                parsed[8].isPunctuation shouldBe true
            }
        }

        When("문장 배열용으로 6개 이하로 줄이면") {
            val merged = SentenceTiles.mergeForArrange(SentenceTiles.parse(sentence, tiles)!!, max = 6)

            Then("문장부호를 빼고 짧은 히라가나 블록을 앞에서부터 앞 블록에 붙인다") {
                merged shouldHaveSize 6
                // 앞에서부터 붙이다가 6개가 되면 멈춘다
                merged.map { it.text } shouldBe listOf("大学の", "前に", "小さい", "店", "が", "あります")
                merged.joinToString("") { it.text } shouldBe "大学の前に小さい店があります"
            }
        }

        When("이미 개수가 적으면") {
            val merged = SentenceTiles.mergeForArrange(SentenceTiles.parse(sentence, tiles)!!, max = 10)

            Then("문장부호만 뺀다") {
                merged.map { it.text } shouldBe listOf("大学", "の", "前", "に", "小さい", "店", "が", "あります")
            }
        }
    }

    Given("예문과 맞지 않는 블록이면") {
        Then("글자가 다르면 null") {
            SentenceTiles.parse(sentence, "大学|の|後|に|小さい|店|が|あります|。").shouldBeNull()
        }
        Then("루비 한자 묶음을 가르면 null (경계값)") {
            SentenceTiles.parse(sentence, "大|学|の|前|に|小さい|店|が|あります|。").shouldBeNull()
        }
        Then("빈 블록이 있으면 null") {
            SentenceTiles.parse(sentence, "大学||の|前|に|小さい|店|が|あります|。").shouldBeNull()
        }
        Then("블록이 없으면 null") {
            SentenceTiles.parse(sentence, null).shouldBeNull()
            SentenceTiles.parse(sentence, " ").shouldBeNull()
        }
    }

    Given("HTML 특수문자가 있는 예문이면") {
        Then("이스케이프한다") {
            SentenceTiles.html("A<b>&") shouldBe "A&lt;b&gt;&amp;"
        }
    }
})
