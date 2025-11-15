package maeilkanji.maeilkanji.support

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import org.testcontainers.containers.MySQLContainer

@SpringBootTest
@Transactional
abstract class IntegrationTestSupport() {
    companion object {
        val mySQLContainer = MySQLContainer<Nothing>("mysql:8").also { it.start() }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { mySQLContainer.jdbcUrl }
            registry.add("spring.datasource.username") { mySQLContainer.username }
            registry.add("spring.datasource.password") { mySQLContainer.password }
        }
    }

    @MockitoBean
    lateinit var restTemplate: RestTemplate
}