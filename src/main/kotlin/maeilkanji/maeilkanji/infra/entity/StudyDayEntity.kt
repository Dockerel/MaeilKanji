package maeilkanji.maeilkanji.infra.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.util.*

/** 회원이 학습한 날(한국 시간). 하루에 한 행. 학습 잔디·누적 학습일 집계에 쓴다. */
@Entity
@Table(
    name = "study_day",
    uniqueConstraints = [UniqueConstraint(columnNames = ["member_id", "study_date"])],
)
class StudyDayEntity(
    @Column(name = "member_id", nullable = false)
    val memberId: UUID,
    @Column(name = "study_date", nullable = false)
    val studyDate: LocalDate,
) : BaseEntity()
