package maeilkanji.maeilkanji.business.publisher

import maeilkanji.maeilkanji.business.config.RabbitMQConfig
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.publisher.dto.SendDailyContentEmailMessageDto
import maeilkanji.maeilkanji.business.service.DailyContentIndexService
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component
import java.util.*

@Component
class DailyContentEmailPublisher(
    private val dailyContentIndexService: DailyContentIndexService,
    private val rabbitTemplate: RabbitTemplate,
) {

    fun publishSendDailyContentEmailMessage(level: KanjiLevel, memberId: UUID, email: String) {
        val contentIndex = dailyContentIndexService.getTodayDailyContentIndex()
        val message = SendDailyContentEmailMessageDto(level, contentIndex, memberId, email)
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.SEND_DAILY_CONTENT_EMAIL_EXCHANGE,
            RabbitMQConfig.SEND_DAILY_CONTENT_EMAIL_QUEUE,
            message
        )
    }

}