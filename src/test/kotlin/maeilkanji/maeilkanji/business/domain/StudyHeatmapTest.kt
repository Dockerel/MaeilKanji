package maeilkanji.maeilkanji.business.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.time.DayOfWeek
import java.time.LocalDate

class StudyHeatmapTest : BehaviorSpec({

    // 2026-11-07 은 토요일
    val today = LocalDate.of(2026, 11, 7)

    Given("하루 활동의 잔디 단계는") {
        Then("방문 1, 퀴즈 2, 60% 이상 3, 만점 4 이다") {
            DailyActivity().level shouldBe 0
            DailyActivity(visited = true).level shouldBe 1
            DailyActivity(visited = true, bestQuizRatio = 0.4).level shouldBe 2
            DailyActivity(visited = true, bestQuizRatio = 0.6).level shouldBe 3
            DailyActivity(visited = true, bestQuizRatio = 1.0).level shouldBe 4
        }

        Then("같은 날 활동을 합치면 더 높은 퀴즈 점수를 쓴다") {
            DailyActivity(visited = true, bestQuizRatio = 0.4).merge(DailyActivity(bestQuizRatio = 1.0)) shouldBe
                DailyActivity(visited = true, bestQuizRatio = 1.0)
        }
    }

    Given("최근 1년 잔디를 만들면") {
        val heatmap = StudyHeatmap.of(
            mapOf(
                today to DailyActivity(visited = true, bestQuizRatio = 1.0),
                today.minusDays(1) to DailyActivity(visited = true),
                today.plusDays(1) to DailyActivity(visited = true), // 미래 기록은 무시
            ),
            today,
        )

        Then("53주 × 7일이고 각 열은 일요일부터 시작한다") {
            heatmap.weeks shouldHaveSize 53
            heatmap.weeks.forEach { week ->
                week.days shouldHaveSize 7
                week.days.first().date.dayOfWeek shouldBe DayOfWeek.SUNDAY
            }
        }

        Then("마지막 열은 오늘이 들어 있는 주이고 오늘 칸을 표시한다") {
            val last = heatmap.weeks.last().days
            last.last().date shouldBe today
            last.last().today shouldBe true
            last.last().level shouldBe 4
            last[5].level shouldBe 1
        }

        Then("학습한 날 수를 센다") {
            heatmap.activeDays shouldBe 2
        }
    }

    Given("오늘이 일요일이면 (경계값)") {
        val sunday = LocalDate.of(2026, 11, 8)
        val heatmap = StudyHeatmap.of(emptyMap(), sunday)

        Then("마지막 열의 월~토는 미래 칸이다") {
            val last = heatmap.weeks.last().days
            last.first().date shouldBe sunday
            last.drop(1).all { it.future } shouldBe true
        }
    }

    Given("달 이름은") {
        val heatmap = StudyHeatmap.of(emptyMap(), today)

        Then("1일이 들어 있는 주에만 붙는다") {
            val labelled = heatmap.weeks.filter { it.monthLabel != null }
            labelled.forEach { week ->
                val first = week.days.firstOrNull { it.date.dayOfMonth == 1 }
                if (first != null) week.monthLabel shouldBe "${first.date.monthValue}월"
            }
            heatmap.weeks.last { it.days.any { d -> d.date == LocalDate.of(2026, 11, 1) } }.monthLabel shouldBe "11월"
        }
    }
})
