package maeilkanji.maeilkanji.business.domain

enum class KanjiLevel(
    val korean: String,
) {
    BEGINNER("초급"),
    INTERMEDIATE("중급"),
    ADVANCED("고급"),
    ;

    /** URL 경로용 소문자 이름. 예: /daily-content/beginner/1 */
    val path: String get() = name.lowercase()

    companion object {
        fun fromPath(path: String): KanjiLevel? = values().firstOrNull { it.path == path }
    }
}
