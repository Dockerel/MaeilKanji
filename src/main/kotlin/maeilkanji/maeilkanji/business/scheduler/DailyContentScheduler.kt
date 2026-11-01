package maeilkanji.maeilkanji.business.scheduler

import maeilkanji.maeilkanji.business.service.DailyContentService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class DailyContentScheduler(
    private val dailyContentService: DailyContentService,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DailyContentScheduler::class.java)
    }

    /**
     * 매일 오전 7시(KST)에 초급 → 중급 → 고급 순서로 발송한다.
     * 한 레벨이 실패해도 나머지 레벨은 계속 발송되도록 레벨별로 예외를 격리한다.
     */
    @Scheduled(cron = "0 0 7 * * *", zone = "Asia/Seoul")
    fun sendDailyContentEmails() {
        sendLevel("beginner") { dailyContentService.sendBeginnerDailyContentEmails() }
        sendLevel("intermediate") { dailyContentService.sendIntermediateDailyContentEmails() }
        sendLevel("advanced") { dailyContentService.sendAdvancedDailyContentEmails() }
    }

    private fun sendLevel(level: String, send: () -> Unit) {
        runCatching(send)
            .onSuccess { logger.info("DailyContentScheduler sending {} daily content", level) }
            .onFailure { logger.error("DailyContentScheduler failed to send {} daily content", level, it) }
    }

}