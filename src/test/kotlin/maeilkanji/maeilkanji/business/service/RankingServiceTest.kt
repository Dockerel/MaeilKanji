package maeilkanji.maeilkanji.business.service

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import maeilkanji.maeilkanji.business.domain.RankingType
import maeilkanji.maeilkanji.business.repository.MemberRepository
import maeilkanji.maeilkanji.business.repository.StudyDayRepository
import maeilkanji.maeilkanji.fixture.memberDto
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class RankingServiceTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerLeaf

    val memberRepository = mockk<MemberRepository>()
    val studyDayRepository = mockk<StudyDayRepository>()
    // 2026-11-07 08:00 KST
    val clock = Clock.fixed(Instant.parse("2026-11-06T23:00:00Z"), ZoneId.of("Asia/Seoul"))
    val rankingService = RankingService(memberRepository, studyDayRepository, clock)

    val today = LocalDateTime.of(2026, 11, 7, 7, 0)
    val yesterday = LocalDateTime.of(2026, 11, 6, 7, 0)
    val twoDaysAgo = LocalDateTime.of(2026, 11, 5, 7, 0)

    val alice = memberDto(id = UUID.randomUUID(), email = "a@test.com", nickname = "alice", streak = 10, maxStreak = 10, lastAccessedAt = today)
    val bob = memberDto(id = UUID.randomUUID(), email = "b@test.com", nickname = "bob", streak = 7, maxStreak = 20, lastAccessedAt = yesterday)
    val anon = memberDto(id = UUID.randomUUID(), email = "c@test.com", nickname = null, streak = 7, maxStreak = 7, lastAccessedAt = today)
    // 이틀 전이 마지막 접속: 현재 스트릭은 끊겼다.
    val broken = memberDto(id = UUID.randomUUID(), email = "d@test.com", nickname = "dan", streak = 30, maxStreak = 30, lastAccessedAt = twoDaysAgo)
    val newbie = memberDto(id = UUID.randomUUID(), email = "e@test.com")

    every { memberRepository.findAll() } returns listOf(newbie, broken, anon, bob, alice)
    every { studyDayRepository.countAllByMember() } returns mapOf(alice.id to 12, bob.id to 12, broken.id to 40)

    Given("현재 스트릭 랭킹은") {
        val ranking = rankingService.getRanking(RankingType.CURRENT_STREAK, bob.id)

        Then("끊긴 스트릭과 0 인 회원을 빼고, 같은 값은 같은 순위로 닉네임 있는 회원을 먼저 보여준다") {
            ranking.entries.map { it.displayName to it.rank } shouldBe listOf(
                "alice" to 1,
                "bob" to 2,
                "익명의 학습자" to 2,
            )
            ranking.totalRanked shouldBe 3
        }

        Then("보는 회원의 순위를 표시한다") {
            ranking.myEntry?.rank shouldBe 2
            ranking.entries.single { it.me }.nickname shouldBe "bob"
        }
    }

    Given("최장 스트릭 랭킹은") {
        val ranking = rankingService.getRanking(RankingType.MAX_STREAK, null)

        Then("최장 스트릭 순으로 정렬하고 회원이 아니면 내 순위가 없다") {
            ranking.entries.map { it.nickname } shouldBe listOf("dan", "bob", "alice", null)
            ranking.myEntry shouldBe null
        }
    }

    Given("누적 학습일 랭킹은") {
        val ranking = rankingService.getRanking(RankingType.STUDY_DAYS, newbie.id)

        Then("학습일 수로 정렬하고 기록이 없는 회원은 순위표 밖이다") {
            ranking.entries.map { it.nickname to it.value } shouldBe listOf("dan" to 40, "alice" to 12, "bob" to 12)
            ranking.entries.map { it.rank } shouldBe listOf(1, 2, 2)
            ranking.myEntry shouldBe null
        }
    }

    Given("limit 보다 많은 회원이 있으면") {
        val ranking = rankingService.getRanking(RankingType.MAX_STREAK, anon.id, limit = 2)

        Then("상위 limit 명만 보여주되 내 순위는 따로 계산한다") {
            ranking.entries.size shouldBe 2
            ranking.myEntry?.rank shouldBe 4
            ranking.totalRanked shouldBe 4
        }
    }

    Given("회원별 순위를 물으면") {
        Then("랭킹 종류별 순위를, 순위표 밖이면 null 을 준다") {
            rankingService.ranksOf(broken.id) shouldBe mapOf(
                RankingType.CURRENT_STREAK to null,
                RankingType.MAX_STREAK to 1,
                RankingType.STUDY_DAYS to 1,
            )
        }
    }
})
