package maeilkanji.maeilkanji.infra.repository

import maeilkanji.maeilkanji.infra.entity.AdvancedDailyContentEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AdvancedDailyContentJpaRepository : JpaRepository<AdvancedDailyContentEntity, Long>