package maeilkanji.maeilkanji.business.repository

interface FeedbackRepository {

    fun save(memberId: String, reason: String, comment: String)
}
