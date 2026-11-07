package maeilkanji.maeilkanji.presentation.controller.api.request

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.service.dto.DailyContentDto
import jakarta.validation.constraints.NotBlank

data class ContentRequest(
    @field:NotBlank val level: String,
    @field:NotBlank val kanji1: String,
    @field:NotBlank val yomi1: String,
    @field:NotBlank val meaning1: String,
    @field:NotBlank val kanji2: String,
    @field:NotBlank val yomi2: String,
    @field:NotBlank val meaning2: String,
    @field:NotBlank val word1: String,
    @field:NotBlank val wordMeaning1: String,
    @field:NotBlank val word2: String,
    @field:NotBlank val wordMeaning2: String,
    @field:NotBlank val word3: String,
    @field:NotBlank val wordMeaning3: String,
    @field:NotBlank val word4: String,
    @field:NotBlank val wordMeaning4: String,
    @field:NotBlank val sentence: String,
    @field:NotBlank val sentenceMeaning: String,
    @field:NotBlank val quiz: String,
    @field:NotBlank val answer: String,
    @field:NotBlank val explanation: String,
    @field:NotBlank val wrongAnswer1: String,
    @field:NotBlank val wrongAnswer2: String,
    @field:NotBlank val wrongAnswer3: String,
    @field:NotBlank val sentenceTiles: String,
) {
    fun toDailyContentDto(): DailyContentDto {
        return DailyContentDto(
            level = KanjiLevel.valueOf(level),
            kanji1 = kanji1,
            yomi1 = yomi1,
            meaning1 = meaning1,
            kanji2 = kanji2,
            yomi2 = yomi2,
            meaning2 = meaning2,
            word1 = word1,
            wordMeaning1 = wordMeaning1,
            word2 = word2,
            wordMeaning2 = wordMeaning2,
            word3 = word3,
            wordMeaning3 = wordMeaning3,
            word4 = word4,
            wordMeaning4 = wordMeaning4,
            sentence = sentence,
            sentenceMeaning = sentenceMeaning,
            quiz = quiz,
            answer = answer,
            explanation = explanation,
            wrongAnswer1 = wrongAnswer1,
            wrongAnswer2 = wrongAnswer2,
            wrongAnswer3 = wrongAnswer3,
            sentenceTiles = sentenceTiles,
        )
    }
}
