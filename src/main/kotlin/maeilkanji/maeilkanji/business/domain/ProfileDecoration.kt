package maeilkanji.maeilkanji.business.domain

/**
 * 프로필 꾸미기 요소. 최장 스트릭(maxStreak)이 기준에 닿으면 잠금이 풀린다.
 * enum 이름은 DB 에 저장되므로 바꾸지 않는다.
 */
interface Unlockable {
    val requiredMaxStreak: Int
    fun isUnlocked(maxStreak: Int): Boolean = maxStreak >= requiredMaxStreak
}

/** 프로필 상단 배경 테마. 실제 색은 profile.css 의 .theme-{cssName} 에 정의한다. */
enum class ProfileTheme(
    val label: String,
    override val requiredMaxStreak: Int,
) : Unlockable {
    DEFAULT("기본 블루", 0),
    SAKURA("벚꽃", 3),
    MATCHA("말차", 7),
    SUNSET("노을", 14),
    NIGHT("밤하늘", 30),
    FUJI("후지산", 100),
    GOLD("황금", 365),
    ;

    val cssName: String get() = name.lowercase()

    companion object {
        fun fromName(name: String?): ProfileTheme? = values().firstOrNull { it.name == name }

        /** 아직 잠긴 테마 중 가장 가까운 것. 모두 풀렸으면 null */
        fun nextLocked(maxStreak: Int): ProfileTheme? = values().firstOrNull { !it.isUnlocked(maxStreak) }
    }
}

/** 프로필 아바타(이모지) */
enum class ProfileAvatar(
    val emoji: String,
    override val requiredMaxStreak: Int,
) : Unlockable {
    SMILE("🙂", 0),
    CAT("🐱", 0),
    DOG("🐶", 0),
    RABBIT("🐰", 0),
    FOX("🦊", 3),
    PANDA("🐼", 7),
    FROG("🐸", 7),
    OWL("🦉", 14),
    TIGER("🐯", 30),
    CRANE("🕊️", 50),
    DRAGON("🐉", 100),
    UNICORN("🦄", 365),
    ;

    companion object {
        fun fromName(name: String?): ProfileAvatar? = values().firstOrNull { it.name == name }
    }
}
