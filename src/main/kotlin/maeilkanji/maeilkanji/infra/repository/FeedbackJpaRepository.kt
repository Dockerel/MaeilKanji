package maeilkanji.maeilkanji.infra.repository

import maeilkanji.maeilkanji.infra.entity.FeedbackEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface FeedbackJpaRepository : JpaRepository<FeedbackEntity, UUID>