package maeilkanji.maeilkanji.business.service

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import maeilkanji.maeilkanji.business.repository.DailyContentRepository
import maeilkanji.maeilkanji.business.service.dto.DailyContentDto
import maeilkanji.maeilkanji.business.service.request.SendAdminLoginCodeServiceRequest
import maeilkanji.maeilkanji.business.service.request.VerifyAdminLoginCodeServiceRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.TimeUnit

private const val ADMIN_CODE_LENGTH: Int = 10
private const val VALID_MINUTE: Long = 3

@Service
class AdminService(
    @Value("\${admin.email}")
    private val adminEmail: String,

    private val redisTemplate: RedisTemplate<String, String>,

    private val dailyContentRepository: DailyContentRepository,
) {

    fun sendAdminLoginCode(request: SendAdminLoginCodeServiceRequest): SendAdminLoginCodeResponse {
        if (request.email != adminEmail) {
            throw MaeilKanjiException("Unauthorized", HttpStatus.UNAUTHORIZED)
        }

        val randomCode = generateRandomAlphaNumericCode(ADMIN_CODE_LENGTH)

        val key = createAdminLoginCodeKey(request.email)

        redisTemplate.opsForValue().set(key, randomCode, VALID_MINUTE, TimeUnit.MINUTES)

        return SendAdminLoginCodeResponse("관리자 페이지 로그인 코드가 이메일로 전송되었습니다.")
    }

    fun verifyAdminLoginCode(request: VerifyAdminLoginCodeServiceRequest): Boolean {
        if (request.email != adminEmail) {
            throw MaeilKanjiException("Unauthorized", HttpStatus.UNAUTHORIZED)
        }

        val key = createAdminLoginCodeKey(request.email)
        val storedCode = redisTemplate.opsForValue().get(key)
            ?: throw MaeilKanjiException("Code expired or not found", HttpStatus.BAD_REQUEST)

        return if (storedCode == request.code) {
            redisTemplate.delete(key)
            true
        } else {
            false
        }
    }

    @Transactional
    fun saveDailyContent(level: KanjiLevel, content: DailyContentDto): Long {
        return when (level) {
            KanjiLevel.BEGINNER -> dailyContentRepository.saveBeginner(content)
            KanjiLevel.INTERMEDIATE -> dailyContentRepository.saveIntermediate(content)
            KanjiLevel.ADVANCED -> dailyContentRepository.saveAdvanced(content)
        }
    }

    fun generateRandomAlphaNumericCode(length: Int): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

        return (1..length)
            .map { charPool.random() } // 문자 풀에서 랜덤 문자 선택
            .joinToString("") // 문자들을 하나의 문자열로 결합
    }

    private fun createAdminLoginCodeKey(email: String): String = "verify_admin_code:$email"

}