package maeilkanji.maeilkanji.presentation.controller.api.request

import jakarta.validation.constraints.Size
import maeilkanji.maeilkanji.business.service.request.UpdateProfileServiceRequest

data class UpdateProfileRequest(
    @field:Size(max = 20, message = "닉네임이 너무 길어요.")
    val nickname: String? = null,
    @field:Size(max = 100, message = "소개는 100자까지 쓸 수 있어요.")
    val bio: String? = null,
    val avatar: String? = null,
    val theme: String? = null,
    val featuredBadge: String? = null,
) {
    fun toServiceRequest() = UpdateProfileServiceRequest(nickname, bio, avatar, theme, featuredBadge)
}
