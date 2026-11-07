package maeilkanji.maeilkanji.business.service.response

data class UpdateProfileResponse(
    val nickname: String?,
    // 저장 후 이동할 공개 프로필 주소. 닉네임이 없으면 /me
    val profileUrl: String,
)
