package maeilkanji.maeilkanji.business.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service

@Service
class EmailTemplateService(
    @Value("classpath:templates/email/verification.html")
    private val templateResource: Resource
) {

    fun generateEmailVerificationTemplate(randomCode: String): String {
        val template = templateResource.inputStream.bufferedReader().use { it.readText() }
        return template.replace("{{CODE}}", randomCode)
    }

}