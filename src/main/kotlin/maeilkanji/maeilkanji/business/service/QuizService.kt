package maeilkanji.maeilkanji.business.service

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.domain.QuizChoices
import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import maeilkanji.maeilkanji.business.repository.DailyContentRepository
import maeilkanji.maeilkanji.business.repository.MemberRepository
import maeilkanji.maeilkanji.business.repository.QuizAnswerRepository
import maeilkanji.maeilkanji.business.service.dto.DailyContentDto
import maeilkanji.maeilkanji.business.service.request.SubmitQuizAnswerServiceRequest
import maeilkanji.maeilkanji.business.service.response.SubmitQuizAnswerResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true)
class QuizService(
    private val dailyContentRepository: DailyContentRepository,
    private val memberRepository: MemberRepository,
    private val quizAnswerRepository: QuizAnswerRepository,
) {

    fun findContent(level: KanjiLevel, contentId: Long): DailyContentDto {
        return when (level) {
            KanjiLevel.BEGINNER -> dailyContentRepository.getBeginnerDailyContentById(contentId)
            KanjiLevel.INTERMEDIATE -> dailyContentRepository.getIntermediateDailyContentById(contentId)
            KanjiLevel.ADVANCED -> dailyContentRepository.getAdvancedDailyContentById(contentId)
        }
    }

    fun findChoices(level: KanjiLevel, contentId: Long): QuizChoices? = QuizChoices.of(findContent(level, contentId))

    /**
     * 퀴즈 응답을 채점하고 정답률을 반환한다.
     * 회원은 콘텐츠당 첫 응답만 기록한다. 다시 풀어도 채점은 하지만 집계는 바뀌지 않는다.
     */
    @Transactional
    fun submitAnswer(request: SubmitQuizAnswerServiceRequest): SubmitQuizAnswerResponse {
        val quizChoices = findChoices(request.level, request.contentId)
            ?: throw MaeilKanjiException("객관식 보기가 없는 콘텐츠입니다.", HttpStatus.BAD_REQUEST)
        if (!quizChoices.isValidChoice(request.choiceIndex)) {
            throw MaeilKanjiException("잘못된 보기 번호입니다.", HttpStatus.BAD_REQUEST)
        }
        val correct = quizChoices.isCorrect(request.choiceIndex)

        request.memberId?.takeIf { it.isNotBlank() }?.let { recordFirstAnswer(UUID.fromString(it), request, correct) }

        val stats = quizAnswerRepository.getStats(request.level, request.contentId)
        return SubmitQuizAnswerResponse(
            correct = correct,
            answerIndex = quizChoices.answerIndex,
            totalCount = stats.totalCount,
            correctCount = stats.correctCount,
            correctRate = stats.correctRate,
        )
    }

    private fun recordFirstAnswer(memberId: UUID, request: SubmitQuizAnswerServiceRequest, correct: Boolean) {
        // 존재하지 않는 회원 id 로 집계가 오염되지 않도록 확인한다. (없으면 404)
        memberRepository.findById(memberId)
        if (quizAnswerRepository.exists(memberId, request.level, request.contentId)) return
        quizAnswerRepository.save(memberId, request.level, request.contentId, request.choiceIndex, correct)
    }

}
