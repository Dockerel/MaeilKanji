package maeilkanji.maeilkanji.business.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldMatch
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.domain.MemberStatus
import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import maeilkanji.maeilkanji.business.repository.DailyContentRepository
import maeilkanji.maeilkanji.business.repository.FeedbackRepository
import maeilkanji.maeilkanji.business.repository.MemberRepository
import maeilkanji.maeilkanji.business.service.dto.FeedbackDto
import maeilkanji.maeilkanji.business.service.request.SendAdminLoginCodeServiceRequest
import maeilkanji.maeilkanji.business.service.request.VerifyAdminLoginCodeServiceRequest
import maeilkanji.maeilkanji.fixture.dailyContentDto
import maeilkanji.maeilkanji.fixture.memberDto
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

class AdminServiceTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerLeaf

    val adminEmail = "admin@test.local"
    val redisTemplate = mockk<RedisTemplate<String, String>>()
    val valueOps = mockk<ValueOperations<String, String>>(relaxUnitFun = true)
    val dailyContentRepository = mockk<DailyContentRepository>(relaxUnitFun = true)
    val memberRepository = mockk<MemberRepository>(relaxUnitFun = true)
    val feedbackRepository = mockk<FeedbackRepository>()
    val mailService = mockk<MailService>(relaxUnitFun = true)
    val emailTemplateService = mockk<EmailTemplateService>()

    every { redisTemplate.opsForValue() } returns valueOps
    every { redisTemplate.delete(any<String>()) } returns true
    every { redisTemplate.expire(any<String>(), any<Long>(), any()) } returns true
    every { emailTemplateService.generateAdminLoginCodeTemplate(any()) } answers { "code:${firstArg<String>()}" }

    val adminService = AdminService(
        adminEmail,
        redisTemplate,
        mailService,
        emailTemplateService,
        dailyContentRepository,
        memberRepository,
        feedbackRepository,
    )

    val codeKey = "verify_admin_code:$adminEmail"
    val attemptKey = "verify_admin_attempts:$adminEmail"

    Given("관리자 이메일로") {
        val codeSlot = slot<String>()
        every { valueOps.set(any(), capture(codeSlot), any<Long>(), any()) } just Runs

        When("로그인 코드 전송을 요청하면") {
            val response = adminService.sendAdminLoginCode(SendAdminLoginCodeServiceRequest(adminEmail))

            Then("10자리 영숫자 코드를 3분 TTL 로 저장한다") {
                response.message shouldBe "관리자 페이지 로그인 코드가 이메일로 전송되었습니다."
                codeSlot.captured shouldMatch Regex("^[a-zA-Z0-9]{10}$")
                verify(exactly = 1) { valueOps.set(codeKey, codeSlot.captured, 3L, TimeUnit.MINUTES) }
            }

            Then("관리자 이메일로 코드를 발송하고 시도 횟수를 초기화한다") {
                verify(exactly = 1) { mailService.send(adminEmail, any(), "code:${codeSlot.captured}") }
                verify(exactly = 1) { redisTemplate.delete(attemptKey) }
            }
        }
    }

    Given("관리자가 아닌 이메일로") {
        When("로그인 코드 전송을 요청하면") {
            Then("401 예외가 발생하고 코드를 저장하지 않는다") {
                val exception = shouldThrow<MaeilKanjiException> {
                    adminService.sendAdminLoginCode(SendAdminLoginCodeServiceRequest("user@test.com"))
                }
                exception.status shouldBe HttpStatus.UNAUTHORIZED
                verify(exactly = 0) { valueOps.set(any(), any(), any<Long>(), any()) }
                verify(exactly = 0) { mailService.send(any(), any(), any()) }
            }
        }

        When("로그인 코드 검증을 요청하면") {
            Then("401 예외가 발생한다") {
                val exception = shouldThrow<MaeilKanjiException> {
                    adminService.verifyAdminLoginCode(VerifyAdminLoginCodeServiceRequest("user@test.com", "abc"))
                }
                exception.status shouldBe HttpStatus.UNAUTHORIZED
            }
        }
    }

    Given("대소문자만 다른 관리자 이메일로 (경계값)") {
        When("로그인 코드 전송을 요청하면") {
            Then("정확히 일치하지 않으므로 401 예외가 발생한다") {
                val exception = shouldThrow<MaeilKanjiException> {
                    adminService.sendAdminLoginCode(SendAdminLoginCodeServiceRequest("ADMIN@test.local"))
                }
                exception.status shouldBe HttpStatus.UNAUTHORIZED
            }
        }
    }

    Given("Redis 에 관리자 로그인 코드가 저장되어 있을 때") {
        every { valueOps.get(codeKey) } returns "Ab3dE5gH9k"
        every { valueOps.increment(attemptKey) } returns 1L

        When("같은 코드로 검증하면") {
            val sessionKeySlot = slot<String>()
            every { valueOps.set(capture(sessionKeySlot), adminEmail, 8L, TimeUnit.HOURS) } just Runs
            val token = adminService.verifyAdminLoginCode(VerifyAdminLoginCodeServiceRequest(adminEmail, "Ab3dE5gH9k"))

            Then("세션 토큰을 발급해 8시간 TTL 로 저장하고 코드와 시도 횟수를 삭제한다") {
                token.shouldNotBeNull()
                token shouldMatch Regex("^[A-Za-z0-9_-]{43}$")
                sessionKeySlot.captured shouldBe "admin_session:$token"
                verify(exactly = 1) {
                    redisTemplate.delete(codeKey)
                    redisTemplate.delete(attemptKey)
                }
            }
        }

        When("첫 시도이면") {
            adminService.verifyAdminLoginCode(VerifyAdminLoginCodeServiceRequest(adminEmail, "wrong"))

            Then("시도 횟수 키에 코드와 같은 3분 TTL 을 건다") {
                verify(exactly = 1) { redisTemplate.expire(attemptKey, 3L, TimeUnit.MINUTES) }
            }
        }

        When("대소문자가 다른 코드로 검증하면 (경계값)") {
            val token = adminService.verifyAdminLoginCode(VerifyAdminLoginCodeServiceRequest(adminEmail, "ab3de5gh9k"))

            Then("null 을 반환하고 코드를 삭제하지 않는다") {
                token.shouldBeNull()
                verify(exactly = 0) { redisTemplate.delete(any<String>()) }
            }
        }

        When("5번째 시도이면 (경계값)") {
            every { valueOps.increment(attemptKey) } returns 5L
            val token = adminService.verifyAdminLoginCode(VerifyAdminLoginCodeServiceRequest(adminEmail, "wrong"))

            Then("아직 검증을 허용한다") {
                token.shouldBeNull()
                verify(exactly = 0) { redisTemplate.delete(codeKey) }
            }
        }

        When("6번째 시도이면 (경계값)") {
            every { valueOps.increment(attemptKey) } returns 6L

            Then("맞는 코드여도 429 예외가 발생하고 코드를 폐기한다") {
                val exception = shouldThrow<MaeilKanjiException> {
                    adminService.verifyAdminLoginCode(VerifyAdminLoginCodeServiceRequest(adminEmail, "Ab3dE5gH9k"))
                }
                exception.status shouldBe HttpStatus.TOO_MANY_REQUESTS
                verify(exactly = 1) {
                    redisTemplate.delete(codeKey)
                    redisTemplate.delete(attemptKey)
                }
            }
        }
    }

    Given("관리자 세션을 확인할 때") {
        When("토큰이 없으면") {
            Then("Redis 를 조회하지 않고 false 를 반환한다") {
                adminService.isValidAdminSession(null) shouldBe false
                adminService.isValidAdminSession("") shouldBe false
                verify(exactly = 0) { redisTemplate.hasKey(any()) }
            }
        }

        When("Redis 에 세션이 있으면") {
            every { redisTemplate.hasKey("admin_session:tok") } returns true

            Then("true 를 반환한다") {
                adminService.isValidAdminSession("tok") shouldBe true
            }
        }

        When("Redis 에 세션이 없으면 (만료)") {
            every { redisTemplate.hasKey("admin_session:tok") } returns false

            Then("false 를 반환한다") {
                adminService.isValidAdminSession("tok") shouldBe false
            }
        }
    }

    Given("관리자가 로그아웃할 때") {
        When("토큰이 있으면") {
            adminService.logoutAdmin("tok")

            Then("세션을 삭제한다") {
                verify(exactly = 1) { redisTemplate.delete("admin_session:tok") }
            }
        }

        When("토큰이 없으면") {
            adminService.logoutAdmin(null)

            Then("아무것도 삭제하지 않는다") {
                verify(exactly = 0) { redisTemplate.delete(any<String>()) }
            }
        }
    }

    Given("Redis 에 관리자 로그인 코드가 없을 때 (만료)") {
        every { valueOps.get(codeKey) } returns null

        When("코드를 검증하면") {
            Then("400 예외가 발생한다") {
                val exception = shouldThrow<MaeilKanjiException> {
                    adminService.verifyAdminLoginCode(VerifyAdminLoginCodeServiceRequest(adminEmail, "Ab3dE5gH9k"))
                }
                exception.status shouldBe HttpStatus.BAD_REQUEST
                exception.message shouldBe "Code expired or not found"
            }
        }
    }

    Given("콘텐츠를 저장할 때") {
        When("BEGINNER 레벨이면") {
            val content = dailyContentDto(KanjiLevel.BEGINNER, id = null)
            every { dailyContentRepository.saveBeginner(content) } returns 10L
            val id = adminService.saveDailyContent(KanjiLevel.BEGINNER, content)

            Then("초급 저장소에 저장하고 id 를 반환한다") {
                id shouldBe 10L
                verify(exactly = 0) {
                    dailyContentRepository.saveIntermediate(any())
                    dailyContentRepository.saveAdvanced(any())
                }
            }
        }

        When("INTERMEDIATE 레벨이면") {
            val content = dailyContentDto(KanjiLevel.INTERMEDIATE, id = null)
            every { dailyContentRepository.saveIntermediate(content) } returns 20L
            val id = adminService.saveDailyContent(KanjiLevel.INTERMEDIATE, content)

            Then("중급 저장소에 저장하고 id 를 반환한다") {
                id shouldBe 20L
            }
        }

        When("ADVANCED 레벨이면") {
            val content = dailyContentDto(KanjiLevel.ADVANCED, id = null)
            every { dailyContentRepository.saveAdvanced(content) } returns 30L
            val id = adminService.saveDailyContent(KanjiLevel.ADVANCED, content)

            Then("고급 저장소에 저장하고 id 를 반환한다") {
                id shouldBe 30L
            }
        }
    }

    Given("레벨이 섞인 콘텐츠를 일괄 저장할 때") {
        When("초급·중급·고급이 한 번에 들어오면") {
            val beginner = dailyContentDto(KanjiLevel.BEGINNER, id = null)
            val intermediate = dailyContentDto(KanjiLevel.INTERMEDIATE, id = null)
            val advanced = dailyContentDto(KanjiLevel.ADVANCED, id = null)
            every { dailyContentRepository.saveBeginner(beginner) } returns 1L
            every { dailyContentRepository.saveIntermediate(intermediate) } returns 2L
            every { dailyContentRepository.saveAdvanced(advanced) } returns 3L
            val ids = adminService.saveDailyContents(listOf(beginner, intermediate, advanced))

            Then("각 레벨 저장소에 저장하고 입력 순서대로 id 를 반환한다") {
                ids shouldContainExactly listOf(1L, 2L, 3L)
            }
        }

        When("빈 목록이면 (경계값)") {
            val ids = adminService.saveDailyContents(emptyList())

            Then("아무것도 저장하지 않는다") {
                ids.shouldBeEmpty()
                verify(exactly = 0) {
                    dailyContentRepository.saveBeginner(any())
                    dailyContentRepository.saveIntermediate(any())
                    dailyContentRepository.saveAdvanced(any())
                }
            }
        }
    }

    Given("레벨별 콘텐츠 목록을 조회할 때") {
        val beginner = listOf(dailyContentDto(KanjiLevel.BEGINNER))
        every { dailyContentRepository.findAllBeginner() } returns beginner
        every { dailyContentRepository.findAllIntermediate() } returns emptyList()
        every { dailyContentRepository.findAllAdvanced() } returns emptyList()

        When("BEGINNER 를 조회하면") {
            val result = adminService.getDailyContents(KanjiLevel.BEGINNER)

            Then("초급 콘텐츠만 반환한다") {
                result shouldContainExactly beginner
            }
        }

        When("콘텐츠가 없는 레벨을 조회하면") {
            val result = adminService.getDailyContents(KanjiLevel.ADVANCED)

            Then("빈 목록을 반환한다") {
                result.shouldBeEmpty()
            }
        }
    }

    Given("콘텐츠를 삭제할 때") {
        When("INTERMEDIATE 레벨이면") {
            adminService.deleteDailyContent(KanjiLevel.INTERMEDIATE, 5L)

            Then("중급 저장소에서만 삭제한다") {
                verify(exactly = 1) { dailyContentRepository.deleteIntermediate(5L) }
                verify(exactly = 0) {
                    dailyContentRepository.deleteBeginner(any())
                    dailyContentRepository.deleteAdvanced(any())
                }
            }
        }
    }

    Given("비활성 회원이 있을 때") {
        val memberId = UUID.randomUUID()
        every { memberRepository.findById(memberId) } returns memberDto(id = memberId, memberStatus = MemberStatus.BOUNCED)

        When("재활성화하면") {
            adminService.reactivateMember(memberId)

            Then("ACTIVE 로 갱신한다") {
                verify(exactly = 1) { memberRepository.update(memberId, match { it.memberStatus == MemberStatus.ACTIVE }) }
            }
        }
    }

    Given("회원과 피드백 목록을 조회할 때") {
        val members = listOf(memberDto(), memberDto(email = "other@test.com"))
        val feedbacks = listOf(FeedbackDto(UUID.randomUUID(), "member-1", "TOO_HARD", "어려워요", LocalDateTime.now()))
        every { memberRepository.findAll() } returns members
        every { feedbackRepository.findAll() } returns feedbacks

        When("getMembers 를 호출하면") {
            val result = adminService.getMembers()

            Then("저장소의 회원 목록을 그대로 반환한다") {
                result shouldContainExactly members
            }
        }

        When("getFeedbacks 를 호출하면") {
            val result = adminService.getFeedbacks()

            Then("저장소의 피드백 목록을 그대로 반환한다") {
                result shouldContainExactly feedbacks
            }
        }
    }

    Given("랜덤 영숫자 코드를 생성할 때") {
        When("길이가 0 이면 (경계값)") {
            val code = adminService.generateRandomAlphaNumericCode(0)

            Then("빈 문자열을 반환한다") {
                code.shouldBeEmpty()
            }
        }

        When("길이가 1 이면 (경계값)") {
            val code = adminService.generateRandomAlphaNumericCode(1)

            Then("영숫자 1자를 반환한다") {
                code shouldMatch Regex("^[a-zA-Z0-9]$")
            }
        }

        When("길이가 10 이면") {
            val code = adminService.generateRandomAlphaNumericCode(10)

            Then("영숫자 10자를 반환한다") {
                code shouldMatch Regex("^[a-zA-Z0-9]{10}$")
            }
        }
    }
})
