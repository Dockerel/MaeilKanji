package maeilkanji.maeilkanji.business.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class StreakPolicyTest : BehaviorSpec({

    val now = LocalDateTime.of(2026, 11, 7, 8, 0)

    Given("접속 이력이 없는 회원이") {
        When("처음 접속하면") {
            Then("스트릭은 1 이다") {
                StreakPolicy.next(0, null, now) shouldBe 1
            }
        }
    }

    Given("오늘 이미 접속한 회원이") {
        When("같은 날 다시 접속하면") {
            Then("스트릭을 유지한다") {
                StreakPolicy.next(5, now.minusHours(1), now) shouldBe 5
            }
        }
    }

    Given("어제 접속한 회원이") {
        When("오늘 접속하면") {
            Then("스트릭이 1 증가한다") {
                StreakPolicy.next(5, now.minusDays(1), now) shouldBe 6
            }
        }

        When("어제 이른 아침에 접속하고 오늘 밤에 접속하면 (24시간 초과, 경계값)") {
            Then("날짜 기준으로 하루 차이이므로 스트릭이 이어진다") {
                StreakPolicy.next(5, LocalDateTime.of(2026, 11, 6, 0, 1), LocalDateTime.of(2026, 11, 7, 23, 59)) shouldBe 6
            }
        }

        When("어제 자정 직전에 접속하고 오늘 자정 직후 접속하면 (경계값)") {
            Then("스트릭이 1 증가한다") {
                StreakPolicy.next(5, LocalDateTime.of(2026, 11, 6, 23, 59), LocalDateTime.of(2026, 11, 7, 0, 0)) shouldBe 6
            }
        }
    }

    Given("하루 이상 건너뛴 회원이") {
        When("접속하면") {
            Then("스트릭이 1 부터 다시 시작한다") {
                StreakPolicy.next(30, now.minusDays(2), now) shouldBe 1
            }
        }
    }
})
