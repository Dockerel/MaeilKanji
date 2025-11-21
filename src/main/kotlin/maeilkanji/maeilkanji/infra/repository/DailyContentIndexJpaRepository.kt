package maeilkanji.maeilkanji.infra.repository

import maeilkanji.maeilkanji.infra.entity.DailyContentIndexEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface DailyContentIndexJpaRepository : JpaRepository<DailyContentIndexEntity, Long> {
    @Query("SELECT d FROM DailyContentIndexEntity d ORDER BY d.id ASC")
    fun getFirst(): DailyContentIndexEntity?
}