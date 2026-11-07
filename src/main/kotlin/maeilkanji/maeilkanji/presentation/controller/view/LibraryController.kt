package maeilkanji.maeilkanji.presentation.controller.view

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.domain.SentenceTiles
import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import maeilkanji.maeilkanji.business.service.KanjiLibraryService
import maeilkanji.maeilkanji.business.service.dto.DailyContentDto
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

/**
 * 검색 유입용 공개 페이지.
 * - /lessons, /lessons/{level}: 레벨별 전체 학습 목록
 * - /kanji/{한자}: 한자 하나의 읽기·뜻·단어·예문 모음
 */
@Controller
class LibraryController(
    private val kanjiLibraryService: KanjiLibraryService,
) {

    @GetMapping("/lessons")
    fun showAllLessons(model: Model): String {
        model.addAttribute("sections", kanjiLibraryService.allLessons().map { (level, lessons) -> LessonSection(level, lessons) })
        model.addAttribute("currentLevel", null)
        return "lessons"
    }

    @GetMapping("/lessons/{levelPath}")
    fun showLevelLessons(@PathVariable levelPath: String, model: Model): String {
        val level = KanjiLevel.fromPath(levelPath)
            ?: throw MaeilKanjiException("Unknown level: $levelPath", HttpStatus.NOT_FOUND)
        model.addAttribute("sections", listOf(LessonSection(level, kanjiLibraryService.lessons(level))))
        model.addAttribute("currentLevel", level)
        return "lessons"
    }

    @GetMapping("/kanji/{kanji}")
    fun showKanji(@PathVariable kanji: String, model: Model): String {
        val entry = kanjiLibraryService.kanji(kanji)
            ?: throw MaeilKanjiException("Unknown kanji: $kanji", HttpStatus.NOT_FOUND)
        model.addAttribute("entry", entry)
        model.addAttribute("sentenceHtml", entry.sentences.map { SentenceTiles.html(it.sentence) })
        return "kanji"
    }

    data class LessonSection(
        val level: KanjiLevel,
        val lessons: List<DailyContentDto>,
    )
}
