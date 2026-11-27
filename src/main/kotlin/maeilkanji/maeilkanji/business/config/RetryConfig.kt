package maeilkanji.maeilkanji.business.config

import org.springframework.context.annotation.Configuration
import org.springframework.resilience.annotation.EnableResilientMethods

// Spring Framework 7 내장 재시도 (@Retryable). spring-retry 대체
@EnableResilientMethods
@Configuration
class RetryConfig {
}