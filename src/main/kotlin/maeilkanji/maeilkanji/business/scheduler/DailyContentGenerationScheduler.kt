package maeilkanji.maeilkanji.business.scheduler

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.service.DailyContentGenerationService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class DailyContentGenerationScheduler(
    private val dailyContentGenerationService: DailyContentGenerationService,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DailyContentGenerationScheduler::class.java)
    }

    /**
     * 매일 자정(KST)에 초급 → 중급 → 고급 문제를 하나씩 새로 만들어 저장한다.
     * ANTHROPIC_API_KEY 가 없으면 건너뛴다. 한 레벨이 실패해도 나머지 레벨은 계속 만든다.
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    fun generateDailyContents() {
        if (!dailyContentGenerationService.isEnabled()) {
            logger.warn("DailyContentGenerationScheduler skipped: ANTHROPIC_API_KEY is not set")
            return
        }
        KanjiLevel.values().forEach { level ->
            runCatching { dailyContentGenerationService.generateAndSave(level) }
                .onSuccess { logger.info("DailyContentGenerationScheduler saved {} content id={}", level, it) }
                .onFailure { logger.error("DailyContentGenerationScheduler failed to generate {} content", level, it) }
        }
    }

}
