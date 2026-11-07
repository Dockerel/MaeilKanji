package maeilkanji.maeilkanji.business.service.request

data class UpdateProfileServiceRequest(
    // 비우면 익명으로 돌아간다.
    val nickname: String?,
    val bio: String?,
    // ProfileAvatar / ProfileTheme / Badge 의 enum 이름. null 이면 기본값
    val avatar: String?,
    val theme: String?,
    val featuredBadge: String?,
)
