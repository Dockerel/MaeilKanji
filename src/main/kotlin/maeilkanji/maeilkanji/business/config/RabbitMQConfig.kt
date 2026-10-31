package maeilkanji.maeilkanji.business.config

import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class RabbitMQConfig {

    companion object {
        const val SEND_DAILY_CONTENT_EMAIL_QUEUE = "sendDailyContentEmailQueue"
        const val SEND_DAILY_CONTENT_EMAIL_EXCHANGE = "sendDailyContentEmailExchange"

        const val DEAD_LETTER_QUEUE = "deadLetterQueue"
        const val DEAD_LETTER_EXCHANGE = "deadLetterExchange"
    }


    // 메일 발송 큐
    @Bean
    fun sendDailyContentEmailQueue(): Queue {
        return QueueBuilder.durable(SEND_DAILY_CONTENT_EMAIL_QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", DEAD_LETTER_QUEUE)
            .build();
    }

    @Bean
    fun sendDailyContentEmailExchange(): DirectExchange {
        return DirectExchange(SEND_DAILY_CONTENT_EMAIL_EXCHANGE)
    }

    @Bean
    fun sendDailyContentEmailBinding(): Binding {
        return BindingBuilder.bind(sendDailyContentEmailQueue())
            .to(sendDailyContentEmailExchange())
            .with(SEND_DAILY_CONTENT_EMAIL_QUEUE)
    }


    @Bean
    fun deadLetterQueue(): Queue {
        return Queue(DEAD_LETTER_QUEUE)
    }

    @Bean
    fun deadLetterExchange(): DirectExchange {
        return DirectExchange(DEAD_LETTER_EXCHANGE)
    }

    @Bean
    fun deadLetterBinding(): Binding {
        return BindingBuilder.bind(deadLetterQueue())
            .to(deadLetterExchange())
            .with(DEAD_LETTER_QUEUE)
    }

    // 직렬화, 역직렬화 설정
    @Bean
    fun messageConverter(): MessageConverter {
        return Jackson2JsonMessageConverter()
    }

    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory): RabbitTemplate {
        return RabbitTemplate(connectionFactory)
            .apply { messageConverter = messageConverter() }
    }

    @Bean
    fun rabbitListenerContainerFactory(
        connectionFactory: ConnectionFactory?,
        configurer: SimpleRabbitListenerContainerFactoryConfigurer
    ): SimpleRabbitListenerContainerFactory {
        val factory = SimpleRabbitListenerContainerFactory()

        configurer.configure(factory, connectionFactory)

        factory.setConnectionFactory(connectionFactory)
        factory.setMessageConverter(messageConverter())

        return factory
    }

}