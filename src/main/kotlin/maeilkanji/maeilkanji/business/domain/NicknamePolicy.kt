package maeilkanji.maeilkanji.business.domain

/** 닉네임 규칙: 2~12자, 한글·영문·숫자·밑줄(_)만. 공개 프로필 주소(/u/{닉네임})로도 쓴다. */
object NicknamePolicy {

    const val MIN_LENGTH = 2
    const val MAX_LENGTH = 12

    private val PATTERN = Regex("^[가-힣A-Za-z0-9_]{$MIN_LENGTH,$MAX_LENGTH}$")

    fun isValid(nickname: String): Boolean = PATTERN.matches(nickname)

    const val RULE_MESSAGE = "닉네임은 2~12자의 한글, 영문, 숫자, 밑줄(_)만 쓸 수 있어요."
}
