package maeilkanji.maeilkanji.infra.repository

import maeilkanji.maeilkanji.infra.entity.BeginnerDailyContentEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BeginnerDailyContentJpaRepository : JpaRepository<BeginnerDailyContentEntity, Long>