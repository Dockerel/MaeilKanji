package maeilkanji.maeilkanji

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MaeilKanjiApplication

fun main(args: Array<String>) {
    runApplication<MaeilKanjiApplication>(*args)
}
