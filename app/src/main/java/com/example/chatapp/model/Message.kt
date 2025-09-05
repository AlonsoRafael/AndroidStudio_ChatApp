package com.example.chatapp.model

data class Message(
    val id: String = "",
    val senderId: String = "",
    val message: String? = "",
    val createdAt: Long = System.currentTimeMillis(),
    val senderName: String = "",
    val senderImage: String? = null,
    val imageUrl: String? = null,
    val videoUrl: String? = null, // URL do vídeo
    val audioUrl: String? = null, // URL do áudio
    val fileUrl: String? = null, // URL do arquivo
    val fileName: String? = null, // Nome do arquivo original
    val fileSize: Long? = null, // Tamanho do arquivo em bytes
    val audioDuration: Long? = null, // Duração do áudio em milissegundos
    val messageType: String = MessageType.TEXT.name, // Tipo da mensagem
    val status: String = MessageStatus.SENDING.name, // Status da mensagem
    val readBy: Map<String, Long> = emptyMap() // Map de userId -> timestamp quando leu
)
