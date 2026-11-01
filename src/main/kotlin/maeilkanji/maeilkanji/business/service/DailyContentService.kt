package maeilkanji.maeilkanji.business.service

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.domain.MemberStatus
import maeilkanji.maeilkanji.business.publisher.DailyContentEmailPublisher
import maeilkanji.maeilkanji.business.repository.DailyContentRepository
import maeilkanji.maeilkanji.business.repository.MemberRepository
import maeilkanji.maeilkanji.business.service.dto.DailyContentDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional(readOnly = true)
class DailyContentService(
    private val dailyContentEmailPublisher: DailyContentEmailPublisher,

    private val dailyContentRepository: DailyContentRepository,
    private val memberRepository: MemberRepository,
) {

    fun findBeginnerDailyContent(contentId: Long): DailyContentDto {
        return dailyContentRepository.getBeginnerDailyContentById(contentId)
    }

    fun findIntermediateDailyContent(contentId: Long): DailyContentDto {
        return dailyContentRepository.getIntermediateDailyContentById(contentId)
    }

    fun findAdvancedDailyContent(contentId: Long): DailyContentDto {
        return dailyContentRepository.getAdvancedDailyContentById(contentId)
    }

    fun sendBeginnerDailyContentEmails() {
        sendDailyContentEmails(KanjiLevel.BEGINNER)
    }

    fun sendIntermediateDailyContentEmails() {
        sendDailyContentEmails(KanjiLevel.INTERMEDIATE)
    }

    fun sendAdvancedDailyContentEmails() {
        sendDailyContentEmails(KanjiLevel.ADVANCED)
    }

    /**
     * 오늘 콘텐츠 메일을 모든 레벨의 ACTIVE 회원에게 다시 발행한다. 관리자 수동 재발송용.
     * 이미 받은 회원도 다시 받는다. 발행한 회원 수를 반환한다.
     */
    fun resendTodayDailyContentEmails(): Int {
        return KanjiLevel.values().sumOf { sendDailyContentEmails(it) }
    }

    private fun sendDailyContentEmails(level: KanjiLevel): Int {
        val members = memberRepository.findAllByMemberStatusAndLevel(MemberStatus.ACTIVE, level)

        for (member in members) {
            dailyContentEmailPublisher.publishSendDailyContentEmailMessage(level, member.id, member.email)
        }
        return members.size
    }

}