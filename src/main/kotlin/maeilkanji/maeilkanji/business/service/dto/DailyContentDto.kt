package maeilkanji.maeilkanji.business.service.dto

import maeilkanji.maeilkanji.business.domain.KanjiLevel

data class DailyContentDto(
    val level: KanjiLevel,
    val kanji1: String,
    val yomi1: String,
    val meaning1: String,
    val kanji2: String,
    val yomi2: String,
    val meaning2: String,
    val word1: String,
    val wordMeaning1: String,
    val word2: String,
    val wordMeaning2: String,
    val word3: String,
    val wordMeaning3: String,
    val word4: String,
    val wordMeaning4: String,
    val sentence: String,
    val sentenceMeaning: String,
    val quiz: String,
    val answer: String,
    val explanation: String,
)