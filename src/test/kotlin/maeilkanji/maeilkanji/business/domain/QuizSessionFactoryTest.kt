package maeilkanji.maeilkanji.business.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import maeilkanji.maeilkanji.business.service.dto.DailyContentDto
import maeilkanji.maeilkanji.fixture.quizContentDto

class QuizSessionFactoryTest : BehaviorSpec({

    val pool = (1L..10L).map { quizContentDto(it) }

    Given("5번 콘텐츠로") {
        val today = pool.first { it.id == 5L }

        When("세션을 만들면") {
            val session = QuizSessionFactory.create(today, pool).shouldNotBeNull()
            val q = session.questions

            Then("단어(한→일), 뜻(일→한), 빈칸 2개, 문장 만들기 순서의 5문제다") {
                q.map { it.type } shouldBe listOf(
                    QuizQuestionType.WORD_TO_JAPANESE,
                    QuizQuestionType.WORD_TO_KOREAN,
                    QuizQuestionType.FILL_BLANK,
                    QuizQuestionType.FILL_BLANK,
                    QuizQuestionType.ARRANGE,
                )
            }

            Then("1번은 메일 보기와 같은 순서·정답이다") {
                val choices = QuizChoices.of(today)!!
                q[0].options.map { it.text } shouldBe choices.choices
                q[0].answerIndex shouldBe choices.answerIndex
            }

            Then("2번은 word3 의 뜻을 묻고 보기 4개가 서로 다르다") {
                q[1].options.map { it.text }.toSet() shouldHaveSize 4
                q[1].options[q[1].answerIndex!!].text shouldBe today.wordMeaning3
                q[1].promptHtml shouldBe "<ruby>友達<rt>ともだち</rt></ruby>"
            }

            Then("빈칸 문제는 어제·그저께 예문에서 kanji1 블록을 비운다") {
                q[2].hint!! shouldContain "어제 배운 예문"
                q[2].options[q[2].answerIndex!!].text shouldBe "木曜日" // 4번 콘텐츠
                q[3].hint!! shouldContain "그저께 배운 예문"
                q[3].options[q[3].answerIndex!!].text shouldBe "水曜日" // 3번 콘텐츠
                q[2].promptHtml!! shouldContain "class=\"blank\""
                q[2].promptHtml!! shouldNotContain "木曜日"
                q[2].options.map { it.text }.toSet() shouldHaveSize 4
            }

            Then("문장 만들기는 오늘 예문 블록(문장부호 제외) + 함정 블록 2개다") {
                val arrange = q[4]
                arrange.answerTexts shouldBe listOf("金曜日", "に", "友達", "と", "会いました")
                arrange.options shouldHaveSize 7
                arrange.options.map { it.text } shouldContainAll arrange.answerTexts!!
                arrange.hint shouldBe today.sentenceMeaning
            }

            Then("같은 콘텐츠는 항상 같은 세션이다") {
                QuizSessionFactory.create(today, pool) shouldBe session
            }
        }
    }

    Given("1번 콘텐츠처럼 어제·그저께가 없으면") {
        val session = QuizSessionFactory.create(pool.first(), pool).shouldNotBeNull()

        Then("id 가 큰 지난 콘텐츠로 빈칸 문제를 만든다") {
            session.questions[2].hint!! shouldContain "지난 예문"
            session.questions[2].options[session.questions[2].answerIndex!!].text shouldBe "秋曜日" // 10번
        }
    }

    Given("세션 채점에서") {
        val session = QuizSessionFactory.create(pool[4], pool)!!
        val q = session.questions
        val arrange = q[4]
        val arrangeAnswer = arrange.answerTexts!!.map { text -> arrange.options.indexOfFirst { it.text == text } }
        val perfect = listOf(listOf(q[0].answerIndex!!), listOf(q[1].answerIndex!!), listOf(q[2].answerIndex!!), listOf(q[3].answerIndex!!), arrangeAnswer)

        Then("모두 맞히면 전부 true") {
            session.grade(perfect) shouldBe List(5) { true }
        }

        Then("문장 블록 순서가 틀리거나 일부만 고르면 false") {
            session.grade(perfect.dropLast(1) + listOf(arrangeAnswer.reversed()))!!.last() shouldBe false
            session.grade(perfect.dropLast(1) + listOf(arrangeAnswer.dropLast(1)))!!.last() shouldBe false
        }

        Then("같은 블록을 두 번 고르거나 범위를 벗어나면 false (경계값)") {
            session.grade(perfect.dropLast(1) + listOf(arrangeAnswer + arrangeAnswer.first()))!!.last() shouldBe false
            session.grade(perfect.dropLast(1) + listOf(listOf(99)))!!.last() shouldBe false
        }

        Then("4지선다에 답을 2개 보내면 false") {
            session.grade(listOf(listOf(0, 1)) + perfect.drop(1))!!.first() shouldBe false
        }

        Then("답 개수가 다르면 null") {
            session.grade(perfect.dropLast(1)).shouldBeNull()
        }
    }

    Given("오답 보기나 예문 블록이 없는 콘텐츠면") {
        Then("세션을 만들지 않는다") {
            QuizSessionFactory.create(pool[4].copy(wrongAnswer1 = null), pool).shouldBeNull()
            QuizSessionFactory.create(pool[4].copy(sentenceTiles = null), pool).shouldBeNull()
        }
    }

    Given("실제 콘텐츠 CSV 330행 전체로") {
        val columns = listOf(
            "level", "kanji1", "yomi1", "meaning1", "kanji2", "yomi2", "meaning2", "quiz",
            "word1", "wordMeaning1", "word2", "wordMeaning2", "word3", "wordMeaning3", "word4", "wordMeaning4",
            "sentence", "sentenceMeaning", "answer", "explanation",
            "wrongAnswer1", "wrongAnswer2", "wrongAnswer3", "sentenceTiles",
        )
        fun load(file: String): List<DailyContentDto> =
            javaClass.classLoader.getResource("static/$file")!!.readText().lines().filter { it.isNotBlank() }
                .mapIndexed { i, line ->
                    val c = line.split(";").also { it.size shouldBe columns.size }
                    DailyContentDto(
                        KanjiLevel.valueOf(c[0]), i + 1L, c[1], c[2], c[3], c[4], c[5], c[6],
                        c[8], c[9], c[10], c[11], c[12], c[13], c[14], c[15], c[16], c[17], c[7], c[18], c[19],
                        c[20], c[21], c[22], c[23],
                    )
                }

        listOf("beginner", "intermediate", "advanced").forEach { level ->
            When("$level 의 모든 회차로 세션을 만들면") {
                val contents = load("${level}_daily_contents.csv")

                Then("모두 만들어지고, 보기와 블록이 규칙을 지킨다") {
                    contents shouldHaveSize 110
                    contents.forEach { content ->
                        val session = QuizSessionFactory.create(content, contents)
                            .shouldNotBeNull()
                        session.questions.filter { it.type != QuizQuestionType.ARRANGE }.forEach { q ->
                            q.options.map { it.text }.toSet() shouldHaveSize 4
                            (q.answerIndex!! in 0..3) shouldBe true
                        }
                        val arrange = session.questions[4]
                        (arrange.answerTexts!!.size <= QuizSessionFactory.ARRANGE_MAX_TILES) shouldBe true
                        arrange.options shouldHaveSize arrange.answerTexts!!.size + 2
                        arrange.answerTexts!!.joinToString("") shouldBe
                            SentenceTiles.plainText(content.sentence).filterNot { it in SentenceTiles.PUNCTUATION }
                    }
                }
            }
        }
    }
})
