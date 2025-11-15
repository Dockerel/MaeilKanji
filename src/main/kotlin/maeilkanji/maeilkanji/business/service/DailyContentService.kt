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
        val members = memberRepository.findAllByMemberStatusAndLevel(MemberStatus.ACTIVE, KanjiLevel.BEGINNER)

        for (member in members) {
            dailyContentEmailPublisher.publishSendDailyContentEmailMessage(
                KanjiLevel.BEGINNER,
                member.contentIndex,
                member.id,
                member.email
            )
        }
    }

    fun sendIntermediateDailyContentEmails() {
        val members = memberRepository.findAllByMemberStatusAndLevel(MemberStatus.ACTIVE, KanjiLevel.INTERMEDIATE)

        for (member in members) {
            dailyContentEmailPublisher.publishSendDailyContentEmailMessage(
                KanjiLevel.INTERMEDIATE,
                member.contentIndex,
                member.id,
                member.email
            )
        }
    }

    fun sendAdvancedDailyContentEmails() {
        val members = memberRepository.findAllByMemberStatusAndLevel(MemberStatus.ACTIVE, KanjiLevel.ADVANCED)

        for (member in members) {
            dailyContentEmailPublisher.publishSendDailyContentEmailMessage(
                KanjiLevel.ADVANCED,
                member.contentIndex,
                member.id,
                member.email
            )
        }
    }

}