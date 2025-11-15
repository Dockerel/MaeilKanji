package maeilkanji.maeilkanji.infra.entity

import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "feedback")
class FeedbackEntity(
    val memberId: String,
    val reason: String,
    val comment: String,
) : BaseEntity()