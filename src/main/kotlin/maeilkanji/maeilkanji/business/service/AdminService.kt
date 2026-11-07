package maeilkanji.maeilkanji.business.service

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.domain.SentenceTiles
import maeilkanji.maeilkanji.business.domain.MemberStatus
import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import maeilkanji.maeilkanji.business.repository.DailyContentRepository
import maeilkanji.maeilkanji.business.repository.FeedbackRepository
import maeilkanji.maeilkanji.business.repository.MemberRepository
import maeilkanji.maeilkanji.business.service.dto.DailyContentDto
import maeilkanji.maeilkanji.business.service.dto.FeedbackDto
import maeilkanji.maeilkanji.business.service.dto.MemberDto
import maeilkanji.maeilkanji.business.service.request.SendAdminLoginCodeServiceRequest
import maeilkanji.maeilkanji.business.service.request.VerifyAdminLoginCodeServiceRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val ADMIN_CODE_LENGTH: Int = 10
private const val VALID_MINUTE: Long = 3
private const val MAX_VERIFY_ATTEMPTS: Long = 5
private const val ADMIN_SESSION_HOURS: Long = 8
private const val ADMIN_SESSION_TOKEN_BYTES: Int = 32
private const val ADMIN_LOGIN_CODE_TITLE = "[매일칸지] 관리자 로그인 코드"

@Service
class AdminService(
    @Value("\${admin.email}")
    private val adminEmail: String,

    private val redisTemplate: RedisTemplate<String, String>,

    private val mailService: MailService,
    private val emailTemplateService: EmailTemplateService,

    private val dailyContentRepository: DailyContentRepository,
    private val memberRepository: MemberRepository,
    private val feedbackRepository: FeedbackRepository,
) {

    private val secureRandom = SecureRandom()

    fun sendAdminLoginCode(request: SendAdminLoginCodeServiceRequest): SendAdminLoginCodeResponse {
        if (request.email != adminEmail) {
            throw MaeilKanjiException("Unauthorized", HttpStatus.UNAUTHORIZED)
        }

        val randomCode = generateRandomAlphaNumericCode(ADMIN_CODE_LENGTH)

        val key = createAdminLoginCodeKey(request.email)

        redisTemplate.opsForValue().set(key, randomCode, VALID_MINUTE, TimeUnit.MINUTES)
        // 새 코드를 발급하면 시도 횟수도 초기화
        redisTemplate.delete(createAdminLoginAttemptKey(request.email))

        mailService.send(adminEmail, ADMIN_LOGIN_CODE_TITLE, emailTemplateService.generateAdminLoginCodeTemplate(randomCode))

        return SendAdminLoginCodeResponse("관리자 페이지 로그인 코드가 이메일로 전송되었습니다.")
    }

    /**
     * 코드가 일치하면 관리자 세션 토큰을 발급해 반환하고, 일치하지 않으면 null 을 반환한다.
     * 코드 하나당 [MAX_VERIFY_ATTEMPTS]회까지 시도할 수 있고, 초과하면 코드를 폐기한다.
     */
    fun verifyAdminLoginCode(request: VerifyAdminLoginCodeServiceRequest): String? {
        if (request.email != adminEmail) {
            throw MaeilKanjiException("Unauthorized", HttpStatus.UNAUTHORIZED)
        }

        val key = createAdminLoginCodeKey(request.email)
        val attemptKey = createAdminLoginAttemptKey(request.email)
        val storedCode = redisTemplate.opsForValue().get(key)
            ?: throw MaeilKanjiException("Code expired or not found", HttpStatus.BAD_REQUEST)

        val attempts = redisTemplate.opsForValue().increment(attemptKey) ?: 1L
        if (attempts == 1L) {
            redisTemplate.expire(attemptKey, VALID_MINUTE, TimeUnit.MINUTES)
        }
        if (attempts > MAX_VERIFY_ATTEMPTS) {
            redisTemplate.delete(key)
            redisTemplate.delete(attemptKey)
            throw MaeilKanjiException("Too many attempts. Request a new code.", HttpStatus.TOO_MANY_REQUESTS)
        }

        if (!MessageDigest.isEqual(storedCode.toByteArray(), request.code.toByteArray())) {
            return null
        }

        redisTemplate.delete(key)
        redisTemplate.delete(attemptKey)
        return createAdminSession()
    }

    fun isValidAdminSession(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        return redisTemplate.hasKey(createAdminSessionKey(token)) == true
    }

    fun logoutAdmin(token: String?) {
        if (token.isNullOrBlank()) return
        redisTemplate.delete(createAdminSessionKey(token))
    }

    private fun createAdminSession(): String {
        val bytes = ByteArray(ADMIN_SESSION_TOKEN_BYTES).also { secureRandom.nextBytes(it) }
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        redisTemplate.opsForValue().set(createAdminSessionKey(token), adminEmail, ADMIN_SESSION_HOURS, TimeUnit.HOURS)
        return token
    }

    @Transactional
    fun saveDailyContent(level: KanjiLevel, content: DailyContentDto): Long {
        if (content.sentenceTiles != null && !SentenceTiles.isValid(content.sentence, content.sentenceTiles)) {
            throw MaeilKanjiException(
                "예문 블록이 예문과 맞지 않습니다. (${content.kanji1}/${content.kanji2}) 블록을 이어 붙이면 루비를 뺀 예문과 같아야 합니다.",
                HttpStatus.BAD_REQUEST
            )
        }
        return when (level) {
            KanjiLevel.BEGINNER -> dailyContentRepository.saveBeginner(content)
            KanjiLevel.INTERMEDIATE -> dailyContentRepository.saveIntermediate(content)
            KanjiLevel.ADVANCED -> dailyContentRepository.saveAdvanced(content)
        }
    }

    /**
     * 여러 레벨이 섞인 콘텐츠를 한 트랜잭션으로 저장한다. 하나라도 실패하면 전부 롤백된다.
     */
    @Transactional
    fun saveDailyContents(contents: List<DailyContentDto>): List<Long> {
        return contents.map { saveDailyContent(it.level, it) }
    }

    fun getDailyContents(level: KanjiLevel): List<DailyContentDto> {
        return when (level) {
            KanjiLevel.BEGINNER -> dailyContentRepository.findAllBeginner()
            KanjiLevel.INTERMEDIATE -> dailyContentRepository.findAllIntermediate()
            KanjiLevel.ADVANCED -> dailyContentRepository.findAllAdvanced()
        }
    }

    @Transactional
    fun deleteDailyContent(level: KanjiLevel, id: Long) {
        when (level) {
            KanjiLevel.BEGINNER -> dailyContentRepository.deleteBeginner(id)
            KanjiLevel.INTERMEDIATE -> dailyContentRepository.deleteIntermediate(id)
            KanjiLevel.ADVANCED -> dailyContentRepository.deleteAdvanced(id)
        }
    }

    fun getMembers(): List<MemberDto> {
        return memberRepository.findAll()
    }

    @Transactional
    fun reactivateMember(memberId: UUID) {
        val member = memberRepository.findById(memberId)
        member.memberStatus = MemberStatus.ACTIVE
        memberRepository.update(memberId, member)
    }

    fun getFeedbacks(): List<FeedbackDto> {
        return feedbackRepository.findAll()
    }

    fun generateRandomAlphaNumericCode(length: Int): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

        return (1..length)
            .map { charPool[secureRandom.nextInt(charPool.size)] } // 문자 풀에서 랜덤 문자 선택
            .joinToString("") // 문자들을 하나의 문자열로 결합
    }

    private fun createAdminLoginCodeKey(email: String): String = "verify_admin_code:$email"

    private fun createAdminLoginAttemptKey(email: String): String = "verify_admin_attempts:$email"

    private fun createAdminSessionKey(token: String): String = "admin_session:$token"

}