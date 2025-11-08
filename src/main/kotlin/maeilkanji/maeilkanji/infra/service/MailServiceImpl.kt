package maeilkanji.maeilkanji.infra.service

import maeilkanji.maeilkanji.business.service.MailService
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

@Component
class MailServiceImpl(
    private val javaMailSender: JavaMailSender
) : MailService {

    override fun send(email: String, content: String) {
        val mimeMessage = javaMailSender.createMimeMessage()

        MimeMessageHelper(mimeMessage, false, "UTF-8").apply {
            setTo(email)
            setSubject("MaeilKanji Email Verification Code")
            setText(content, true)
        }

        javaMailSender.send(mimeMessage)
    }

}