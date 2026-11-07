package maeilkanji.maeilkanji.business.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class NicknamePolicyTest : BehaviorSpec({

    Given("닉네임 규칙에서") {
        Then("한글·영문·숫자·밑줄 2~12자는 쓸 수 있다") {
            listOf("칸지", "kanji_king", "학습자01", "a_" + "b".repeat(10)).forEach { NicknamePolicy.isValid(it) shouldBe true }
        }

        Then("1자, 13자, 공백·특수문자·이모지는 쓸 수 없다 (경계값)") {
            listOf("칸", "가".repeat(13), "칸 지", "kanji!", "🙂🙂", "", "../admin").forEach {
                NicknamePolicy.isValid(it) shouldBe false
            }
        }
    }
})
