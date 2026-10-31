package maeilkanji.maeilkanji.infra.repository

import maeilkanji.maeilkanji.infra.entity.IntermediateDailyContentEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface IntermediateDailyContentJpaRepository : JpaRepository<IntermediateDailyContentEntity, Long>