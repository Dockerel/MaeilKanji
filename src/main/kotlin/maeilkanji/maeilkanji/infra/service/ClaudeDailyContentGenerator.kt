package maeilkanji.maeilkanji.infra.service

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.models.messages.JsonOutputFormat
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.service.DailyContentGenerator
import maeilkanji.maeilkanji.business.service.dto.DailyContentDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

private const val MODEL = "claude-opus-5-5"
private const val MAX_TOKENS = 16000L
private const val SYSTEM_PROMPT_PATH = "prompt/daily_content_generation.md"

/** 모델이 돌려주는 JSON. level 과 id 는 서버가 채운다. */
private data class GeneratedContent(
    val kanji1: String,
    val yomi1: String,
    val meaning1: String,
    val kanji2: String,
    val yomi2: String,
    val meaning2: String,
    val quiz: String,
    val word1: String,
    val wordMeaning1: String,
    val word2: String,
    val wordMeaning2: String,
    val word3: String,
    val wordMeaning3: String,
    val word4: String,
    val wordMeaning4: String,
    val sentence: String,
    val sentenceMeaning: String,
    val answer: String,
    val explanation: String,
    val wrongAnswer1: String,
    val wrongAnswer2: String,
    val wrongAnswer3: String,
    val sentenceTiles: String,
)

private val FIELDS = listOf(
    "kanji1", "yomi1", "meaning1", "kanji2", "yomi2", "meaning2", "quiz",
    "word1", "wordMeaning1", "word2", "wordMeaning2", "word3", "wordMeaning3", "word4", "wordMeaning4",
    "sentence", "sentenceMeaning", "answer", "explanation",
    "wrongAnswer1", "wrongAnswer2", "wrongAnswer3", "sentenceTiles",
)

/**
 * Claude API 로 학습 문제를 만든다. 구조화 출력(JSON 스키마)으로 필드 형식을 강제하고,
 * 내용 규칙은 시스템 프롬프트(resources/prompt/daily_content_generation.md)와 서버 검증으로 지킨다.
 */
@Component
class ClaudeDailyContentGenerator(
    @Value("\${anthropic.api-key:}")
    private val apiKey: String,
    private val objectMapper: ObjectMapper,
) : DailyContentGenerator {

    private val systemPrompt: String by lazy {
        ClassPathResource(SYSTEM_PROMPT_PATH).inputStream.use { String(it.readAllBytes(), Charsets.UTF_8) }
    }

    private val client: AnthropicClient by lazy {
        AnthropicOkHttpClient.builder().apiKey(apiKey).build()
    }

    private val outputFormat: JsonOutputFormat by lazy {
        val properties = FIELDS.associateWith { mapOf("type" to "string") }
        JsonOutputFormat.builder()
            .schema(
                JsonOutputFormat.Schema.builder()
                    .putAdditionalProperty("type", JsonValue.from("object"))
                    .putAdditionalProperty("properties", JsonValue.from(properties))
                    .putAdditionalProperty("required", JsonValue.from(FIELDS))
                    .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                    .build()
            )
            .build()
    }

    override fun isEnabled(): Boolean = apiKey.isNotBlank()

    override fun generate(level: KanjiLevel, usedPairs: List<String>, previousError: String?): DailyContentDto {
        check(isEnabled()) { "ANTHROPIC_API_KEY is not set" }

        val params = MessageCreateParams.builder()
            .model(MODEL)
            .maxTokens(MAX_TOKENS)
            .system(systemPrompt)
            .outputConfig(
                OutputConfig.builder()
                    .effort(OutputConfig.Effort.HIGH)
                    .format(outputFormat)
                    .build()
            )
            // 안전 분류기가 거절하면 서버가 거절 유형에 맞는 다른 모델로 다시 시도한다.
            .putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
            .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
            .addUserMessage(userMessage(level, usedPairs, previousError))
            .build()

        val response = client.messages().create(params)

        when (response.stopReason().orElse(null)) {
            StopReason.REFUSAL -> throw IllegalStateException("Claude refused: ${response.stopDetails().orElse(null)}")
            StopReason.MAX_TOKENS -> throw IllegalStateException("Claude response hit max_tokens")
            else -> Unit
        }

        val json = response.content()
            .mapNotNull { it.text().orElse(null)?.text() }
            .joinToString("")
        val generated = objectMapper.readValue<GeneratedContent>(json)
        return generated.toDto(level)
    }

    private fun userMessage(level: KanjiLevel, usedPairs: List<String>, previousError: String?): String = buildString {
        appendLine("${level.name} (${level.korean}) 레벨 문제를 하나 만들어 주세요.")
        appendLine()
        appendLine("이미 사용한 한자 쌍 (kanji1/kanji2):")
        appendLine(usedPairs.joinToString(", ").ifEmpty { "(없음)" })
        if (previousError != null) {
            appendLine()
            appendLine("직전에 만든 문제가 아래 이유로 검증에 실패했습니다. 이 점을 고쳐서 새로 만들어 주세요.")
            appendLine(previousError)
        }
    }

    private fun GeneratedContent.toDto(level: KanjiLevel) = DailyContentDto(
        level = level,
        kanji1 = kanji1.trim(),
        yomi1 = yomi1.trim(),
        meaning1 = meaning1.trim(),
        kanji2 = kanji2.trim(),
        yomi2 = yomi2.trim(),
        meaning2 = meaning2.trim(),
        word1 = word1.trim(),
        wordMeaning1 = wordMeaning1.trim(),
        word2 = word2.trim(),
        wordMeaning2 = wordMeaning2.trim(),
        word3 = word3.trim(),
        wordMeaning3 = wordMeaning3.trim(),
        word4 = word4.trim(),
        wordMeaning4 = wordMeaning4.trim(),
        sentence = sentence.trim(),
        sentenceMeaning = sentenceMeaning.trim(),
        quiz = quiz.trim(),
        answer = answer.trim(),
        explanation = explanation.trim(),
        wrongAnswer1 = wrongAnswer1.trim(),
        wrongAnswer2 = wrongAnswer2.trim(),
        wrongAnswer3 = wrongAnswer3.trim(),
        sentenceTiles = sentenceTiles.trim(),
    )
}
