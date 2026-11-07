package maeilkanji.maeilkanji.business.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import maeilkanji.maeilkanji.business.repository.DailyContentRepository
import maeilkanji.maeilkanji.fixture.dailyContentDto
import maeilkanji.maeilkanji.fixture.validGeneratedContent

class DailyContentGenerationServiceTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerLeaf

    val generator = mockk<DailyContentGenerator>()
    val repository = mockk<DailyContentRepository>()
    val service = DailyContentGenerationService(generator, repository)

    every { repository.findAllBeginner() } returns listOf(dailyContentDto(level = KanjiLevel.BEGINNER))
    every { repository.findAllIntermediate() } returns emptyList()
    every { repository.findAllAdvanced() } returns emptyList()
    every { repository.saveIntermediate(any()) } returns 42L

    val level = KanjiLevel.INTERMEDIATE

    Given("첫 시도에 올바른 문제가 나오면") {
        every { generator.generate(level, any(), any()) } returns validGeneratedContent()

        When("generateAndSave") {
            val id = service.generateAndSave(level)

            Then("한 번만 생성하고 해당 레벨 테이블에 저장한다") {
                id shouldBe 42L
                verify(exactly = 1) { generator.generate(level, listOf("日/月"), null) }
                verify(exactly = 1) { repository.saveIntermediate(match { it.kanji1 == "増" && it.id == null }) }
            }
        }
    }

    Given("첫 시도가 검증에 실패하면") {
        val errors = mutableListOf<String?>()
        every { generator.generate(level, any(), captureNullable(errors)) } returnsMany listOf(
            validGeneratedContent().copy(answer = "틀린 답"),
            validGeneratedContent(),
        )

        When("generateAndSave") {
            service.generateAndSave(level)

            Then("실패 사유를 넘겨 다시 만들고 저장한다") {
                errors[0].shouldBeNull()
                errors[1].shouldNotBeNull() shouldContain "answer"
                verify(exactly = 1) { repository.saveIntermediate(any()) }
            }
        }
    }

    Given("API 호출이 계속 실패하면") {
        every { generator.generate(level, any(), any()) } throws IllegalStateException("boom")

        Then("최대 횟수만큼 시도한 뒤 예외를 던지고 저장하지 않는다") {
            shouldThrow<MaeilKanjiException> { service.generateAndSave(level) }
            verify(exactly = MAX_GENERATION_ATTEMPTS) { generator.generate(level, any(), any()) }
            verify(exactly = 0) { repository.saveIntermediate(any()) }
        }
    }
})
