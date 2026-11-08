package maeilkanji.maeilkanji.business.service

import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * 인증·로그인 코드처럼 사용자가 직접 요청하는 메일의 발송량을 제한한다.
 * Resend 무료 플랜은 하루 100건이라, 매일 아침 학습 메일 몫을 남겨 두기 위해 세 단계로 막는다.
 *
 * 1. 같은 주소로는 [cooldownSeconds]초에 한 번만 보낸다. (버튼 연타 방지)
 * 2. 같은 주소로는 하루 [perEmailDailyLimit]건까지만 보낸다.
 * 3. 이 종류([MailPurpose]) 전체로 하루 [globalDailyLimit]건까지만 보낸다. (여러 주소로 도배하는 경우 방지)
 *
 * 하루 기준은 KST 날짜이고, 실제 발송에 실패해도 사용한 몫은 돌려주지 않는다.
 */
@Component
class MailSendLimiter(
    private val redisTemplate: RedisTemplate<String, String>,

    @Value("\${mail.limit.cooldown-seconds:60}")
    private val cooldownSeconds: Long,
    @Value("\${mail.limit.per-email-daily:5}")
    private val perEmailDailyLimit: Long,
    @Value("\${mail.limit.global-daily:30}")
    private val globalDailyLimit: Long,

    private val clock: Clock = Clock.system(ZoneId.of("Asia/Seoul")),
) {

    fun acquire(purpose: MailPurpose, email: String) {
        val address = email.trim().lowercase()

        val cooldownKey = "mail_cooldown:${purpose.key}:$address"
        val acquired = redisTemplate.opsForValue()
            .setIfAbsent(cooldownKey, "1", cooldownSeconds, TimeUnit.SECONDS) == true
        if (!acquired) {
            val remaining = redisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS)?.takeIf { it > 0 } ?: cooldownSeconds
            throw MaeilKanjiException("${remaining}초 후에 다시 요청해 주세요.", HttpStatus.TOO_MANY_REQUESTS)
        }

        val today = LocalDate.now(clock)
        if (increaseDailyCount("mail_daily:${purpose.key}:$address:$today") > perEmailDailyLimit) {
            throw MaeilKanjiException(
                "오늘은 이 이메일로 더 이상 코드를 보낼 수 없어요. 내일 다시 시도해 주세요.",
                HttpStatus.TOO_MANY_REQUESTS
            )
        }
        if (increaseDailyCount("mail_daily_total:${purpose.key}:$today") > globalDailyLimit) {
            throw MaeilKanjiException(
                "오늘 발송 가능한 인증 메일이 모두 소진됐어요. 내일 다시 시도해 주세요.",
                HttpStatus.TOO_MANY_REQUESTS
            )
        }
    }

    /**
     * 코드 인증에 성공한 주소의 쿨다운·일일 카운트를 지워 바로 다시 요청할 수 있게 한다.
     * 전체 카운트는 실제로 나간 메일 수라 그대로 둔다.
     */
    fun reset(purpose: MailPurpose, email: String) {
        val address = email.trim().lowercase()
        val today = LocalDate.now(clock)
        redisTemplate.delete("mail_cooldown:${purpose.key}:$address")
        redisTemplate.delete("mail_daily:${purpose.key}:$address:$today")
    }

    private fun increaseDailyCount(key: String): Long {
        val count = redisTemplate.opsForValue().increment(key) ?: 1L
        if (count == 1L) {
            redisTemplate.expire(key, 1, TimeUnit.DAYS)
        }
        return count
    }
}

enum class MailPurpose(val key: String) {
    EMAIL_VERIFICATION("verify"),
    ADMIN_LOGIN("admin_login"),
}
