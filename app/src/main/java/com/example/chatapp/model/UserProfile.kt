package com.example.chatapp.model

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val profileImageUrl: String? = null,
    val bio: String = "",
    val status: UserStatus = UserStatus.OFFLINE,
    val lastSeen: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class UserStatus(val displayName: String) {
    ONLINE("Online"),
    OFFLINE("Offline"),
    BUSY("Ocupado"),
    AWAY("Ausente"),
    DO_NOT_DISTURB("Não incomodar")
}
