package maeilkanji.maeilkanji.business.service

import jakarta.annotation.PostConstruct
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.repository.DailyContentRepository
import maeilkanji.maeilkanji.business.service.dto.DailyContentDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Year
import java.time.ZoneId
import java.util.*


@Service
class EmailTemplateService(
    private val dailyContentRepository: DailyContentRepository,

    @Value("\${service.base-url}")
    private val baseUrl: String,

    @Value("classpath:templates/email/verification.html")
    private val emailVerificationTemplateResource: Resource,
    @Value("classpath:templates/email/daily-content-email.html")
    private val dailyContentTemplateResource: Resource,
    @Value("classpath:templates/email/admin-login-code.html")
    private val adminLoginCodeTemplateResource: Resource,

    private val clock: Clock = Clock.system(ZoneId.of("Asia/Seoul")),
) {

    private lateinit var emailVerificationTemplate:String
    private lateinit var dailyContentTemplate:String
    private lateinit var adminLoginCodeTemplate:String

    @PostConstruct
    fun init() {
        emailVerificationTemplate=emailVerificationTemplateResource.inputStream.bufferedReader().use { it.readText() }
        dailyContentTemplate=dailyContentTemplateResource.inputStream.bufferedReader().use { it.readText() }
        adminLoginCodeTemplate=adminLoginCodeTemplateResource.inputStream.bufferedReader().use { it.readText() }
    }

    fun generateEmailVerificationTemplate(randomCode: String): String {
        return emailVerificationTemplate
            .replace("{{LOGO_URL}}", logoUrl())
            .replace("{{CODE}}", randomCode)
            .replace("{{YEAR}}", currentYear())
    }

    fun generateAdminLoginCodeTemplate(code: String): String {
        return adminLoginCodeTemplate
            .replace("{{LOGO_URL}}", logoUrl())
            .replace("{{CODE}}", code)
            .replace("{{YEAR}}", currentYear())
    }

    fun generateDailyContentEmailTemplate(level: KanjiLevel, contentIndex: Long, memberId: UUID): Map<String, String> {
        val content = getContentByLevelAndContentIndex(level, contentIndex)

        val siteUrl = when (content.level) {
            KanjiLevel.BEGINNER -> "$baseUrl/daily-content/beginner/$contentIndex"
            KanjiLevel.INTERMEDIATE -> "$baseUrl/daily-content/intermediate/$contentIndex"
            KanjiLevel.ADVANCED -> "$baseUrl/daily-content/advanced/$contentIndex"
        }

        val fullTemplate = dailyContentTemplate
            .replace("{{LOGO_URL}}", logoUrl())
            .replace("{{level}}", content.level.korean)
            .replace("{{kanji1}}", content.kanji1)
            .replace("{{yomi1}}", content.yomi1)
            .replace("{{meaning1}}", content.meaning1)
            .replace("{{kanji2}}", content.kanji2)
            .replace("{{yomi2}}", content.yomi2)
            .replace("{{meaning2}}", content.meaning2)
            .replace("{{quiz}}", content.quiz)
            .replace("{{SITE_ACCESS_URL}}", siteUrl)
            .replace("{{UNSUBSCRIBE_URL}}", "$baseUrl/unsubscribe-confirm?memberId=$memberId")
            .replace("{{LEVEL_CHANGE_URL}}", "$baseUrl/change-level-confirm?memberId=$memberId")
            .replace("{{YEAR}}", currentYear())


        return mapOf("quiz" to content.quiz, "template" to fullTemplate)
    }

    private fun logoUrl(): String = "$baseUrl/maeilkanji.png"

    private fun currentYear(): String = Year.now(clock).value.toString()

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