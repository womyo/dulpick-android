package com.dulpick.app.domain.profile

data class UserProfile(
    val nickname: String,
    val iconId: Int,
    val datePreference: DatePreference?,
)
