package maeilkanji.maeilkanji.infra.repository

import maeilkanji.maeilkanji.business.repository.FeedbackRepository
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
}