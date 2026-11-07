package maeilkanji.maeilkanji.business.service

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.repository.DailyContentRepository
import maeilkanji.maeilkanji.business.service.dto.LessonNeighbors
import maeilkanji.maeilkanji.fixture.dailyContentDto
import maeilkanji.maeilkanji.fixture.validGeneratedContent

class KanjiLibraryServiceTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerLeaf

    val repository = mockk<DailyContentRepository>()
    val service = KanjiLibraryService(repository)

    // 증감(増/減) 중급 학습 하나와, 다른 쌍이지만 예문에 増 이 들어간 고급 학습 하나
    val intermediate = validGeneratedContent(KanjiLevel.INTERMEDIATE).copy(id = 5L)
    val advanced = validGeneratedContent(KanjiLevel.ADVANCED).copy(
        id = 9L, kanji1 = "栄", kanji2 = "衰", yomi1 = "(えい)", meaning1 = "번영하다",
        word1 = "繁栄(はんえい)", word2 = "栄える(さかえる)", word3 = "衰退(すいたい)", word4 = "増税(ぞうぜい)",
        sentence = "<ruby><rb>繁栄</rb><rt>はんえい</rt></ruby>が<ruby><rb>衰退</rb><rt>すいたい</rt></ruby>した。",
    )
    every { repository.findAllBeginner() } returns listOf(
        dailyContentDto(KanjiLevel.BEGINNER, id = 3L),
        dailyContentDto(KanjiLevel.BEGINNER, id = 1L),
        dailyContentDto(KanjiLevel.BEGINNER, id = 2L),
    )
    every { repository.findAllIntermediate() } returns listOf(intermediate)
    every { repository.findAllAdvanced() } returns listOf(advanced)

    Given("학습 목록은") {
        Then("id 순으로 정렬한다") {
            service.lessons(KanjiLevel.BEGINNER).map { it.id } shouldContainExactly listOf(1L, 2L, 3L)
        }
    }

    Given("앞뒤 학습을 찾을 때") {
        Then("가운데면 양쪽, 끝이면 한쪽만 있다") {
            service.neighbors(KanjiLevel.BEGINNER, 2L) shouldBe LessonNeighbors(1L, 3L)
            service.neighbors(KanjiLevel.BEGINNER, 1L) shouldBe LessonNeighbors(null, 2L)
            service.neighbors(KanjiLevel.BEGINNER, 3L) shouldBe LessonNeighbors(2L, null)
        }
    }

    Given("한자 페이지를 만들 때") {
        val entry = service.kanji("増").shouldNotBeNull()

        Then("읽기·뜻·짝 한자는 그 한자를 배운 학습에서 가져온다") {
            entry.readings shouldContainExactly listOf("(ぞう, ふ)")
            entry.meanings shouldContainExactly listOf("늘다, 증가하다")
            entry.pairedKanji shouldContainExactly listOf("減")
            entry.lessons.map { it.id } shouldContainExactly listOf(5L)
        }

        Then("단어는 다른 학습에 나온 것까지 한자 부분 기준으로 모은다") {
            entry.words.map { it.word } shouldContainExactly listOf("人口増加(じんこうぞうか)", "増える(ふえる)", "増税(ぞうぜい)")
        }

        Then("예문은 루비를 뺀 문장에 한자가 있는 학습만 모은다") {
            entry.sentences.map { it.lessonId } shouldContainExactly listOf(5L)
        }
    }

    Given("학습 한자로 나온 적 없는 한자는") {
        Then("null 이다") {
            service.kanji("猫").shouldBeNull()
        }
    }

    Given("sitemap 용 한자 목록은") {
        Then("kanji1·kanji2 를 중복 없이 모은다") {
            service.allKanji() shouldContainExactly listOf("増", "日", "月", "栄", "衰", "減").sorted()
        }
    }
})
