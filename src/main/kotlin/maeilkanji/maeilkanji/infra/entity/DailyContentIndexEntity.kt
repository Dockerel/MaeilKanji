package maeilkanji.maeilkanji.infra.entity

import jakarta.persistence.*

@Entity
@Table(name = "daily_content_index")
class DailyContentIndexEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    var dailyContentIndex: Long = 0L,
) {
    fun updateIndex(value: Long) {
        dailyContentIndex += value;
    }
}