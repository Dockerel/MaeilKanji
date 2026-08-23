package maeilkanji.maeilkanji.business.repository

import maeilkanji.maeilkanji.business.service.dto.DailyContentDto

interface DailyContentRepository {

    fun getBeginnerDailyContentById(contentId: Long): DailyContentDto
    fun getIntermediateDailyContentById(contentId: Long): DailyContentDto
    fun getAdvancedDailyContentById(contentId: Long): DailyContentDto

    fun saveBeginner(content: DailyContentDto): Long
    fun saveIntermediate(content: DailyContentDto): Long
    fun saveAdvanced(content: DailyContentDto): Long

}
