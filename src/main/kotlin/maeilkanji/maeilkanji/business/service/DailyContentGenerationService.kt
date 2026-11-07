package maeilkanji.maeilkanji.business.service

import maeilkanji.maeilkanji.business.domain.GeneratedContentValidator
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import maeilkanji.maeilkanji.business.repository.DailyContentRepository
import maeilkanji.maeilkanji.business.service.dto.DailyContentDto
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

const val MAX_GENERATION_ATTEMPTS = 3

/**
 * 레벨별 학습 문제를 하나 새로 만들어 검증한 뒤 저장한다.
 * 검증에 실패하면 실패 사유를 모델에게 알려 주고 [MAX_GENERATION_ATTEMPTS]회까지 다시 만든다.
 */
@Service
class DailyContentGenerationService(
    private val dailyContentGenerator: DailyContentGenerator,
    private val dailyContentRepository: DailyContentRepository,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DailyContentGenerationService::class.java)
    }

    fun isEnabled(): Boolean = dailyContentGenerator.isEnabled()

    /** 새 문제를 만들어 저장하고 저장된 id 를 돌려준다. 모든 시도가 실패하면 예외를 던진다. */
    fun generateAndSave(level: KanjiLevel): Long {
        val existing = findAll()
        val usedPairs = existing.map { "${it.kanji1}/${it.kanji2}" }.distinct()

        var previousError: String? = null
        repeat(MAX_GENERATION_ATTEMPTS) { attempt ->
            val content = runCatching { dailyContentGenerator.generate(level, usedPairs, previousError) }
                .getOrElse {
                    logger.warn("{} 문제 생성 호출 실패 ({}회차)", level, attempt + 1, it)
                    previousError = null
                    return@repeat
                }

            val errors = GeneratedContentValidator.validate(level, content.copy(id = null), existing)
            if (errors.isEmpty()) {
                return save(level, content.copy(id = null))
            }
            logger.warn("{} 생성 문제 검증 실패 ({}회차): {}", level, attempt + 1, errors)
            previousError = errors.joinToString("\n") { "- $it" }
        }

        throw MaeilKanjiException(
            "$level 문제 생성이 ${MAX_GENERATION_ATTEMPTS}회 모두 실패했습니다.",
            HttpStatus.INTERNAL_SERVER_ERROR
        )
    }

    private fun findAll(): List<DailyContentDto> =
        dailyContentRepository.findAllBeginner() +
            dailyContentRepository.findAllIntermediate() +
            dailyContentRepository.findAllAdvanced()

    private fun save(level: KanjiLevel, content: DailyContentDto): Long = when (level) {
        KanjiLevel.BEGINNER -> dailyContentRepository.saveBeginner(content)
        KanjiLevel.INTERMEDIATE -> dailyContentRepository.saveIntermediate(content)
        KanjiLevel.ADVANCED -> dailyContentRepository.saveAdvanced(content)
    }
}
