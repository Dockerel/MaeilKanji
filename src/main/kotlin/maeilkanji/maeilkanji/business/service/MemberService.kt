package maeilkanji.maeilkanji.business.service

import maeilkanji.maeilkanji.business.domain.Badge
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.domain.LearningStats
import maeilkanji.maeilkanji.business.domain.MemberStatus
import maeilkanji.maeilkanji.business.domain.StreakPolicy
import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import maeilkanji.maeilkanji.business.repository.FeedbackRepository
import maeilkanji.maeilkanji.business.repository.MemberRepository
import maeilkanji.maeilkanji.business.repository.StudyDayRepository
import maeilkanji.maeilkanji.business.service.dto.MemberSignupDto
import maeilkanji.maeilkanji.business.service.request.*
import maeilkanji.maeilkanji.business.service.response.*
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.concurrent.TimeUnit

private const val VALID_MINUTE: Long = 3

private const val emailVerificationCodeTitle = "MaeilKanji Email Verification Code"

@Service
@Transactional(readOnly = true)
class MemberService(
    private val redisTemplate: RedisTemplate<String, String>,

    private val mailService: MailService,
    private val emailTemplateService: EmailTemplateService,

    private val memberRepository: MemberRepository,
    private val feedbackRepository: FeedbackRepository,
    private val studyDayRepository: StudyDayRepository,

    private val clock: Clock = Clock.system(ZoneId.of("Asia/Seoul")),
) {

    fun sendVerificationCode(request: SendVerificationCodeServiceRequest): SendVerificationCodeResponse {
        val email = request.email

        // 1. DB 중복 확인
        val existByEmail = memberRepository.existByEmailAndMemberStatus(email, MemberStatus.ACTIVE)
        if (existByEmail) {
            throw MaeilKanjiException("Email already exists", HttpStatus.BAD_REQUEST)
        }

        // 2. 랜덤 4자리 숫자 코드 생성
        val randomCode: String = (1000..9999).random().toString()

        // 3. Redis key 생성
        val key = createEmailVerificationCodeKey(email)

        // 4. Redis에 확인 코드 저장 및 TTL 설정
        redisTemplate.opsForValue().set(key, randomCode, VALID_MINUTE, TimeUnit.MINUTES)

        // 5. 확인 코드 메일 발송
        val content = emailTemplateService.generateEmailVerificationTemplate(randomCode)
        mailService.send(email, emailVerificationCodeTitle, content)

        return SendVerificationCodeResponse("확인 코드가 전송되었습니다.")
    }

    fun verifyEmailVerificationCode(request: VerifyVerificationCodeServiceRequest): VerifyEmailVerificationCodeResponse {
        val email = request.email
        val code = request.code

        val emailVerificationCodeKey = createEmailVerificationCodeKey(email)

        val dbEmailVerificationCode = redisTemplate.opsForValue().get(emailVerificationCodeKey)
            ?: throw MaeilKanjiException("Email verification code not found", HttpStatus.BAD_REQUEST)

        if (code != dbEmailVerificationCode) {
            return VerifyEmailVerificationCodeResponse(false)
        }

        val verifiedEmailKey = createVerifiedEmailKey(email)
        redisTemplate.opsForValue().set(verifiedEmailKey, email, VALID_MINUTE, TimeUnit.MINUTES)

        return VerifyEmailVerificationCodeResponse(true)
    }

    @Transactional
    fun signup(request: SignupServiceRequest): SignupResponse {
        val email = request.email
        val level = KanjiLevel.valueOf(request.level)

        // 1. 이메일 검증 플래그 확인
        val createVerifiedEmailKey = createVerifiedEmailKey(email)

        redisTemplate.opsForValue().get(createVerifiedEmailKey)
            ?: throw MaeilKanjiException("Email is not verified", HttpStatus.FORBIDDEN)
        redisTemplate.delete(createVerifiedEmailKey)

        // 2. 기존에 회원가입 했던 이력이 있는 유저인지 확인
        val existMember = memberRepository.existByEmail(email)
        if (existMember) {
            // 가입했던 유저는 활성 상태만 바꾸기
            val findMember = memberRepository.findByEmail(email)
            findMember.memberStatus = MemberStatus.ACTIVE
            findMember.level = level
            memberRepository.update(email, findMember)
        } else {
            // 새로운 유저 생성
            memberRepository.save(MemberSignupDto(email, level))
        }
        return SignupResponse("Successfully signed up.")
    }

    @Transactional
    fun changeLevel(request: ChangeLevelServiceRequest): ChangeLevelResponse {
        val memberIdString = request.memberId
        val level = KanjiLevel.valueOf(request.level)

        val memberId = UUID.fromString(memberIdString)
        val findMember = memberRepository.findById(memberId)

        findMember.level = level
        memberRepository.update(memberId, findMember)

        return ChangeLevelResponse("Successfully changed level.")
    }

    @Transactional
    fun stopMail(memberIdString: String): StopMailResponse {
        val memberId = UUID.fromString(memberIdString)
        val findMember = memberRepository.findById(memberId)

        findMember.memberStatus = MemberStatus.INACTIVE
        memberRepository.update(memberId, findMember)

        return StopMailResponse("Successfully stopped mail.")
    }

    @Transactional
    fun feedback(request: FeedbackServiceRequest): FeedbackResponse {
        feedbackRepository.save(request.memberId, request.reason, request.comment)
        return FeedbackResponse("Successfully feedback.")
    }

    /**
     * 학습 페이지 접속을 기록하고 갱신된 스트릭을 반환한다. 규칙은 [StreakPolicy] 참고.
     * 학습일(잔디)·최장 스트릭도 함께 갱신하고, 이번 접속으로 새로 얻은 배지를 알려준다.
     */
    @Transactional
    fun recordStudyAccess(memberIdString: String): StreakResponse {
        val memberId = UUID.fromString(memberIdString)
        val findMember = memberRepository.findById(memberId)
        val now = LocalDateTime.now(clock)

        backfillStudyDays(memberId, findMember.streak, findMember.lastAccessedAt)
        val before = LearningStats(maxStreak = findMember.maxStreak, studyDays = studyDayRepository.count(memberId))

        findMember.streak = StreakPolicy.next(findMember.streak, findMember.lastAccessedAt, now)
        findMember.lastAccessedAt = now
        findMember.maxStreak = maxOf(findMember.maxStreak, findMember.streak)
        memberRepository.update(memberId, findMember)
        studyDayRepository.saveIfAbsent(memberId, now.toLocalDate())

        val after = LearningStats(maxStreak = findMember.maxStreak, studyDays = studyDayRepository.count(memberId))
        val newBadges = Badge.newlyEarned(before, after).map { BadgeResponse.of(it) }
        return StreakResponse(findMember.streak, findMember.maxStreak, newBadges)
    }

    /**
     * 학습일 기록(study_day) 도입 전부터 스트릭이 있던 회원: 마지막 접속일로 끝나는 연속 [streak]일을 채워 둔다.
     * 스트릭 정의상 그 날들은 모두 학습한 날이다.
     */
    private fun backfillStudyDays(memberId: UUID, streak: Int, lastAccessedAt: LocalDateTime?) {
        if (lastAccessedAt == null || streak <= 0) return
        if (studyDayRepository.count(memberId) > 0) return
        val lastDay = lastAccessedAt.toLocalDate()
        (0 until streak).forEach { studyDayRepository.saveIfAbsent(memberId, lastDay.minusDays(it.toLong())) }
    }

    @Transactional
    fun updateMemberToBounced(memberId: UUID) {
        val memberDto = memberRepository.findById(memberId)
        memberDto.memberStatus=MemberStatus.BOUNCED
        memberRepository.update(memberId, memberDto)
    }

    private fun createEmailVerificationCodeKey(email: String): String = "verify_code:$email"
    private fun createVerifiedEmailKey(email: String): String = "verified_email:$email"

}