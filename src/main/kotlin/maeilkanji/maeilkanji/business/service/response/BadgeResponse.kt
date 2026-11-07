package maeilkanji.maeilkanji.business.service.response

import maeilkanji.maeilkanji.business.domain.Badge

data class BadgeResponse(
    val code: String,
    val emoji: String,
    val title: String,
    val description: String,
) {
    companion object {
        fun of(badge: Badge) = BadgeResponse(badge.name, badge.emoji, badge.title, badge.description)
    }
}
