package maeilkanji.maeilkanji.infra.repository

import maeilkanji.maeilkanji.infra.entity.StudyDayEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface StudyDayJpaRepository : JpaRepository<StudyDayEntity, UUID> {
    fun existsByMemberIdAndStudyDate(memberId: UUID, studyDate: LocalDate): Boolean
    fun countByMemberId(memberId: UUID): Long
    fun findAllByMemberIdAndStudyDateGreaterThanEqual(memberId: UUID, from: LocalDate): List<StudyDayEntity>

    @Query("select s.memberId as memberId, count(s) as days from StudyDayEntity s group by s.memberId")
    fun countGroupByMember(): List<MemberStudyDayCount>

    interface MemberStudyDayCount {
        val memberId: UUID
        val days: Long
    }
}
