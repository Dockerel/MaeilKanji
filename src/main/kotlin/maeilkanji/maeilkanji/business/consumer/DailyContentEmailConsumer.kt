package maeilkanji.maeilkanji.business.consumer

import maeilkanji.maeilkanji.business.config.RabbitMQConfig
import maeilkanji.maeilkanji.business.publisher.dto.SendDailyContentEmailMessageDto
import maeilkanji.maeilkanji.business.service.EmailTemplateService
import maeilkanji.maeilkanji.business.service.MailService
import maeilkanji.maeilkanji.business.service.MemberService
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@Component
class DailyContentEmailConsumer(
    private val emailTemplateService: EmailTemplateService,
    private val mailService: MailService,
    private val memberService: MemberService,
) {

    @RabbitListener(queues = [RabbitMQConfig.SEND_DAILY_CONTENT_EMAIL_QUEUE])
    fun consumeDailyContentEmailMessage(message: SendDailyContentEmailMessageDto) {
        val content =
            emailTemplateService.generateDailyContentEmailTemplate(
                message.level,
                message.contentIndex,
                message.memberId
            )
        mailService.send(message.email, "[매일 칸지] ${content["quiz"]!!}", content["template"]!!)
        memberService.updateMemberContentIndex(message.memberId)
    }

    @RabbitListener(queues = [RabbitMQConfig.DEAD_LETTER_QUEUE])
    fun consumeDeadLetterDailyContentEmailMessage(message: SendDailyContentEmailMessageDto) {
        memberService.updateMemberToBounced(message.memberId)
    }

}