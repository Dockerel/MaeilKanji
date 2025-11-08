package maeilkanji.maeilkanji.business.exception

import org.springframework.http.HttpStatus

class MaeilKanjiException(override val message: String, val status: HttpStatus) : RuntimeException(message)