package maeilkanji.maeilkanji.business.service

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import maeilkanji.maeilkanji.business.repository.DailyContentIndexRepository

class DailyContentIndexServiceTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerLeaf

    val dailyContentIndexRepository = mockk<DailyContentIndexRepository>(relaxUnitFun = true)
    val dailyContentIndexService = DailyContentIndexService(dailyContentIndexRepository)

    Given("오늘의 콘텐츠 인덱스가 7 일 때") {
        every { dailyContentIndexRepository.getIndex() } returns 7L

        When("오늘 인덱스를 조회하면") {
            val index = dailyContentIndexService.getTodayDailyContentIndex()

            Then("7 을 반환한다") {
                index shouldBe 7L
            }
        }
    }

    Given("인덱스 갱신 시점에") {
        When("updateDailyContentIndex 를 호출하면") {
            dailyContentIndexService.updateDailyContentIndex()

            Then("저장소의 인덱스를 1회 갱신한다") {
                verify(exactly = 1) { dailyContentIndexRepository.updateIndex() }
            }
        }
    }
})
