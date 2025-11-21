package maeilkanji.maeilkanji.business.repository

interface DailyContentIndexRepository {

    fun getIndex(): Long
    fun updateIndex()

}
