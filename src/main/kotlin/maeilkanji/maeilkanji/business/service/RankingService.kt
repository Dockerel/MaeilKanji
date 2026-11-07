package maeilkanji.maeilkanji.business.service

import maeilkanji.maeilkanji.business.domain.ProfileAvatar
import maeilkanji.maeilkanji.business.domain.RankingType
import maeilkanji.maeilkanji.business.domain.StreakPolicy
import maeilkanji.maeilkanji.business.repository.MemberRepository
import maeilkanji.maeilkanji.business.repository.StudyDayRepository
import maeilkanji.maeilkanji.business.service.dto.MemberDto
import maeilkanji.maeilkanji.business.service.dto.RankingDto
import maeilkanji.maeilkanji.business.service.dto.RankingEntryDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

/**
 * 스트릭 랭킹. 회원 수가 수백 명 규모라 전체를 불러와 메모리에서 정렬한다.
 * 같은 값이면 같은 순위 (1, 1, 3 ...). 값이 0 인 회원은 순위표에 넣지 않는다.
 */
@Service
@Transactional(readOnly = true)
class RankingService(
    private val memberRepository: MemberRepository,
    private val studyDayRepository: StudyDayRepository,
    private val clock: Clock = Clock.system(ZoneId.of("Asia/Seoul")),
) {

    fun getRanking(type: RankingType, viewerId: UUID?, limit: Int = DEFAULT_LIMIT): RankingDto {
        val ranked = rankAll(type)
        val entries = ranked.take(limit).map { (member, rank, value) -> toEntry(member, rank, value, viewerId) }
        val myEntry = viewerId?.let { id ->
            ranked.firstOrNull { it.member.id == id }?.let { toEntry(it.member, it.rank, it.value, viewerId) }
        }
        return RankingDto(type, entries, myEntry, ranked.size)
    }

    /** 회원의 랭킹 종류별 순위. 순위표 밖이면 null */
    fun ranksOf(memberId: UUID): Map<RankingType, Int?> =
        RankingType.values().associateWith { type -> rankAll(type).firstOrNull { it.member.id == memberId }?.rank }

    private data class Ranked(val member: MemberDto, val rank: Int, val value: Int)

    private fun rankAll(type: RankingType): List<Ranked> {
        val now = LocalDateTime.now(clock)
        val studyDays = if (type == RankingType.STUDY_DAYS) studyDayRepository.countAllByMember() else emptyMap()
        val valueOf: (MemberDto) -> Int = when (type) {
            RankingType.CURRENT_STREAK -> { m -> StreakPolicy.current(m.streak, m.lastAccessedAt, now) }
            RankingType.MAX_STREAK -> { m -> m.maxStreak }
            RankingType.STUDY_DAYS -> { m -> studyDays[m.id] ?: 0 }
        }

        val sorted = memberRepository.findAll()
            .map { it to valueOf(it) }
            .filter { it.second > 0 }
            // 같은 값이면 닉네임이 있는 회원을 먼저, 그다음 최근 접속 순으로 보여준다.
            .sortedWith(
                compareByDescending<Pair<MemberDto, Int>> { it.second }
                    .thenBy { it.first.nickname == null }
                    .thenByDescending { it.first.lastAccessedAt }
            )

        var rank = 0
        return sorted.mapIndexed { i, (member, value) ->
            if (i == 0 || sorted[i - 1].second != value) rank = i + 1
            Ranked(member, rank, value)
        }
    }

    private fun toEntry(member: MemberDto, rank: Int, value: Int, viewerId: UUID?) = RankingEntryDto(
        rank = rank,
        nickname = member.nickname,
        avatar = member.avatar ?: ProfileAvatar.SMILE,
        featuredBadge = member.featuredBadge,
        value = value,
        me = member.id == viewerId,
    )

    companion object {
        const val DEFAULT_LIMIT = 100
    }
}
