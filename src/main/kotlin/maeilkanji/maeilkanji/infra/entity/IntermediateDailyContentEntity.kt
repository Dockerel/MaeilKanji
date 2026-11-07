package maeilkanji.maeilkanji.infra.entity

import jakarta.persistence.*
import maeilkanji.maeilkanji.business.domain.KanjiLevel

@Entity
@Table(name = "intermediate_daily_content")
class IntermediateDailyContentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Enumerated(EnumType.STRING)
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
    @Column(columnDefinition = "TEXT")
    val sentence: String,
    @Column(columnDefinition = "TEXT")
    val sentenceMeaning: String,
    val quiz: String,
    val answer: String,
    val explanation: String,
    // 객관식 오답 보기. 도입 전 콘텐츠는 비어 있을 수 있다.
    val wrongAnswer1: String? = null,
    val wrongAnswer2: String? = null,
    val wrongAnswer3: String? = null,
)