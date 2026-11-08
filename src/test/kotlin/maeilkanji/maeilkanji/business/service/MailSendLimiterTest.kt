package maeilkanji.maeilkanji.business.service

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.http.HttpStatus
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class MailSendLimiterTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerLeaf

    val redisTemplate = mockk<RedisTemplate<String, String>>()
    val valueOps = mockk<ValueOperations<String, String>>()
    val clock = Clock.fixed(Instant.parse("2026-11-08T03:00:00Z"), ZoneId.of("Asia/Seoul"))
    val limiter = MailSendLimiter(redisTemplate, 60, 5, 30, clock)

    val email = "User@Test.com"
    val cooldownKey = "mail_cooldown:verify:user@test.com"
    val dailyKey = "mail_daily:verify:user@test.com:2026-11-08"
    val totalKey = "mail_daily_total:verify:2026-11-08"

    every { redisTemplate.opsForValue() } returns valueOps
    every { redisTemplate.expire(any<String>(), any<Long>(), any()) } returns true
    every { valueOps.setIfAbsent(cooldownKey, "1", 60L, TimeUnit.SECONDS) } returns true
    every { valueOps.increment(dailyKey) } returns 1L
    every { valueOps.increment(totalKey) } returns 1L

    Given("오늘 처음 요청하는 이메일이면") {
        When("acquire 를 호출하면") {
            Then("통과하고 일일 카운터에 1일 TTL 을 건다 (주소는 소문자로 정규화)") {
                shouldNotThrowAny { limiter.acquire(MailPurpose.EMAIL_VERIFICATION, email) }
                verify(exactly = 1) {
                    redisTemplate.expire(dailyKey, 1L, TimeUnit.DAYS)
                    redisTemplate.expire(totalKey, 1L, TimeUnit.DAYS)
                }
            }
        }
    }

    Given("쿨다운 중인 이메일이면") {
        every { valueOps.setIfAbsent(cooldownKey, "1", 60L, TimeUnit.SECONDS) } returns false
        every { redisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS) } returns 42L

        When("acquire 를 호출하면") {
            Then("남은 초를 알려주는 429 예외가 나고 일일 카운터는 올리지 않는다") {
                val e = shouldThrow<MaeilKanjiException> { limiter.acquire(MailPurpose.EMAIL_VERIFICATION, email) }
                e.status shouldBe HttpStatus.TOO_MANY_REQUESTS
                e.message shouldStartWith "42초"
                verify(exactly = 0) { valueOps.increment(any()) }
            }
        }
    }

    Given("같은 이메일로 오늘 이미 5건을 보냈으면") {
        every { valueOps.increment(dailyKey) } returns 6L

        When("acquire 를 호출하면") {
            Then("429 예외가 나고 전체 카운터는 올리지 않는다") {
                val e = shouldThrow<MaeilKanjiException> { limiter.acquire(MailPurpose.EMAIL_VERIFICATION, email) }
                e.status shouldBe HttpStatus.TOO_MANY_REQUESTS
                verify(exactly = 0) { valueOps.increment(totalKey) }
            }
        }
    }

    Given("오늘 전체 인증 메일이 30건을 넘었으면") {
        every { valueOps.increment(totalKey) } returns 31L

        When("acquire 를 호출하면") {
            Then("429 예외가 난다") {
                val e = shouldThrow<MaeilKanjiException> { limiter.acquire(MailPurpose.EMAIL_VERIFICATION, email) }
                e.status shouldBe HttpStatus.TOO_MANY_REQUESTS
            }
        }
    }

    Given("코드 인증에 성공한 주소면") {
        every { redisTemplate.delete(any<String>()) } returns true

        When("reset 을 호출하면") {
            limiter.reset(MailPurpose.EMAIL_VERIFICATION, email)

            Then("쿨다운과 주소별 일일 카운트만 지우고 전체 카운트는 남긴다") {
                verify(exactly = 1) {
                    redisTemplate.delete(cooldownKey)
                    redisTemplate.delete(dailyKey)
                }
                verify(exactly = 0) { redisTemplate.delete(totalKey) }
            }
        }
    }
})
