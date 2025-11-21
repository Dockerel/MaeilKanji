package maeilkanji.maeilkanji.infra.repository

import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import maeilkanji.maeilkanji.business.repository.DailyContentIndexRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class DailyContentIndexRepositoryImpl(
    private val dailyContentIndexJpaRepository: DailyContentIndexJpaRepository
) : DailyContentIndexRepository {
    override fun getIndex(): Long {
        val firstEntity = dailyContentIndexJpaRepository.getFirst()
            ?: throw MaeilKanjiException("Can't find daily content index", HttpStatus.NOT_FOUND)
        return firstEntity.dailyContentIndex
    }

    override fun updateIndex() {
        val firstEntity = dailyContentIndexJpaRepository.getFirst()
            ?: throw MaeilKanjiException("Can't find daily content index", HttpStatus.NOT_FOUND)
        firstEntity.updateIndex(1L)
    }
}
