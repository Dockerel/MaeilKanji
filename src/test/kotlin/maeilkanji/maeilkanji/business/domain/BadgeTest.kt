package maeilkanji.maeilkanji.business.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class BadgeTest : BehaviorSpec({

    Given("기록이 없는 회원은") {
        Then("배지가 없다") {
            Badge.earned(LearningStats()).shouldBeEmpty()
        }
    }

    Given("최장 스트릭이 정확히 7일이면 (경계값)") {
        val stats = LearningStats(maxStreak = 7)

        Then("3일·7일 배지를 얻고 14일 배지는 아직이다") {
            Badge.earned(stats) shouldContainExactly listOf(Badge.STREAK_3, Badge.STREAK_7)
        }
    }

    Given("세 레벨 퀴즈를 모두 푼 회원은") {
        val stats = LearningStats(quizSessions = 3, levelsPlayed = KanjiLevel.values().toSet())

        Then("레벨 탐험가 배지를 얻는다") {
            Badge.ALL_LEVELS.isEarned(stats) shouldBe true
            Badge.ALL_LEVELS.isEarned(stats.copy(levelsPlayed = setOf(KanjiLevel.BEGINNER))) shouldBe false
        }
    }

    Given("기록이 바뀌면") {
        val before = LearningStats(maxStreak = 2, studyDays = 9, quizSessions = 9, perfectSessions = 0)
        val after = LearningStats(maxStreak = 3, studyDays = 10, quizSessions = 10, perfectSessions = 1)

        Then("새로 조건을 만족한 배지만 enum 순서대로 돌려준다") {
            Badge.newlyEarned(before, after) shouldContainExactly
                listOf(Badge.STREAK_3, Badge.DAYS_10, Badge.QUIZ_10, Badge.PERFECT_1)
        }

        Then("기록이 그대로면 새 배지가 없다") {
            Badge.newlyEarned(after, after).shouldBeEmpty()
        }
    }

    Given("enum 이름으로 찾을 때") {
        Then("없는 이름이나 null 은 null 이다") {
            Badge.fromName("STREAK_3") shouldBe Badge.STREAK_3
            Badge.fromName("NOPE") shouldBe null
            Badge.fromName(null) shouldBe null
        }
    }
})
