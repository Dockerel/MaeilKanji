package mailnews.maeilnews

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MaeilNewsApplication

fun main(args: Array<String>) {
    runApplication<MaeilNewsApplication>(*args)
}
