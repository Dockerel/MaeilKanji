package maeilkanji.maeilkanji.business.service

import maeilkanji.maeilkanji.business.domain.Badge
import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.domain.QuizSession
import maeilkanji.maeilkanji.business.domain.QuizSessionFactory
import maeilkanji.maeilkanji.business.exception.MaeilKanjiException
import maeilkanji.maeilkanji.business.repository.DailyContentRepository
import maeilkanji.maeilkanji.business.repository.MemberRepository
import maeilkanji.maeilkanji.business.repository.QuizQuestionAnswerRepository
import maeilkanji.maeilkanji.business.service.dto.DailyContentDto
import maeilkanji.maeilkanji.business.service.request.SubmitQuizSessionServiceRequest
import maeilkanji.maeilkanji.business.service.response.BadgeResponse
import maeilkanji.maeilkanji.business.service.response.SubmitQuizSessionResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true)
class QuizService(
    private val dailyContentRepository: DailyContentRepository,
    private val memberRepository: MemberRepository,
    private val quizQuestionAnswerRepository: QuizQuestionAnswerRepository,
    private val profileService: ProfileService,
) {

    /**
     * 오늘의 5문제를 만든다. 콘텐츠가 없으면 404.
     * 오답 보기나 예문 블록이 없는 콘텐츠면 null (학습 페이지의 정답 공개 방식으로 대체).
     */
    fun createSession(level: KanjiLevel, contentId: Long): QuizSession? {
        val pool = findAll(level)
        val today = pool.firstOrNull { it.id == contentId }
            ?: throw MaeilKanjiException("Can't find daily content with id: $contentId", HttpStatus.NOT_FOUND)
        return QuizSessionFactory.create(today, pool)
    }

    /**
     * 답안을 채점하고 오늘의 점수 통계를 반환한다.
     * 회원은 콘텐츠당 첫 세션만 기록한다. 다시 풀어도 채점은 하지만 집계는 바뀌지 않는다.
     */
    @Transactional
    fun submitSession(request: SubmitQuizSessionServiceRequest): SubmitQuizSessionResponse {
        val session = createSession(request.level, request.contentId)
            ?: throw MaeilKanjiException("퀴즈를 만들 수 없는 콘텐츠입니다.", HttpStatus.BAD_REQUEST)
        val results = session.grade(request.answers)
            ?: throw MaeilKanjiException("답안 개수가 문제 수와 다릅니다.", HttpStatus.BAD_REQUEST)

        val newBadges = request.memberId?.takeIf { it.isNotBlank() }
            ?.let { recordFirstSession(UUID.fromString(it), request, results) }
            .orEmpty()

        val score = results.count { it }
        val stats = quizQuestionAnswerRepository.getStats(request.level, request.contentId, results.size)
        return SubmitQuizSessionResponse(
            results = results,
            score = score,
            questionCount = results.size,
            participantCount = stats.participantCount,
            averageScore = stats.averageScore,
            topPercent = stats.topPercent(score),
            questionCorrectRates = stats.questionCorrectRates,
            newBadges = newBadges.map { BadgeResponse.of(it) },
        )
    }

    /** 첫 세션이면 기록하고, 그 기록으로 새로 얻은 퀴즈 배지를 반환한다. */
    private fun recordFirstSession(memberId: UUID, request: SubmitQuizSessionServiceRequest, results: List<Boolean>): List<Badge> {
        // 존재하지 않는 회원 id 로 집계가 오염되지 않도록 확인한다. (없으면 404)
        memberRepository.findById(memberId)
        if (quizQuestionAnswerRepository.exists(memberId, request.level, request.contentId)) return emptyList()
        val before = profileService.learningStats(memberId)
        quizQuestionAnswerRepository.saveAll(memberId, request.level, request.contentId, results)
        val after = profileService.learningStats(memberId)
        return Badge.newlyEarned(before, after)
    }

    private fun findAll(level: KanjiLevel): List<DailyContentDto> = when (level) {
        KanjiLevel.BEGINNER -> dailyContentRepository.findAllBeginner()
        KanjiLevel.INTERMEDIATE -> dailyContentRepository.findAllIntermediate()
        KanjiLevel.ADVANCED -> dailyContentRepository.findAllAdvanced()
    }

}
