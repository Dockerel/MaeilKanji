package maeilkanji.maeilkanji.business.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
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

    every { redisTemplate.opsForValue() } returns valueOps
    every { redisTemplate.delete(any<String>()) } returns true

    val adminService = AdminService(
        adminEmail,
        redisTemplate,
        dailyContentRepository,
        memberRepository,
        feedbackRepository,
    )

    val codeKey = "verify_admin_code:$adminEmail"

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

        When("같은 코드로 검증하면") {
            val result = adminService.verifyAdminLoginCode(VerifyAdminLoginCodeServiceRequest(adminEmail, "Ab3dE5gH9k"))

            Then("true 를 반환하고 코드를 삭제한다") {
                result shouldBe true
                verify(exactly = 1) { redisTemplate.delete(codeKey) }
            }
        }

        When("대소문자가 다른 코드로 검증하면 (경계값)") {
            val result = adminService.verifyAdminLoginCode(VerifyAdminLoginCodeServiceRequest(adminEmail, "ab3de5gh9k"))

            Then("false 를 반환하고 코드를 삭제하지 않는다") {
                result shouldBe false
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
