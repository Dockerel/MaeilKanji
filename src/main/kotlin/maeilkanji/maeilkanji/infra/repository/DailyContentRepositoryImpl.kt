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

    override fun findAllBeginner(): List<DailyContentDto> {
        return beginnerDailyContentJpaRepository.findAll().map { dailyContentMapper.convert(it) }
    }

    override fun findAllIntermediate(): List<DailyContentDto> {
        return intermediateDailyContentJpaRepository.findAll().map { dailyContentMapper.convert(it) }
    }

    override fun findAllAdvanced(): List<DailyContentDto> {
        return advancedDailyContentJpaRepository.findAll().map { dailyContentMapper.convert(it) }
    }

    override fun saveBeginner(content: DailyContentDto): Long {
        val entity = dailyContentMapper.toBeginnerEntity(content)
        return beginnerDailyContentJpaRepository.save(entity).id!!
    }

    override fun saveIntermediate(content: DailyContentDto): Long {
        val entity = dailyContentMapper.toIntermediateEntity(content)
        return intermediateDailyContentJpaRepository.save(entity).id!!
    }

    override fun saveAdvanced(content: DailyContentDto): Long {
        val entity = dailyContentMapper.toAdvancedEntity(content)
        return advancedDailyContentJpaRepository.save(entity).id!!
    }

    override fun deleteBeginner(id: Long) {
        beginnerDailyContentJpaRepository.deleteById(id)
    }

    override fun deleteIntermediate(id: Long) {
        intermediateDailyContentJpaRepository.deleteById(id)
    }

    override fun deleteAdvanced(id: Long) {
        advancedDailyContentJpaRepository.deleteById(id)
    }

}