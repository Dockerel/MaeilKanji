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

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    fun sendBeginnerDailyContentEmails() {
        dailyContentService.sendBeginnerDailyContentEmails()
        logger.info("DailyContentScheduler sending beginner daily content")
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    fun sendIntermediateDailyContentEmails() {
        dailyContentService.sendIntermediateDailyContentEmails()
        logger.info("DailyContentScheduler sending intermediate daily content")
    }

    @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Seoul")
    fun sendAdvancedDailyContentEmails() {
        dailyContentService.sendAdvancedDailyContentEmails()
        logger.info("DailyContentScheduler sending advanced daily content")
    }

}