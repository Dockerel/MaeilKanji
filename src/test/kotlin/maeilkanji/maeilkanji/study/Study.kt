package maeilkanji.maeilkanji.study

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.infra.entity.AdvancedDailyContentEntity
import maeilkanji.maeilkanji.infra.entity.BeginnerDailyContentEntity
import maeilkanji.maeilkanji.infra.entity.IntermediateDailyContentEntity
import maeilkanji.maeilkanji.infra.repository.AdvancedDailyContentJpaRepository
import maeilkanji.maeilkanji.infra.repository.BeginnerDailyContentJpaRepository
import maeilkanji.maeilkanji.infra.repository.IntermediateDailyContentJpaRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.Commit
import org.springframework.transaction.annotation.Transactional
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

@SpringBootTest
class Study() {

    @Autowired
    lateinit var beginnerDailyContentJpaRepository: BeginnerDailyContentJpaRepository

    @Autowired
    lateinit var intermediateDailyContentJpaRepository: IntermediateDailyContentJpaRepository

    @Autowired
    lateinit var advancedDailyContentJpaRepository: AdvancedDailyContentJpaRepository

    @Test
    @Transactional
    @Commit
//    @Disabled
    fun SaveDailyContentsByLevel() {
        val contents = listOf(
            "./src/main/resources/static/beginner_daily_contents.csv",
            "./src/main/resources/static/intermediate_daily_contents.csv",
            "./src/main/resources/static/advanced_daily_contents.csv",
        )

        for (i in 0..2) {
            val content = contents[i]
            val file = File(content)
            val reader = BufferedReader(FileReader(file, Charsets.UTF_8))
            for (line in reader.lines()) {
                val split = line.split(";")
                when (i) {
                    0 -> {
                        val entity = BeginnerDailyContentEntity(
                            level = KanjiLevel.valueOf(split[0]),

                            kanji1 = split[1],
                            yomi1 = split[2],
                            meaning1 = split[3],
                            kanji2 = split[4],
                            yomi2 = split[5],
                            meaning2 = split[6],
                            quiz = split[7],

                            word1 = split[8],
                            wordMeaning1 = split[9],
                            word2 = split[10],
                            wordMeaning2 = split[11],
                            word3 = split[12],
                            wordMeaning3 = split[13],
                            word4 = split[14],
                            wordMeaning4 = split[15],

                            sentence = split[16],
                            sentenceMeaning = split[17],
                            answer = split[18],
                            explanation = split[19],
                        )
                        beginnerDailyContentJpaRepository.save(entity)
                    }

                    1 -> {
                        val entity = IntermediateDailyContentEntity(
                            level = KanjiLevel.valueOf(split[0]),

                            kanji1 = split[1],
                            yomi1 = split[2],
                            meaning1 = split[3],
                            kanji2 = split[4],
                            yomi2 = split[5],
                            meaning2 = split[6],
                            quiz = split[7],

                            word1 = split[8],
                            wordMeaning1 = split[9],
                            word2 = split[10],
                            wordMeaning2 = split[11],
                            word3 = split[12],
                            wordMeaning3 = split[13],
                            word4 = split[14],
                            wordMeaning4 = split[15],

                            sentence = split[16],
                            sentenceMeaning = split[17],
                            answer = split[18],
                            explanation = split[19],
                        )
                        intermediateDailyContentJpaRepository.save(entity)
                    }

                    else -> {
                        val entity = AdvancedDailyContentEntity(
                            level = KanjiLevel.valueOf(split[0]),

                            kanji1 = split[1],
                            yomi1 = split[2],
                            meaning1 = split[3],
                            kanji2 = split[4],
                            yomi2 = split[5],
                            meaning2 = split[6],
                            quiz = split[7],

                            word1 = split[8],
                            wordMeaning1 = split[9],
                            word2 = split[10],
                            wordMeaning2 = split[11],
                            word3 = split[12],
                            wordMeaning3 = split[13],
                            word4 = split[14],
                            wordMeaning4 = split[15],

                            sentence = split[16],
                            sentenceMeaning = split[17],
                            answer = split[18],
                            explanation = split[19],
                        )
                        advancedDailyContentJpaRepository.save(entity)
                    }
                }
            }
        }
    }
}

