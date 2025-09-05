package com.example.chatapp.model

data class Contact(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val profileImageUrl: String? = null,
    val isFromDevice: Boolean = false, // Indica se foi importado do dispositivo
    val addedAt: Long = System.currentTimeMillis()
)
