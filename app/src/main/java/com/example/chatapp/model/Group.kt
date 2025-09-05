package com.example.chatapp.model

data class Group(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val participants: Map<String, String> = emptyMap(), // userId -> userName
    val admins: List<String> = emptyList(), // Lista de IDs dos administradores
    val lastMessage: String = "",
    val lastMessageSender: String = "", // Nome de quem enviou a última mensagem
    val lastMessageTime: Long = 0L
)

data class GroupMessage(
    val id: String = "",
    val groupId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderImage: String? = null,
    val message: String? = "",
    val imageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "SENT",
    val readBy: Map<String, Long> = emptyMap()
)
