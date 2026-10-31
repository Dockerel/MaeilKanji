package maeilkanji.maeilkanji.infra.repository

import maeilkanji.maeilkanji.business.repository.FeedbackRepository
import maeilkanji.maeilkanji.business.service.dto.FeedbackDto
import maeilkanji.maeilkanji.infra.entity.FeedbackEntity
import org.springframework.stereotype.Repository

@Repository
class FeedbackRepositoryImpl(
    private val feedbackJpaRepository: FeedbackJpaRepository
) : FeedbackRepository {
    override fun save(memberId: String, reason: String, comment: String) {
        val entity = FeedbackEntity(memberId, reason, comment)
        feedbackJpaRepository.save(entity)
    }

    override fun findAll(): List<FeedbackDto> {
        return feedbackJpaRepository.findAll().map {
            FeedbackDto(it.id!!, it.memberId, it.reason, it.comment, it.createdAt)
        }
    }
}