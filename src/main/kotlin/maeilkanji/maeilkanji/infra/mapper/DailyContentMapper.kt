package maeilkanji.maeilkanji.infra.mapper

import maeilkanji.maeilkanji.business.repository.DailyContentRepository
import maeilkanji.maeilkanji.business.service.dto.DailyContentDto
import maeilkanji.maeilkanji.infra.entity.AdvancedDailyContentEntity
import maeilkanji.maeilkanji.infra.entity.BeginnerDailyContentEntity
import maeilkanji.maeilkanji.infra.entity.IntermediateDailyContentEntity
import org.springframework.stereotype.Component

@Component
class DailyContentMapper{

    fun convert(dailyContentEntity: BeginnerDailyContentEntity): DailyContentDto {
        return DailyContentDto(
            dailyContentEntity.level,
            dailyContentEntity.kanji1,
            dailyContentEntity.yomi1,
            dailyContentEntity.meaning1,
            dailyContentEntity.kanji2,
            dailyContentEntity.yomi2,
            dailyContentEntity.meaning2,
            dailyContentEntity.word1,
            dailyContentEntity.wordMeaning1,
            dailyContentEntity.word2,
            dailyContentEntity.wordMeaning2,
            dailyContentEntity.word3,
            dailyContentEntity.wordMeaning3,
            dailyContentEntity.word4,
            dailyContentEntity.wordMeaning4,
            dailyContentEntity.sentence,
            dailyContentEntity.sentenceMeaning,
            dailyContentEntity.quiz,
            dailyContentEntity.answer,
            dailyContentEntity.explanation,
        )
    }

    fun convert(dailyContentEntity: IntermediateDailyContentEntity): DailyContentDto {
        return DailyContentDto(
            dailyContentEntity.level,
            dailyContentEntity.kanji1,
            dailyContentEntity.yomi1,
            dailyContentEntity.meaning1,
            dailyContentEntity.kanji2,
            dailyContentEntity.yomi2,
            dailyContentEntity.meaning2,
            dailyContentEntity.word1,
            dailyContentEntity.wordMeaning1,
            dailyContentEntity.word2,
            dailyContentEntity.wordMeaning2,
            dailyContentEntity.word3,
            dailyContentEntity.wordMeaning3,
            dailyContentEntity.word4,
            dailyContentEntity.wordMeaning4,
            dailyContentEntity.sentence,
            dailyContentEntity.sentenceMeaning,
            dailyContentEntity.quiz,
            dailyContentEntity.answer,
            dailyContentEntity.explanation,
        )
    }

    fun convert(dailyContentEntity: AdvancedDailyContentEntity): DailyContentDto {
        return DailyContentDto(
            dailyContentEntity.level,
            dailyContentEntity.kanji1,
            dailyContentEntity.yomi1,
            dailyContentEntity.meaning1,
            dailyContentEntity.kanji2,
            dailyContentEntity.yomi2,
            dailyContentEntity.meaning2,
            dailyContentEntity.word1,
            dailyContentEntity.wordMeaning1,
            dailyContentEntity.word2,
            dailyContentEntity.wordMeaning2,
            dailyContentEntity.word3,
            dailyContentEntity.wordMeaning3,
            dailyContentEntity.word4,
            dailyContentEntity.wordMeaning4,
            dailyContentEntity.sentence,
            dailyContentEntity.sentenceMeaning,
            dailyContentEntity.quiz,
            dailyContentEntity.answer,
            dailyContentEntity.explanation,
        )
    }

}
