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

    fun toBeginnerEntity(dto: DailyContentDto): BeginnerDailyContentEntity {
        return BeginnerDailyContentEntity(
            level = dto.level,
            kanji1 = dto.kanji1,
            yomi1 = dto.yomi1,
            meaning1 = dto.meaning1,
            kanji2 = dto.kanji2,
            yomi2 = dto.yomi2,
            meaning2 = dto.meaning2,
            word1 = dto.word1,
            wordMeaning1 = dto.wordMeaning1,
            word2 = dto.word2,
            wordMeaning2 = dto.wordMeaning2,
            word3 = dto.word3,
            wordMeaning3 = dto.wordMeaning3,
            word4 = dto.word4,
            wordMeaning4 = dto.wordMeaning4,
            sentence = dto.sentence,
            sentenceMeaning = dto.sentenceMeaning,
            quiz = dto.quiz,
            answer = dto.answer,
            explanation = dto.explanation,
        )
    }

    fun toIntermediateEntity(dto: DailyContentDto): IntermediateDailyContentEntity {
        return IntermediateDailyContentEntity(
            level = dto.level,
            kanji1 = dto.kanji1,
            yomi1 = dto.yomi1,
            meaning1 = dto.meaning1,
            kanji2 = dto.kanji2,
            yomi2 = dto.yomi2,
            meaning2 = dto.meaning2,
            word1 = dto.word1,
            wordMeaning1 = dto.wordMeaning1,
            word2 = dto.word2,
            wordMeaning2 = dto.wordMeaning2,
            word3 = dto.word3,
            wordMeaning3 = dto.wordMeaning3,
            word4 = dto.word4,
            wordMeaning4 = dto.wordMeaning4,
            sentence = dto.sentence,
            sentenceMeaning = dto.sentenceMeaning,
            quiz = dto.quiz,
            answer = dto.answer,
            explanation = dto.explanation,
        )
    }

    fun toAdvancedEntity(dto: DailyContentDto): AdvancedDailyContentEntity {
        return AdvancedDailyContentEntity(
            level = dto.level,
            kanji1 = dto.kanji1,
            yomi1 = dto.yomi1,
            meaning1 = dto.meaning1,
            kanji2 = dto.kanji2,
            yomi2 = dto.yomi2,
            meaning2 = dto.meaning2,
            word1 = dto.word1,
            wordMeaning1 = dto.wordMeaning1,
            word2 = dto.word2,
            wordMeaning2 = dto.wordMeaning2,
            word3 = dto.word3,
            wordMeaning3 = dto.wordMeaning3,
            word4 = dto.word4,
            wordMeaning4 = dto.wordMeaning4,
            sentence = dto.sentence,
            sentenceMeaning = dto.sentenceMeaning,
            quiz = dto.quiz,
            answer = dto.answer,
            explanation = dto.explanation,
        )
    }

}
