package maeilkanji.maeilkanji.business.service

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.repository.DailyContentRepository
import maeilkanji.maeilkanji.business.service.dto.DailyContentDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.util.*

private const val BASE_URL = "http://localhost:8080"
//private const val BASE_URL = "https://998a2c91822b.ngrok-free.app"

@Service
class EmailTemplateService(
    private val dailyContentRepository: DailyContentRepository,

    @Value("classpath:templates/email/verification.html")
    private val emailVerificationTemplateResource: Resource,
    @Value("classpath:templates/email/daily-content-email.html")
    private val beginnerDailyContentTemplateResource: Resource,
) {

    fun generateEmailVerificationTemplate(randomCode: String): String {
        val template = emailVerificationTemplateResource.inputStream.bufferedReader().use { it.readText() }
        return template.replace("{{CODE}}", randomCode)
    }

    fun generateDailyContentEmailTemplate(level: KanjiLevel, contentIndex: Long, memberId: UUID): Map<String, String> {
        val content = getContentByLevelAndContentIndex(level, contentIndex)

        val template = beginnerDailyContentTemplateResource.inputStream.bufferedReader().use { it.readText() }

        val siteUrl = when (content.level) {
            KanjiLevel.BEGINNER -> "$BASE_URL/daily-content/beginner/$contentIndex"
            KanjiLevel.INTERMEDIATE -> "$BASE_URL/daily-content/intermediate/$contentIndex"
            KanjiLevel.ADVANCED -> "$BASE_URL/daily-content/advanced/$contentIndex"
        }

        val fullTemplate = template
            .replace("{{level}}", content.level.korean)
            .replace("{{kanji1}}", content.kanji1)
            .replace("{{yomi1}}", content.yomi1)
            .replace("{{meaning1}}", content.meaning1)
            .replace("{{kanji2}}", content.kanji2)
            .replace("{{yomi2}}", content.yomi2)
            .replace("{{meaning2}}", content.meaning2)
            .replace("{{quiz}}", content.quiz)
            .replace("{{SITE_ACCESS_URL}}", siteUrl)
            .replace("{{UNSUBSCRIBE_URL}}", "$BASE_URL/unsubscribe-confirm?memberId=$memberId")


        return mapOf("quiz" to content.quiz, "template" to fullTemplate)
    }

    private fun getContentByLevelAndContentIndex(level: KanjiLevel, contentIndex: Long): DailyContentDto {
        return when (level) {
            KanjiLevel.BEGINNER -> {
                dailyContentRepository.getBeginnerDailyContentById(contentIndex)
            }

            KanjiLevel.INTERMEDIATE -> {
                dailyContentRepository.getIntermediateDailyContentById(contentIndex)
            }

            else -> { // Advanced
                dailyContentRepository.getAdvancedDailyContentById(contentIndex)
            }
        }
    }

}