package maeilkanji.maeilkanji.business.service

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.domain.MemberStatus
import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import maeilkanji.maeilkanji.business.repository.FeedbackRepository
import maeilkanji.maeilkanji.business.repository.MemberRepository
import maeilkanji.maeilkanji.business.service.dto.MemberSignupDto
import maeilkanji.maeilkanji.business.service.request.*
import maeilkanji.maeilkanji.business.service.response.*
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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

    @Transactional
    fun updateMemberToBounced(memberId: UUID) {
        val memberDto = memberRepository.findById(memberId)
        memberDto.memberStatus=MemberStatus.BOUNCED
        memberRepository.update(memberId, memberDto)
    }

    private fun createEmailVerificationCodeKey(email: String): String = "verify_code:$email"
    private fun createVerifiedEmailKey(email: String): String = "verified_email:$email"

}