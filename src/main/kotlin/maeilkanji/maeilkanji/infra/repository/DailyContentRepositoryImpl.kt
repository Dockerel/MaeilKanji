package maeilkanji.maeilkanji.infra.repository

import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import maeilkanji.maeilkanji.business.repository.DailyContentRepository
import maeilkanji.maeilkanji.business.service.dto.DailyContentDto
import maeilkanji.maeilkanji.infra.mapper.DailyContentMapper
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class DailyContentRepositoryImpl(
    private val beginnerDailyContentJpaRepository: BeginnerDailyContentJpaRepository,
    private val intermediateDailyContentJpaRepository: IntermediateDailyContentJpaRepository,
    private val advancedDailyContentJpaRepository: AdvancedDailyContentJpaRepository,

    private val dailyContentMapper: DailyContentMapper,
) : DailyContentRepository {
    override fun getBeginnerDailyContentById(contentId: Long): DailyContentDto {
        val dailyContent = beginnerDailyContentJpaRepository.findById(contentId)
            .orElseThrow { MaeilKanjiException("Can't find daily content with id: $contentId", HttpStatus.NOT_FOUND) }
        return dailyContentMapper.convert(dailyContent)
    }

    override fun getIntermediateDailyContentById(contentId: Long): DailyContentDto {
        val dailyContent = intermediateDailyContentJpaRepository.findById(contentId)
            .orElseThrow { MaeilKanjiException("Can't find daily content with id: $contentId", HttpStatus.NOT_FOUND) }
        return dailyContentMapper.convert(dailyContent)
    }

    override fun getAdvancedDailyContentById(contentId: Long): DailyContentDto {
        val dailyContent = advancedDailyContentJpaRepository.findById(contentId)
            .orElseThrow { MaeilKanjiException("Can't find daily content with id: $contentId", HttpStatus.NOT_FOUND) }
        return dailyContentMapper.convert(dailyContent)
    }

}