package maeilkanji.maeilkanji.business.service

import maeilkanji.maeilkanji.business.domain.*
import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import maeilkanji.maeilkanji.business.repository.MemberRepository
import maeilkanji.maeilkanji.business.repository.QuizQuestionAnswerRepository
import maeilkanji.maeilkanji.business.repository.StudyDayRepository
import maeilkanji.maeilkanji.business.service.dto.BadgeStatusDto
import maeilkanji.maeilkanji.business.service.dto.MemberDto
import maeilkanji.maeilkanji.business.service.dto.ProfileDto
import maeilkanji.maeilkanji.business.service.dto.QuizSessionRecordDto
import maeilkanji.maeilkanji.business.service.request.UpdateProfileServiceRequest
import maeilkanji.maeilkanji.business.service.response.NicknameAvailabilityResponse
import maeilkanji.maeilkanji.business.service.response.UpdateProfileResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.math.roundToInt

private const val BIO_MAX_LENGTH = 100

@Service
@Transactional(readOnly = true)
class ProfileService(
    private val memberRepository: MemberRepository,
    private val studyDayRepository: StudyDayRepository,
    private val quizQuestionAnswerRepository: QuizQuestionAnswerRepository,
    private val rankingService: RankingService,
    private val clock: Clock = Clock.system(ZoneId.of("Asia/Seoul")),
) {

    fun getProfile(memberId: UUID): ProfileDto = buildProfile(memberRepository.findById(memberId))

    fun getPublicProfile(nickname: String): ProfileDto {
        val member = memberRepository.findByNickname(nickname)
            ?: throw MaeilKanjiException("존재하지 않는 프로필입니다.", HttpStatus.NOT_FOUND)
        return buildProfile(member)
    }

    /** 회원의 닉네임. 회원이 없거나 닉네임이 없으면 null */
    fun nicknameOf(memberId: UUID): String? =
        runCatching { memberRepository.findById(memberId) }.getOrNull()?.nickname

    /** 배지 판정용 누적 기록 */
    fun learningStats(memberId: UUID): LearningStats {
        val member = memberRepository.findById(memberId)
        return learningStats(member, quizQuestionAnswerRepository.findSessionsByMember(memberId))
    }

    fun checkNickname(memberId: UUID?, nickname: String): NicknameAvailabilityResponse {
        val trimmed = nickname.trim()
        if (!NicknamePolicy.isValid(trimmed)) return NicknameAvailabilityResponse(false, NicknamePolicy.RULE_MESSAGE)
        val owner = memberRepository.findByNickname(trimmed)
        if (owner != null && owner.id != memberId) return NicknameAvailabilityResponse(false, "이미 사용 중인 닉네임이에요.")
        return NicknameAvailabilityResponse(true, "사용할 수 있는 닉네임이에요.")
    }

    /**
     * 프로필 꾸미기 저장. 아바타·테마는 최장 스트릭으로 잠금이 풀린 것만, 대표 배지는 얻은 것만 고를 수 있다.
     */
    @Transactional
    fun updateProfile(memberId: UUID, request: UpdateProfileServiceRequest): UpdateProfileResponse {
        val member = memberRepository.findById(memberId)

        val nickname = request.nickname?.trim()?.takeIf { it.isNotEmpty() }
        if (nickname != null) {
            val check = checkNickname(memberId, nickname)
            if (!check.available) throw MaeilKanjiException(check.message, HttpStatus.BAD_REQUEST)
        }

        val bio = request.bio?.trim()?.takeIf { it.isNotEmpty() }
        if (bio != null && bio.length > BIO_MAX_LENGTH) {
            throw MaeilKanjiException("소개는 ${BIO_MAX_LENGTH}자까지 쓸 수 있어요.", HttpStatus.BAD_REQUEST)
        }

        val avatar = request.avatar?.let {
            ProfileAvatar.fromName(it) ?: throw MaeilKanjiException("알 수 없는 아바타입니다.", HttpStatus.BAD_REQUEST)
        }
        if (avatar != null && !avatar.isUnlocked(member.maxStreak)) {
            throw MaeilKanjiException("최장 스트릭 ${avatar.requiredMaxStreak}일을 달성하면 쓸 수 있어요.", HttpStatus.BAD_REQUEST)
        }

        val theme = request.theme?.let {
            ProfileTheme.fromName(it) ?: throw MaeilKanjiException("알 수 없는 테마입니다.", HttpStatus.BAD_REQUEST)
        }
        if (theme != null && !theme.isUnlocked(member.maxStreak)) {
            throw MaeilKanjiException("최장 스트릭 ${theme.requiredMaxStreak}일을 달성하면 쓸 수 있어요.", HttpStatus.BAD_REQUEST)
        }

        val featuredBadge = request.featuredBadge?.let {
            Badge.fromName(it) ?: throw MaeilKanjiException("알 수 없는 배지입니다.", HttpStatus.BAD_REQUEST)
        }
        if (featuredBadge != null && !featuredBadge.isEarned(learningStats(memberId))) {
            throw MaeilKanjiException("아직 얻지 않은 배지입니다.", HttpStatus.BAD_REQUEST)
        }

        member.nickname = nickname
        member.bio = bio
        member.avatar = avatar
        member.profileTheme = theme
        member.featuredBadge = featuredBadge
        memberRepository.update(memberId, member)

        val profileUrl = nickname?.let { "/u/" + URLEncoder.encode(it, StandardCharsets.UTF_8) } ?: "/me"
        return UpdateProfileResponse(nickname, profileUrl)
    }

    private fun buildProfile(member: MemberDto): ProfileDto {
        val now = LocalDateTime.now(clock)
        val today = now.toLocalDate()
        val sessions = quizQuestionAnswerRepository.findSessionsByMember(member.id)
        val stats = learningStats(member, sessions)

        val heatmapFrom = today.minusWeeks(StudyHeatmap.WEEK_COUNT.toLong())
        val heatmap = StudyHeatmap.of(activities(member.id, heatmapFrom, sessions), today)

        return ProfileDto(
            nickname = member.nickname,
            bio = member.bio,
            avatar = member.avatar ?: ProfileAvatar.SMILE,
            theme = member.profileTheme ?: ProfileTheme.DEFAULT,
            featuredBadge = member.featuredBadge,
            currentStreak = StreakPolicy.current(member.streak, member.lastAccessedAt, now),
            maxStreak = member.maxStreak,
            studyDays = stats.studyDays,
            quizSessions = stats.quizSessions,
            perfectSessions = stats.perfectSessions,
            averageQuizRate = sessions.takeIf { it.isNotEmpty() }?.let { s -> (s.map { it.ratio }.average() * 100).roundToInt() },
            heatmap = heatmap,
            badges = Badge.values().map { BadgeStatusDto(it, it.isEarned(stats)) },
            ranks = rankingService.ranksOf(member.id),
            nextTheme = ProfileTheme.nextLocked(member.maxStreak),
        )
    }

    private fun learningStats(member: MemberDto, sessions: List<QuizSessionRecordDto>) = LearningStats(
        maxStreak = member.maxStreak,
        studyDays = studyDayRepository.count(member.id),
        quizSessions = sessions.size,
        perfectSessions = sessions.count { it.perfect },
        levelsPlayed = sessions.mapTo(mutableSetOf()) { it.level },
    )

    private fun activities(memberId: UUID, from: LocalDate, sessions: List<QuizSessionRecordDto>): Map<LocalDate, DailyActivity> {
        val visits = studyDayRepository.findDates(memberId, from).associateWith { DailyActivity(visited = true) }
        val quizzes = sessions.filter { !it.solvedOn.isBefore(from) }
            .groupBy { it.solvedOn }
            .mapValues { (_, list) -> DailyActivity(visited = true, bestQuizRatio = list.maxOf { it.ratio }) }
        return (visits.keys + quizzes.keys).associateWith { date ->
            (visits[date] ?: DailyActivity()).merge(quizzes[date] ?: DailyActivity())
        }
    }
}
