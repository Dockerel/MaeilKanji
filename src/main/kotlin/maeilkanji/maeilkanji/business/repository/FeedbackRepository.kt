package maeilkanji.maeilkanji.business.repository

import maeilkanji.maeilkanji.business.service.dto.FeedbackDto

interface FeedbackRepository {

    fun save(memberId: String, reason: String, comment: String)
    fun findAll(): List<FeedbackDto>
}
