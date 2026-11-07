package maeilkanji.maeilkanji.presentation.controller.api

import jakarta.validation.Valid
import maeilkanji.maeilkanji.business.service.QuizService
import maeilkanji.maeilkanji.business.service.response.SubmitQuizAnswerResponse
import maeilkanji.maeilkanji.presentation.controller.api.request.SubmitQuizAnswerRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/quiz")
class QuizRestController(
    private val quizService: QuizService,
) {

    @PostMapping("/answers")
    fun submitAnswer(@Valid @RequestBody request: SubmitQuizAnswerRequest): ResponseEntity<SubmitQuizAnswerResponse> {
        val response = quizService.submitAnswer(request.toServiceRequest())
        return ResponseEntity.ok(response)
    }

}
