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

/**
 * 프로필 아바타. 화면에는 한자 한 글자(glyph)를 색 타일에 담아 보여주고,
 * 색은 profile.css 의 .av-{cssName} 에 정의한다. emoji 는 배지 알림 등 텍스트용.
 */
enum class ProfileAvatar(
    val emoji: String,
    val glyph: String,
    val label: String,
    override val requiredMaxStreak: Int,
) : Unlockable {
    SMILE("🙂", "笑", "웃음", 0),
    CAT("🐱", "猫", "고양이", 0),
    DOG("🐶", "犬", "강아지", 0),
    RABBIT("🐰", "兎", "토끼", 0),
    FOX("🦊", "狐", "여우", 3),
    PANDA("🐼", "熊", "판다", 7),
    FROG("🐸", "蛙", "개구리", 7),
    OWL("🦉", "梟", "부엉이", 14),
    TIGER("🐯", "虎", "호랑이", 30),
    CRANE("🕊️", "鶴", "학", 50),
    DRAGON("🐉", "龍", "용", 100),
    UNICORN("🦄", "麟", "기린", 365),
    ;

    val cssName: String get() = name.lowercase()

    companion object {
        fun fromName(name: String?): ProfileAvatar? = values().firstOrNull { it.name == name }
    }
}
