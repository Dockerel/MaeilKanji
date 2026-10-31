package maeilkanji.maeilkanji.support

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.RabbitMQContainer

@SpringBootTest
@ActiveProfiles("test")
@Transactional
abstract class IntegrationTestSupport() {
    companion object {
        // 싱글톤 컨테이너: 모든 테스트 클래스가 공유하고 JVM 종료 시 Ryuk 이 정리한다.
        val mySQLContainer = MySQLContainer<Nothing>("mysql:8.0").also { it.start() }
        val redisContainer = GenericContainer<Nothing>("redis:7-alpine").apply { withExposedPorts(6379) }.also { it.start() }
        val rabbitMQContainer = RabbitMQContainer("rabbitmq:3-management").also { it.start() }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { mySQLContainer.jdbcUrl }
            registry.add("spring.datasource.username") { mySQLContainer.username }
            registry.add("spring.datasource.password") { mySQLContainer.password }

            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
            registry.add("spring.data.redis.password") { "" }

            registry.add("spring.rabbitmq.host") { rabbitMQContainer.host }
            registry.add("spring.rabbitmq.port") { rabbitMQContainer.amqpPort }
            registry.add("spring.rabbitmq.username") { rabbitMQContainer.adminUsername }
            registry.add("spring.rabbitmq.password") { rabbitMQContainer.adminPassword }
        }
    }

    @MockitoBean
    lateinit var restTemplate: RestTemplate
}
