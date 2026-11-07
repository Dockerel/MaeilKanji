package maeilkanji.maeilkanji.business.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.slot
import io.mockk.verify
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.domain.MemberStatus
import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import maeilkanji.maeilkanji.business.repository.FeedbackRepository
import maeilkanji.maeilkanji.business.repository.MemberRepository
import maeilkanji.maeilkanji.business.repository.StudyDayRepository
import maeilkanji.maeilkanji.business.service.dto.MemberSignupDto
import maeilkanji.maeilkanji.business.service.request.ChangeLevelServiceRequest
import maeilkanji.maeilkanji.business.service.request.FeedbackServiceRequest
import maeilkanji.maeilkanji.business.service.request.SendVerificationCodeServiceRequest
import maeilkanji.maeilkanji.business.service.request.SignupServiceRequest
import maeilkanji.maeilkanji.business.service.request.VerifyVerificationCodeServiceRequest
import maeilkanji.maeilkanji.fixture.memberDto
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.http.HttpStatus
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.TimeUnit

class MemberServiceTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerLeaf

    val redisTemplate = mockk<RedisTemplate<String, String>>()
    val valueOps = mockk<ValueOperations<String, String>>(relaxUnitFun = true)
    val mailService = mockk<MailService>(relaxUnitFun = true)
    val emailTemplateService = mockk<EmailTemplateService>()
    val memberRepository = mockk<MemberRepository>(relaxUnitFun = true)
    val feedbackRepository = mockk<FeedbackRepository>(relaxUnitFun = true)
    val studyDayRepository = mockk<StudyDayRepository>(relaxUnitFun = true)

    every { redisTemplate.opsForValue() } returns valueOps
    every { redisTemplate.delete(any<String>()) } returns true

    val memberService = MemberService(
        redisTemplate,
        mailService,
        emailTemplateService,
        memberRepository,
        feedbackRepository,
        studyDayRepository,
    )

    val email = "user@test.com"

    Given("활성 회원이 없는 이메일로 확인 코드 전송을 요청하면") {
        val codeSlot = slot<String>()
        every { memberRepository.existByEmailAndMemberStatus(email, MemberStatus.ACTIVE) } returns false
        every { valueOps.set(any(), capture(codeSlot), any<Long>(), any()) } just Runs
        every { emailTemplateService.generateEmailVerificationTemplate(any()) } returns "<html>code</html>"

        When("sendVerificationCode 를 호출하면") {
            val response = memberService.sendVerificationCode(SendVerificationCodeServiceRequest(email))

            Then("4자리 코드를 3분 TTL 로 Redis 에 저장하고 메일을 발송한다") {
                response.message shouldBe "확인 코드가 전송되었습니다."
                codeSlot.captured shouldHaveLength 4
                verify(exactly = 1) {
                    valueOps.set("verify_code:$email", codeSlot.captured, 3L, TimeUnit.MINUTES)
                    emailTemplateService.generateEmailVerificationTemplate(codeSlot.captured)
                    mailService.send(email, "MaeilKanji Email Verification Code", "<html>code</html>")
                }
            }
        }

        When("여러 번 호출하면") {
            val codes = mutableListOf<String>()
            every { valueOps.set(any(), capture(codes), any<Long>(), any()) } just Runs
            repeat(200) { memberService.sendVerificationCode(SendVerificationCodeServiceRequest(email)) }

            Then("모든 코드는 1000 이상 9999 이하이다") {
                codes shouldHaveSize 200
                codes.forEach { it.toInt() shouldBeInRange 1000..9999 }
            }
        }
    }

    Given("이미 활성 회원인 이메일로 확인 코드 전송을 요청하면") {
        every { memberRepository.existByEmailAndMemberStatus(email, MemberStatus.ACTIVE) } returns true

        When("sendVerificationCode 를 호출하면") {
            val exception = shouldThrow<MaeilKanjiException> {
                memberService.sendVerificationCode(SendVerificationCodeServiceRequest(email))
            }

            Then("400 예외가 발생하고 코드 저장과 메일 발송을 하지 않는다") {
                exception.status shouldBe HttpStatus.BAD_REQUEST
                exception.message shouldBe "Email already exists"
                verify(exactly = 0) {
                    valueOps.set(any(), any(), any<Long>(), any())
                    mailService.send(any(), any(), any())
                }
            }
        }
    }

    Given("Redis 에 확인 코드 1234 가 저장되어 있을 때") {
        every { valueOps.get("verify_code:$email") } returns "1234"

        When("같은 코드로 검증하면") {
            val response = memberService.verifyEmailVerificationCode(VerifyVerificationCodeServiceRequest(email, "1234"))

            Then("true 를 반환하고 인증 완료 플래그를 3분 TTL 로 저장한다") {
                response.verified shouldBe true
                verify(exactly = 1) { valueOps.set("verified_email:$email", email, 3L, TimeUnit.MINUTES) }
            }
        }

        When("다른 코드로 검증하면") {
            val response = memberService.verifyEmailVerificationCode(VerifyVerificationCodeServiceRequest(email, "1235"))

            Then("false 를 반환하고 인증 완료 플래그를 저장하지 않는다") {
                response.verified shouldBe false
                verify(exactly = 0) { valueOps.set("verified_email:$email", any(), any<Long>(), any()) }
            }
        }
    }

    Given("Redis 에 확인 코드가 없을 때 (만료)") {
        every { valueOps.get("verify_code:$email") } returns null

        When("코드를 검증하면") {
            val exception = shouldThrow<MaeilKanjiException> {
                memberService.verifyEmailVerificationCode(VerifyVerificationCodeServiceRequest(email, "1234"))
            }

            Then("400 예외가 발생한다") {
                exception.status shouldBe HttpStatus.BAD_REQUEST
                exception.message shouldBe "Email verification code not found"
            }
        }
    }

    Given("이메일 인증을 완료한 신규 사용자가") {
        every { valueOps.get("verified_email:$email") } returns email
        every { memberRepository.existByEmail(email) } returns false

        When("회원가입하면") {
            val response = memberService.signup(SignupServiceRequest(email, "INTERMEDIATE"))

            Then("인증 플래그를 삭제하고 새 회원을 저장한다") {
                response.message shouldBe "Successfully signed up."
                verify(exactly = 1) {
                    redisTemplate.delete("verified_email:$email")
                    memberRepository.save(MemberSignupDto(email, KanjiLevel.INTERMEDIATE))
                }
            }
        }
    }

    Given("이메일 인증을 완료한 탈퇴 이력이 있는 사용자가") {
        val existing = memberDto(email = email, memberStatus = MemberStatus.INACTIVE, level = KanjiLevel.BEGINNER)
        every { valueOps.get("verified_email:$email") } returns email
        every { memberRepository.existByEmail(email) } returns true
        every { memberRepository.findByEmail(email) } returns existing

        When("다른 레벨로 재가입하면") {
            memberService.signup(SignupServiceRequest(email, "ADVANCED"))

            Then("새로 저장하지 않고 기존 회원을 ACTIVE 와 새 레벨로 갱신한다") {
                verify(exactly = 1) {
                    memberRepository.update(email, match { it.memberStatus == MemberStatus.ACTIVE && it.level == KanjiLevel.ADVANCED })
                }
                verify(exactly = 0) { memberRepository.save(any()) }
            }
        }
    }

    Given("이메일 인증을 하지 않은 사용자가") {
        every { valueOps.get("verified_email:$email") } returns null

        When("회원가입하면") {
            val exception = shouldThrow<MaeilKanjiException> {
                memberService.signup(SignupServiceRequest(email, "BEGINNER"))
            }

            Then("403 예외가 발생하고 회원을 저장하지 않는다") {
                exception.status shouldBe HttpStatus.FORBIDDEN
                exception.message shouldBe "Email is not verified"
                verify(exactly = 0) { memberRepository.save(any()) }
            }
        }
    }

    Given("존재하지 않는 레벨 문자열로") {
        When("회원가입하면") {
            Then("IllegalArgumentException 이 발생하고 Redis 를 조회하지 않는다") {
                shouldThrow<IllegalArgumentException> {
                    memberService.signup(SignupServiceRequest(email, "EXPERT"))
                }
                verify(exactly = 0) { valueOps.get(any()) }
            }
        }
    }

    Given("존재하는 회원이") {
        val memberId = UUID.randomUUID()
        val member = memberDto(id = memberId, level = KanjiLevel.BEGINNER)
        every { memberRepository.findById(memberId) } returns member

        When("레벨 변경을 요청하면") {
            val response = memberService.changeLevel(ChangeLevelServiceRequest(memberId.toString(), "ADVANCED"))

            Then("새 레벨로 갱신한다") {
                response.message shouldBe "Successfully changed level."
                verify(exactly = 1) { memberRepository.update(memberId, match { it.level == KanjiLevel.ADVANCED }) }
            }
        }

        When("메일 수신 중단을 요청하면") {
            val response = memberService.stopMail(memberId.toString())

            Then("INACTIVE 로 갱신한다") {
                response.message shouldBe "Successfully stopped mail."
                verify(exactly = 1) { memberRepository.update(memberId, match { it.memberStatus == MemberStatus.INACTIVE }) }
            }
        }

        When("반송 처리되면") {
            memberService.updateMemberToBounced(memberId)

            Then("BOUNCED 로 갱신한다") {
                verify(exactly = 1) { memberRepository.update(memberId, match { it.memberStatus == MemberStatus.BOUNCED }) }
            }
        }
    }

    Given("어제 학습 페이지에 접속한 회원이") {
        // 2026-11-07 08:00 KST
        val fixedClock = Clock.fixed(Instant.parse("2026-11-06T23:00:00Z"), ZoneId.of("Asia/Seoul"))
        val service = MemberService(redisTemplate, mailService, emailTemplateService, memberRepository, feedbackRepository, studyDayRepository, fixedClock)
        val memberId = UUID.randomUUID()
        val yesterday = LocalDateTime.of(2026, 11, 6, 21, 0)
        every { memberRepository.findById(memberId) } returns memberDto(id = memberId, streak = 3, lastAccessedAt = yesterday, maxStreak = 3)
        every { studyDayRepository.count(memberId) } returnsMany listOf(5, 5, 6)

        When("오늘 접속을 기록하면") {
            val response = service.recordStudyAccess(memberId.toString())

            Then("스트릭을 1 올리고 마지막 접속 시각을 한국 시간 기준 현재로 갱신한다") {
                response.streak shouldBe 4
                verify(exactly = 1) {
                    memberRepository.update(memberId, match {
                        it.streak == 4 && it.lastAccessedAt == LocalDateTime.of(2026, 11, 7, 8, 0)
                    })
                }
            }

            Then("오늘을 학습일로 기록하고 최장 스트릭을 갱신한다") {
                response.maxStreak shouldBe 4
                verify(exactly = 1) { studyDayRepository.saveIfAbsent(memberId, LocalDate.of(2026, 11, 7)) }
                verify(exactly = 1) { memberRepository.update(memberId, match { it.maxStreak == 4 }) }
            }

            Then("새로 얻은 배지가 없으면 빈 목록이다") {
                response.newBadges shouldBe emptyList()
            }
        }
    }

    Given("6일 연속 학습한 회원이") {
        val fixedClock = Clock.fixed(Instant.parse("2026-11-06T23:00:00Z"), ZoneId.of("Asia/Seoul"))
        val service = MemberService(redisTemplate, mailService, emailTemplateService, memberRepository, feedbackRepository, studyDayRepository, fixedClock)
        val memberId = UUID.randomUUID()
        every { memberRepository.findById(memberId) } returns
            memberDto(id = memberId, streak = 6, lastAccessedAt = LocalDateTime.of(2026, 11, 6, 7, 0), maxStreak = 6)
        every { studyDayRepository.count(memberId) } returnsMany listOf(9, 9, 10)

        When("7일째 접속하면") {
            val response = service.recordStudyAccess(memberId.toString())

            Then("일주일 개근·잔디 심기 배지를 새로 얻는다") {
                response.newBadges.map { it.code } shouldBe listOf("STREAK_7", "DAYS_10")
            }
        }
    }

    Given("학습일 기록이 없던 기존 스트릭 회원이") {
        val fixedClock = Clock.fixed(Instant.parse("2026-11-06T23:00:00Z"), ZoneId.of("Asia/Seoul"))
        val service = MemberService(redisTemplate, mailService, emailTemplateService, memberRepository, feedbackRepository, studyDayRepository, fixedClock)
        val memberId = UUID.randomUUID()
        every { memberRepository.findById(memberId) } returns
            memberDto(id = memberId, streak = 3, lastAccessedAt = LocalDateTime.of(2026, 11, 6, 7, 0))
        every { studyDayRepository.count(memberId) } returnsMany listOf(0, 3, 4)

        When("접속하면") {
            val response = service.recordStudyAccess(memberId.toString())

            Then("마지막 접속일로 끝나는 스트릭 일수만큼 학습일을 채운다") {
                listOf(4, 5, 6).forEach { day ->
                    verify(exactly = 1) { studyDayRepository.saveIfAbsent(memberId, LocalDate.of(2026, 11, day)) }
                }
                response.maxStreak shouldBe 4
            }
        }
    }

    Given("UUID 형식이 아닌 memberId 로") {
        When("레벨 변경을 요청하면") {
            Then("IllegalArgumentException 이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    memberService.changeLevel(ChangeLevelServiceRequest("not-a-uuid", "BEGINNER"))
                }
            }
        }

        When("메일 수신 중단을 요청하면") {
            Then("IllegalArgumentException 이 발생하고 회원을 조회하지 않는다") {
                shouldThrow<IllegalArgumentException> { memberService.stopMail("not-a-uuid") }
                verify(exactly = 0) { memberRepository.findById(any()) }
            }
        }
    }

    Given("소문자 레벨 문자열로 (경계값)") {
        When("레벨 변경을 요청하면") {
            Then("IllegalArgumentException 이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    memberService.changeLevel(ChangeLevelServiceRequest(UUID.randomUUID().toString(), "beginner"))
                }
            }
        }
    }

    Given("피드백 요청이 들어오면") {
        val request = FeedbackServiceRequest("member-1", "TOO_HARD", "너무 어려워요")

        When("feedback 을 호출하면") {
            val response = memberService.feedback(request)

            Then("피드백을 저장한다") {
                response.message shouldBe "Successfully feedback."
                verify(exactly = 1) { feedbackRepository.save("member-1", "TOO_HARD", "너무 어려워요") }
            }
        }
    }
})
