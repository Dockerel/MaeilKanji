package maeilkanji.maeilkanji.business.scheduler

import maeilkanji.maeilkanji.business.service.DailyContentIndexService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class DailyContentIndexScheduler(
    private val dailyContentIndexService: DailyContentIndexService,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DailyContentIndexScheduler::class.java)
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    fun increaseDailyContentIndex() {
        dailyContentIndexService.updateDailyContentIndex()
        logger.info("DailyContentScheduler increasing daily content")
    }

}