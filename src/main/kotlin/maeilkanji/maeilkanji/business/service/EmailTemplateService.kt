package maeilkanji.maeilkanji.business.service

import jakarta.annotation.PostConstruct
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.domain.QuizChoices
import maeilkanji.maeilkanji.business.repository.DailyContentRepository
import maeilkanji.maeilkanji.business.service.dto.DailyContentDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.web.util.HtmlUtils
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
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

        // memberId 는 학습 페이지에서 스트릭 기록용으로 사용한다.
        val siteUrl = when (content.level) {
            KanjiLevel.BEGINNER -> "$baseUrl/daily-content/beginner/$contentIndex?memberId=$memberId"
            KanjiLevel.INTERMEDIATE -> "$baseUrl/daily-content/intermediate/$contentIndex?memberId=$memberId"
            KanjiLevel.ADVANCED -> "$baseUrl/daily-content/advanced/$contentIndex?memberId=$memberId"
        }

        val fullTemplate = dailyContentTemplate
            .replace("{{LOGO_URL}}", logoUrl())
            .replace("{{level}}", content.level.korean)
            .replace("{{kanji1}}", content.kanji1)
            .replace("{{yomi1}}", stripParentheses(content.yomi1))
            .replace("{{meaning1}}", content.meaning1)
            .replace("{{kanji2}}", content.kanji2)
            .replace("{{yomi2}}", stripParentheses(content.yomi2))
            .replace("{{meaning2}}", content.meaning2)
            .replace("{{quiz}}", content.quiz)
            .replace("{{QUIZ_CHOICES}}", quizChoicesHtml(content, memberId))
            .replace("{{SITE_ACCESS_URL}}", siteUrl)
            .replace("{{UNSUBSCRIBE_URL}}", "$baseUrl/unsubscribe-confirm?memberId=$memberId")
            .replace("{{LEVEL_CHANGE_URL}}", "$baseUrl/change-level-confirm?memberId=$memberId")
            .replace("{{FEEDBACK_URL}}", feedbackUrl(memberId, "${content.level.korean} #$contentIndex"))
            .replace("{{YEAR}}", currentYear())


        return mapOf("quiz" to content.quiz, "template" to fullTemplate)
    }

    /**
     * 메일용 객관식 보기 버튼. 누르면 결과 페이지(/daily-content/{level}/{id}/quiz)로 이동한다.
     * 메일 클라이언트 호환을 위해 table + 인라인 스타일만 쓴다.
     */
    private fun quizChoicesHtml(content: DailyContentDto, memberId: UUID): String {
        val quizChoices = QuizChoices.of(content) ?: return ""
        val rows = quizChoices.choices.mapIndexed { index, choice ->
            val url = "$baseUrl/daily-content/${content.level.path}/${content.id}/quiz?choice=$index&memberId=$memberId"
            """<tr><td style="padding:0 0 8px 0;"><a href="$url" style="display:block;background-color:#ffffff;border:1px solid #E2E8F0;border-radius:10px;padding:12px 16px;color:#0F172A;text-decoration:none;font-size:15px;"><strong style="color:#2563EB;">${index + 1}.</strong>&nbsp; <span lang="ja">${HtmlUtils.htmlEscape(choice)}</span></a></td></tr>"""
        }.joinToString("")
        return """<p style="margin:0 0 10px 0;font-size:13px;color:#64748B;">정답이라고 생각하는 보기를 눌러보세요!</p>""" +
            """<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="border-collapse:collapse;margin:0 0 12px 0;">$rows</table>"""
    }

    // 학습 페이지(fragments/daily-lesson.html)와 동일하게 읽기 표기의 괄호를 제거한다. 예: "(ぞう, ふ)" → "ぞう, ふ"
    private fun stripParentheses(yomi: String): String = yomi.replace(Regex("[()（）]"), "").trim()

    private fun feedbackUrl(memberId: UUID, context: String): String =
        "$baseUrl/feedback?memberId=$memberId&context=${URLEncoder.encode(context, StandardCharsets.UTF_8)}"

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