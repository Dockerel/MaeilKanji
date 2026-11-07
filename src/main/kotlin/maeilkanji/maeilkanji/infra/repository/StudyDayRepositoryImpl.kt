package maeilkanji.maeilkanji.infra.repository

import maeilkanji.maeilkanji.business.repository.StudyDayRepository
import maeilkanji.maeilkanji.infra.entity.StudyDayEntity
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.*

@Component
class StudyDayRepositoryImpl(
    private val studyDayJpaRepository: StudyDayJpaRepository,
) : StudyDayRepository {

    override fun saveIfAbsent(memberId: UUID, date: LocalDate) {
        if (studyDayJpaRepository.existsByMemberIdAndStudyDate(memberId, date)) return
        studyDayJpaRepository.save(StudyDayEntity(memberId, date))
    }

    override fun count(memberId: UUID): Int = studyDayJpaRepository.countByMemberId(memberId).toInt()

    override fun findDates(memberId: UUID, from: LocalDate): Set<LocalDate> =
        studyDayJpaRepository.findAllByMemberIdAndStudyDateGreaterThanEqual(memberId, from)
            .mapTo(mutableSetOf()) { it.studyDate }

    override fun countAllByMember(): Map<UUID, Int> =
        studyDayJpaRepository.countGroupByMember().associate { it.memberId to it.days.toInt() }

}
