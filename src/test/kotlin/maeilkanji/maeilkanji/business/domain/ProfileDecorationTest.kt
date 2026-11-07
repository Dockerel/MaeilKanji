package maeilkanji.maeilkanji.business.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ProfileDecorationTest : BehaviorSpec({

    Given("최장 스트릭이 0일인 회원은") {
        Then("기본 테마와 기본 아바타만 쓸 수 있다") {
            ProfileTheme.values().filter { it.isUnlocked(0) } shouldBe listOf(ProfileTheme.DEFAULT)
            ProfileAvatar.values().filter { it.isUnlocked(0) }.map { it.requiredMaxStreak }.toSet() shouldBe setOf(0)
        }

        Then("다음 테마는 벚꽃이다") {
            ProfileTheme.nextLocked(0) shouldBe ProfileTheme.SAKURA
        }
    }

    Given("최장 스트릭이 정확히 7일이면 (경계값)") {
        Then("말차 테마가 열리고 다음은 노을이다") {
            ProfileTheme.MATCHA.isUnlocked(7) shouldBe true
            ProfileTheme.MATCHA.isUnlocked(6) shouldBe false
            ProfileTheme.nextLocked(7) shouldBe ProfileTheme.SUNSET
        }
    }

    Given("모든 테마를 연 회원은") {
        Then("다음 테마가 없다") {
            ProfileTheme.nextLocked(365) shouldBe null
        }
    }
})
