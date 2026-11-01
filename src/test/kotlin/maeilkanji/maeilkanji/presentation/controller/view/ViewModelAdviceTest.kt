package maeilkanji.maeilkanji.presentation.controller.view

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class ViewModelAdviceTest : BehaviorSpec({

    Given("UTC 기준 12월 31일이지만 한국 시간으로는 새해인 시점일 때") {
        val clock = Clock.fixed(Instant.parse("2026-12-31T15:00:00Z"), ZoneId.of("Asia/Seoul"))
        val advice = ViewModelAdvice(clock)

        When("footer 연도를 조회하면") {
            Then("한국 시간 기준 연도를 반환한다") {
                advice.currentYear() shouldBe 2027
            }
        }
    }

    Given("한국 시간으로 아직 12월 31일인 시점일 때") {
        val clock = Clock.fixed(Instant.parse("2026-12-31T14:59:59Z"), ZoneId.of("Asia/Seoul"))
        val advice = ViewModelAdvice(clock)

        When("footer 연도를 조회하면") {
            Then("올해 연도를 반환한다") {
                advice.currentYear() shouldBe 2026
            }
        }
    }

})
