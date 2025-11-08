package maeilkanji.maeilkanji.presentation

import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiControllerAdvice {

    @ExceptionHandler(MaeilKanjiException::class)
    fun handleMaeilKanjiException(e: MaeilKanjiException): ResponseEntity<String> {
        return ResponseEntity(e.message, e.status)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMaeilKanjiException(e: MethodArgumentNotValidException): ResponseEntity<String> {
        return ResponseEntity(e.getBindingResult().getAllErrors().get(0).getDefaultMessage(), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<String> {
        e.printStackTrace()
        return ResponseEntity(e.message, HttpStatus.INTERNAL_SERVER_ERROR)
    }

}
