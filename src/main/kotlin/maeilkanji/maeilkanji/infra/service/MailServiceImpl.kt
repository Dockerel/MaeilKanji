package maeilkanji.maeilkanji.infra.service

import lombok.extern.slf4j.Slf4j
import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import maeilkanji.maeilkanji.business.service.MailService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component

@Slf4j
@Component
class MailServiceImpl(
    private val javaMailSender: JavaMailSender
) : MailService {

    @Retryable(
        maxAttempts = 3,
        backoff = Backoff(delay = 1000),
        include = [MaeilKanjiException::class]
    )
    override fun send(email: String, title: String, content: String) {
        try {
            val mimeMessage = javaMailSender.createMimeMessage()

            MimeMessageHelper(mimeMessage, false, "UTF-8").apply {
                setTo(email)
                setSubject(title)
                setText(content, true)
            }

            javaMailSender.send(mimeMessage)
        } catch (e: Exception) {
            throw MaeilKanjiException("Failed to send email $email", HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

}