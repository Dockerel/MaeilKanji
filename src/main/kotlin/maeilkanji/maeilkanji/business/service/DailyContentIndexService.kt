package maeilkanji.maeilkanji.business.service

import maeilkanji.maeilkanji.business.repository.DailyContentIndexRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class DailyContentIndexService(
    private val dailyContentIndexRepository: DailyContentIndexRepository
) {

    fun getTodayDailyContentIndex(): Long {
        return dailyContentIndexRepository.getIndex()
    }

    @Transactional
    fun updateDailyContentIndex() {
        dailyContentIndexRepository.updateIndex()
    }

}