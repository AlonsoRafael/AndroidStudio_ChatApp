package com.example.chatapp.feature.chat

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.chatapp.R
import com.example.chatapp.SupabaseStorageUtils
import com.example.chatapp.FileInfo
import com.example.chatapp.model.Channel
import com.example.chatapp.model.Group
import com.example.chatapp.model.Message
import com.example.chatapp.model.MessageStatus
import com.example.chatapp.model.MessageType
import com.example.chatapp.model.UserProfile
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthCredential
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.storage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(@ApplicationContext val context: Context) : ViewModel() {


    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val message = _messages.asStateFlow()
    
    private val _allMessages = MutableStateFlow<List<Message>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    
    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive = _isSearchActive.asStateFlow()
    
    private val _currentGroup = MutableStateFlow<Group?>(null)
    val currentGroup = _currentGroup.asStateFlow()
    
    private val db = Firebase.database

    fun sendMessage(channelID: String, messageText: String?, image: String? = null, messageType: MessageType = MessageType.TEXT) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            // Buscar foto do perfil do usuário atual
            db.reference.child("users").child(currentUser.uid).get()
                .addOnSuccessListener { userSnapshot ->
                    val userProfile = userSnapshot.getValue(UserProfile::class.java)
                    val senderImage = userProfile?.profileImageUrl
                    
                    val message = Message(
                        db.reference.push().key ?: UUID.randomUUID().toString(),
                        currentUser.uid,
                        messageText,
                        System.currentTimeMillis(),
                        currentUser.displayName ?: userProfile?.name ?: "",
                        senderImage,
                        image,
                        messageType = messageType.name,
                        status = MessageStatus.SENDING.name
                    )

                    val messagesRef = if (isPrivateChat(channelID)) {
                        db.reference.child("private_messages").child(channelID)
                    } else {
                        db.reference.child("messages").child(channelID)
                    }
                    
                    messagesRef.push().setValue(message)
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                // Atualizar status para SENT
                                updateMessageStatus(channelID, message.id, MessageStatus.SENT)
                                
                                // Simular DELIVERED após um pequeno delay (como no WhatsApp)
                                viewModelScope.launch {
                                    kotlinx.coroutines.delay(1000) // 1 segundo
                                    updateMessageStatus(channelID, message.id, MessageStatus.DELIVERED)
                                }
                                
                                if (!isPrivateChat(channelID)) {
                                    postNotificationToUsers(channelID, message.senderName, messageText ?: getMessageTypeDescription(messageType))
                                    // Atualizar última mensagem do grupo
                                    updateGroupLastMessage(channelID, messageText ?: getMessageTypeDescription(messageType), message.senderName)
                                } else {
                                    // Para chats privados, notificar apenas o outro usuário
                                    postNotificationToPrivateChat(channelID, message.senderName, messageText ?: getMessageTypeDescription(messageType))
                                    // Atualizar último mensagem no chat privado
                                    updatePrivateChatLastMessage(channelID, messageText ?: getMessageTypeDescription(messageType))
                                }
                            }
                        }
                }
                .addOnFailureListener {
                    // Se falhar ao buscar o perfil, enviar mensagem sem foto
                    val message = Message(
                        db.reference.push().key ?: UUID.randomUUID().toString(),
                        currentUser.uid,
                        messageText,
                        System.currentTimeMillis(),
                        currentUser.displayName ?: "",
                        null,
                        image,
                        messageType = messageType.name
                    )

                    val messagesRef = if (isPrivateChat(channelID)) {
                        db.reference.child("private_messages").child(channelID)
                    } else {
                        db.reference.child("messages").child(channelID)
                    }
                    
                    messagesRef.push().setValue(message)
                }
        }
    }

    fun sendImageMessage(uri: Uri, channelID: String) {
        viewModelScope.launch {
            val storageUtils = SupabaseStorageUtils(context)
            val downloadUri = storageUtils.uploadImage(uri)
            downloadUri?.let {
                sendMessage(channelID, null, downloadUri, MessageType.IMAGE)
            }
        }
    }

    fun sendVideoMessage(uri: Uri, channelID: String) {
        viewModelScope.launch {
            val storageUtils = SupabaseStorageUtils(context)
            val fileInfo = storageUtils.uploadVideo(uri)
            fileInfo?.let { info ->
                sendVideoMessageWithInfo(channelID, info.url, info.fileName, info.fileSize)
            }
        }
    }

    fun sendAudioMessage(uri: Uri, channelID: String, duration: Long? = null) {
        Log.d("ChatViewModel", "sendAudioMessage chamado com URI: $uri, channelID: $channelID, duration: $duration")
        viewModelScope.launch {
            try {
                val storageUtils = SupabaseStorageUtils(context)
                Log.d("ChatViewModel", "Iniciando upload do áudio...")
                val fileInfo = storageUtils.uploadAudio(uri)
                fileInfo?.let { info ->
                    Log.d("ChatViewModel", "Áudio enviado com sucesso: ${info.url}")
                    sendAudioMessageWithInfo(channelID, info.url, info.fileName, info.fileSize, duration)
                } ?: run {
                    Log.e("ChatViewModel", "Falha no upload do áudio")
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Erro ao enviar áudio: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun sendFileMessage(uri: Uri, channelID: String) {
        viewModelScope.launch {
            val storageUtils = SupabaseStorageUtils(context)
            val fileInfo = storageUtils.uploadFile(uri)
            fileInfo?.let { info ->
                sendFileMessageWithInfo(channelID, info.url, info.fileName, info.fileSize)
            }
        }
    }

    fun sendStickerMessage(stickerUrl: String, channelID: String) {
        Log.d("ChatViewModel", "Enviando sticker: $stickerUrl")
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            db.reference.child("users").child(currentUser.uid).get()
                .addOnSuccessListener { userSnapshot ->
                    val userName = userSnapshot.child("name").getValue(String::class.java) ?: "Usuário"
                    val profilePictureUrl = userSnapshot.child("profilePictureUrl").getValue(String::class.java) ?: ""
                    
                    val messageId = UUID.randomUUID().toString()
                    val message = Message(
                        id = messageId,
                        senderId = currentUser.uid,
                        senderName = userName,
                        senderImage = profilePictureUrl,
                        message = null, // Stickers não têm texto
                        imageUrl = stickerUrl, // Usar imageUrl para mostrar o sticker
                        videoUrl = null,
                        audioUrl = null,
                        fileUrl = null,
                        fileName = "Sticker",
                        fileSize = null,
                        audioDuration = null,
                        messageType = MessageType.STICKER.name,
                        createdAt = System.currentTimeMillis(),
                        status = MessageStatus.SENT.name,
                        readBy = mapOf(currentUser.uid to System.currentTimeMillis())
                    )
                    
                    Log.d("ChatViewModel", "Objeto message criado: $message")
                    
                    // Salvar no local correto baseado no tipo de chat
                    val isPrivate = isPrivateChat(channelID)
                    val dbPath = if (isPrivate) {
                        "private_messages/$channelID/$messageId"
                    } else {
                        "messages/$channelID/$messageId"
                    }
                    
                    Log.d("ChatViewModel", "Salvando sticker no Firebase path: $dbPath (isPrivate: $isPrivate)")
                    
                    val dbRef = if (isPrivate) {
                        db.reference.child("private_messages").child(channelID).child(messageId)
                    } else {
                        db.reference.child("messages").child(channelID).child(messageId)
                    }
                    
                    dbRef.setValue(message)
                        .addOnSuccessListener {
                            Log.d("ChatViewModel", "✅ Sticker salvo no Firebase com sucesso! Path: $dbPath")
                            Log.d("ChatViewModel", "✅ Dados salvos: messageType=${message.messageType}, imageUrl=${message.imageUrl}")
                            
                            // Atualizar status para SENT
                            updateMessageStatus(channelID, message.id, MessageStatus.SENT)
                            
                            // Simular DELIVERED após um pequeno delay
                            viewModelScope.launch {
                                kotlinx.coroutines.delay(1000)
                                updateMessageStatus(channelID, message.id, MessageStatus.DELIVERED)
                            }
                            
                            // Atualizar notificações e última mensagem
                            if (!isPrivate) {
                                postNotificationToUsers(channelID, message.senderName, getMessageTypeDescription(MessageType.STICKER))
                                // Atualizar última mensagem do grupo
                                updateGroupLastMessage(channelID, getMessageTypeDescription(MessageType.STICKER), message.senderName)
                            } else {
                                // Para chats privados, notificar apenas o outro usuário
                                postNotificationToPrivateChat(channelID, message.senderName, getMessageTypeDescription(MessageType.STICKER))
                                // Atualizar último mensagem no chat privado
                                updatePrivateChatLastMessage(channelID, getMessageTypeDescription(MessageType.STICKER))
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("ChatViewModel", "❌ ERRO ao salvar sticker no Firebase: ${e.message}")
                            e.printStackTrace()
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("ChatViewModel", "Erro ao buscar dados do usuário para sticker: ${e.message}")
                }
        }
    }

    private fun sendVideoMessageWithInfo(channelID: String, videoUrl: String, fileName: String, fileSize: Long) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            db.reference.child("users").child(currentUser.uid).get()
                .addOnSuccessListener { userSnapshot ->
                    val userProfile = userSnapshot.getValue(UserProfile::class.java)
                    val senderImage = userProfile?.profileImageUrl
                    
                    val message = Message(
                        id = db.reference.push().key ?: UUID.randomUUID().toString(),
                        senderId = currentUser.uid,
                        message = null,
                        createdAt = System.currentTimeMillis(),
                        senderName = currentUser.displayName ?: userProfile?.name ?: "",
                        senderImage = senderImage,
                        videoUrl = videoUrl,
                        fileName = fileName,
                        fileSize = fileSize,
                        messageType = MessageType.VIDEO.name,
                        status = MessageStatus.SENDING.name
                    )

                    val messagesRef = if (isPrivateChat(channelID)) {
                        db.reference.child("private_messages").child(channelID)
                    } else {
                        db.reference.child("messages").child(channelID)
                    }
                    
                    messagesRef.push().setValue(message)
                        .addOnSuccessListener {
                            Log.d("ChatViewModel", "Mensagem de vídeo salva com sucesso no Firebase")
                            
                            // Atualizar status para SENT
                            updateMessageStatus(channelID, message.id, MessageStatus.SENT)
                            
                            // Simular DELIVERED após um pequeno delay
                            viewModelScope.launch {
                                kotlinx.coroutines.delay(1000)
                                updateMessageStatus(channelID, message.id, MessageStatus.DELIVERED)
                            }
                            
                            // Atualizar notificações e última mensagem
                            if (!isPrivateChat(channelID)) {
                                postNotificationToUsers(channelID, message.senderName, getMessageTypeDescription(MessageType.VIDEO))
                                // Atualizar última mensagem do grupo
                                updateGroupLastMessage(channelID, getMessageTypeDescription(MessageType.VIDEO), message.senderName)
                            } else {
                                // Para chats privados, notificar apenas o outro usuário
                                postNotificationToPrivateChat(channelID, message.senderName, getMessageTypeDescription(MessageType.VIDEO))
                                // Atualizar último mensagem no chat privado
                                updatePrivateChatLastMessage(channelID, getMessageTypeDescription(MessageType.VIDEO))
                            }
                        }
                        .addOnFailureListener { error ->
                            Log.e("ChatViewModel", "Erro ao salvar mensagem de vídeo: ${error.message}")
                        }
                }
        }
    }

    private fun sendAudioMessageWithInfo(channelID: String, audioUrl: String, fileName: String, fileSize: Long, duration: Long?) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            db.reference.child("users").child(currentUser.uid).get()
                .addOnSuccessListener { userSnapshot ->
                    val userProfile = userSnapshot.getValue(UserProfile::class.java)
                    val senderImage = userProfile?.profileImageUrl
                    
                    val message = Message(
                        id = db.reference.push().key ?: UUID.randomUUID().toString(),
                        senderId = currentUser.uid,
                        message = null,
                        createdAt = System.currentTimeMillis(),
                        senderName = currentUser.displayName ?: userProfile?.name ?: "",
                        senderImage = senderImage,
                        audioUrl = audioUrl,
                        fileName = fileName,
                        fileSize = fileSize,
                        audioDuration = duration,
                        messageType = MessageType.AUDIO.name,
                        status = MessageStatus.SENDING.name
                    )

                    Log.d("ChatViewModel", "Criando mensagem de áudio: $message")

                    val messagesRef = if (isPrivateChat(channelID)) {
                        db.reference.child("private_messages").child(channelID)
                    } else {
                        db.reference.child("messages").child(channelID)
                    }
                    
                    messagesRef.push().setValue(message)
                        .addOnSuccessListener {
                            Log.d("ChatViewModel", "Mensagem de áudio salva com sucesso no Firebase")
                            
                            // Atualizar status para SENT
                            updateMessageStatus(channelID, message.id, MessageStatus.SENT)
                            
                            // Simular DELIVERED após um pequeno delay
                            viewModelScope.launch {
                                kotlinx.coroutines.delay(1000)
                                updateMessageStatus(channelID, message.id, MessageStatus.DELIVERED)
                            }
                            
                            // Atualizar notificações e última mensagem
                            if (!isPrivateChat(channelID)) {
                                postNotificationToUsers(channelID, message.senderName, getMessageTypeDescription(MessageType.AUDIO))
                                // Atualizar última mensagem do grupo
                                updateGroupLastMessage(channelID, getMessageTypeDescription(MessageType.AUDIO), message.senderName)
                            } else {
                                // Para chats privados, notificar apenas o outro usuário
                                postNotificationToPrivateChat(channelID, message.senderName, getMessageTypeDescription(MessageType.AUDIO))
                                // Atualizar último mensagem no chat privado
                                updatePrivateChatLastMessage(channelID, getMessageTypeDescription(MessageType.AUDIO))
                            }
                        }
                        .addOnFailureListener { error ->
                            Log.e("ChatViewModel", "Erro ao salvar mensagem de áudio: ${error.message}")
                        }
                }
        }
    }

    private fun sendFileMessageWithInfo(channelID: String, fileUrl: String, fileName: String, fileSize: Long) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            db.reference.child("users").child(currentUser.uid).get()
                .addOnSuccessListener { userSnapshot ->
                    val userProfile = userSnapshot.getValue(UserProfile::class.java)
                    val senderImage = userProfile?.profileImageUrl
                    
                    val message = Message(
                        id = db.reference.push().key ?: UUID.randomUUID().toString(),
                        senderId = currentUser.uid,
                        message = null,
                        createdAt = System.currentTimeMillis(),
                        senderName = currentUser.displayName ?: userProfile?.name ?: "",
                        senderImage = senderImage,
                        fileUrl = fileUrl,
                        fileName = fileName,
                        fileSize = fileSize,
                        messageType = MessageType.FILE.name,
                        status = MessageStatus.SENDING.name
                    )

                    val messagesRef = if (isPrivateChat(channelID)) {
                        db.reference.child("private_messages").child(channelID)
                    } else {
                        db.reference.child("messages").child(channelID)
                    }
                    
                    messagesRef.push().setValue(message)
                        .addOnSuccessListener {
                            Log.d("ChatViewModel", "Mensagem de arquivo salva com sucesso no Firebase")
                            
                            // Atualizar status para SENT
                            updateMessageStatus(channelID, message.id, MessageStatus.SENT)
                            
                            // Simular DELIVERED após um pequeno delay
                            viewModelScope.launch {
                                kotlinx.coroutines.delay(1000)
                                updateMessageStatus(channelID, message.id, MessageStatus.DELIVERED)
                            }
                            
                            // Atualizar notificações e última mensagem
                            if (!isPrivateChat(channelID)) {
                                postNotificationToUsers(channelID, message.senderName, getMessageTypeDescription(MessageType.FILE))
                                // Atualizar última mensagem do grupo
                                updateGroupLastMessage(channelID, getMessageTypeDescription(MessageType.FILE), message.senderName)
                            } else {
                                // Para chats privados, notificar apenas o outro usuário
                                postNotificationToPrivateChat(channelID, message.senderName, getMessageTypeDescription(MessageType.FILE))
                                // Atualizar último mensagem no chat privado
                                updatePrivateChatLastMessage(channelID, getMessageTypeDescription(MessageType.FILE))
                            }
                        }
                        .addOnFailureListener { error ->
                            Log.e("ChatViewModel", "Erro ao salvar mensagem de arquivo: ${error.message}")
                        }
                }
        }
    }

    private fun getMessageTypeDescription(messageType: MessageType): String {
        return when (messageType) {
            MessageType.IMAGE -> "Imagem"
            MessageType.VIDEO -> "Vídeo"
            MessageType.AUDIO -> "Áudio"
            MessageType.FILE -> "Arquivo"
            MessageType.EMOJI -> "Emoji"
            MessageType.STICKER -> "Sticker"
            else -> "Mensagem"
        }
    }

    // ===== FUNCIONALIDADES DE MENSAGENS FIXADAS =====
    
    private val _pinnedMessage = MutableStateFlow<Message?>(null)
    val pinnedMessage = _pinnedMessage.asStateFlow()

    fun pinMessage(channelID: String, message: Message) {
        val currentUser = Firebase.auth.currentUser ?: return
        
        val messagesRef = if (isPrivateChat(channelID)) {
            db.reference.child("private_messages").child(channelID)
        } else {
            db.reference.child("messages").child(channelID)
        }
        
        // Primeiro, desfixa qualquer mensagem já fixada
        unpinCurrentMessage(channelID) {
            // Depois fixa a nova mensagem
            val updatedMessage = message.copy(
                isPinned = true,
                pinnedAt = System.currentTimeMillis(),
                pinnedBy = currentUser.uid
            )
            
            messagesRef.orderByChild("id").equalTo(message.id).addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        snapshot.children.firstOrNull()?.let { messageSnapshot ->
                            messageSnapshot.ref.setValue(updatedMessage)
                                .addOnSuccessListener {
                                    _pinnedMessage.value = updatedMessage
                                    Log.d("ChatViewModel", "Mensagem fixada com sucesso: ${message.id}")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("ChatViewModel", "Erro ao fixar mensagem: ${e.message}")
                                }
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("ChatViewModel", "Erro ao buscar mensagem para fixar: ${error.message}")
                    }
                }
            )
        }
    }

    fun unpinMessage(channelID: String, message: Message) {
        val messagesRef = if (isPrivateChat(channelID)) {
            db.reference.child("private_messages").child(channelID)
        } else {
            db.reference.child("messages").child(channelID)
        }
        
        val updatedMessage = message.copy(
            isPinned = false,
            pinnedAt = null,
            pinnedBy = null
        )
        
        messagesRef.orderByChild("id").equalTo(message.id).addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.firstOrNull()?.let { messageSnapshot ->
                        messageSnapshot.ref.setValue(updatedMessage)
                            .addOnSuccessListener {
                                _pinnedMessage.value = null
                                Log.d("ChatViewModel", "Mensagem desfixada com sucesso: ${message.id}")
                            }
                            .addOnFailureListener { e ->
                                Log.e("ChatViewModel", "Erro ao desafixar mensagem: ${e.message}")
                            }
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatViewModel", "Erro ao buscar mensagem para desafixar: ${error.message}")
                }
            }
        )
    }
    
    private fun unpinCurrentMessage(channelID: String, onComplete: () -> Unit) {
        val messagesRef = if (isPrivateChat(channelID)) {
            db.reference.child("private_messages").child(channelID)
        } else {
            db.reference.child("messages").child(channelID)
        }
        
        messagesRef.orderByChild("isPinned").equalTo(true).addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        snapshot.children.forEach { messageSnapshot ->
                            val message = messageSnapshot.getValue(Message::class.java)
                            message?.let {
                                val updatedMessage = it.copy(
                                    isPinned = false,
                                    pinnedAt = null,
                                    pinnedBy = null
                                )
                                messageSnapshot.ref.setValue(updatedMessage)
                            }
                        }
                    }
                    onComplete()
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatViewModel", "Erro ao desafixar mensagem atual: ${error.message}")
                    onComplete()
                }
            }
        )
    }
    
    private fun loadPinnedMessage(channelID: String) {
        val messagesRef = if (isPrivateChat(channelID)) {
            db.reference.child("private_messages").child(channelID)
        } else {
            db.reference.child("messages").child(channelID)
        }
        
        messagesRef.orderByChild("isPinned").equalTo(true).addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val pinnedMessage = snapshot.children.firstOrNull()?.getValue(Message::class.java)
                    _pinnedMessage.value = pinnedMessage
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatViewModel", "Erro ao carregar mensagem fixada: ${error.message}")
                }
            }
        )
    }

    fun listenForMessages(channelID: String) {
        val isPrivate = isPrivateChat(channelID)
        Log.d("ChatViewModel", "📋 listenForMessages - channelID: $channelID, isPrivateChat: $isPrivate")
        
        // Carregar mensagem fixada
        loadPinnedMessage(channelID)
        
        val messagesRef = if (isPrivate) {
            val path = "private_messages/$channelID"
            Log.d("ChatViewModel", "📋 Carregando mensagens PRIVADAS de: $path")
            db.getReference("private_messages").child(channelID).orderByChild("createdAt")
        } else {
            val path = "messages/$channelID"
            Log.d("ChatViewModel", "📋 Carregando mensagens de GRUPO de: $path")
            db.getReference("messages").child(channelID).orderByChild("createdAt")
        }
        
        messagesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("ChatViewModel", "📥 onDataChange chamado - snapshot existe: ${snapshot.exists()}")
                    Log.d("ChatViewModel", "📥 Quantidade de filhos no snapshot: ${snapshot.childrenCount}")
                    
                    val list = mutableListOf<Message>()
                    snapshot.children.forEach { data ->
                        Log.d("ChatViewModel", "📥 Processando snapshot child: ${data.key}")
                        val message = data.getValue(Message::class.java)
                        message?.let {
                            Log.d("ChatViewModel", "📥 Mensagem deserializada: type=${it.messageType}, sender=${it.senderName}")
                            
                            // Se a mensagem não tem status definido, definir como DELIVERED por padrão
                            val messageWithStatus = if (it.status.isEmpty()) {
                                it.copy(status = MessageStatus.DELIVERED.name)
                            } else {
                                it
                            }
                            list.add(messageWithStatus)
                            
                            // Log para mensagens de áudio
                            if (it.messageType == MessageType.AUDIO.name) {
                                Log.d("ChatViewModel", "🎵 Mensagem de áudio carregada: audioUrl=${it.audioUrl}, fileName=${it.fileName}")
                            }
                            
                            // Log para stickers
                            if (it.messageType == MessageType.STICKER.name) {
                                Log.d("ChatViewModel", "🎨 STICKER CARREGADO: imageUrl=${it.imageUrl}, sender=${it.senderName}")
                            }
                            
                            // Log para emojis
                            if (it.messageType == MessageType.EMOJI.name) {
                                Log.d("ChatViewModel", "😊 Emoji carregado: message=${it.message}, sender=${it.senderName}")
                            }
                        } ?: Log.e("ChatViewModel", "❌ ERRO: Falha ao deserializar mensagem do snapshot: ${data.key}")
                    }
                    Log.d("ChatViewModel", "Total de mensagens carregadas: ${list.size}")
                    _allMessages.value = list
                    filterMessages() // Aplicar filtro após carregar mensagens
                    
                    // Marcar mensagens como lidas quando carregadas (simula visualização)
                    markUnreadMessagesAsRead(channelID, list)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        subscribeForNotification(channelID)
        registerUserIdtoChannel(channelID)
    }

    fun getAllUserEmails(channelID: String, callback: (List<String>) -> Unit) {
        val ref = db.reference.child("channels").child(channelID).child("users")
        val userIds = mutableListOf<String>()
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    userIds.add(it.value.toString())
                }
                callback.invoke(userIds)
            }

            override fun onCancelled(error: DatabaseError) {
                callback.invoke(emptyList())
            }
        })
    }

    fun registerUserIdtoChannel(channelID: String) {
        val currentUser = Firebase.auth.currentUser
        val ref = db.reference.child("channels").child(channelID).child("users")
        ref.child(currentUser?.uid ?: "").addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        ref.child(currentUser?.uid ?: "").setValue(currentUser?.email)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            }
        )

    }

    private fun subscribeForNotification(channelID: String) {
        FirebaseMessaging.getInstance().subscribeToTopic("group_$channelID")
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d("ChatViewModel", "Subscribed to topic: group_$channelID")
                } else {
                    Log.d("ChatViewModel", "Failed to subscribe to topic: group_$channelID")
                    // Handle failure
                }
            }
    }

    private fun postNotificationToUsers(
        channelID: String,
        senderName: String,
        messageContent: String
    ) {
        val fcmUrl = "https://fcm.googleapis.com/v1/projects/chatter-bbd0d/messages:send"
        val jsonBody = JSONObject().apply {
            put("message", JSONObject().apply {
                put("topic", "group_$channelID")
                put("notification", JSONObject().apply {
                    put("title", "New message in $channelID")
                    put("body", "$senderName: $messageContent")
                })
            })
        }

        val requestBody = jsonBody.toString()

        val request = object : StringRequest(Method.POST, fcmUrl, Response.Listener {
            Log.d("ChatViewModel", "Notification sent successfully")
        }, Response.ErrorListener {
            Log.e("ChatViewModel", "Failed to send notification")
        }) {
            override fun getBody(): ByteArray {
                return requestBody.toByteArray()
            }

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer ${getAccessToken()}"
                headers["Content-Type"] = "application/json"
                return headers
            }
        }
        val queue = Volley.newRequestQueue(context)
        queue.add(request)
    }

    private fun getAccessToken(): String {
        val inputStream = context.resources.openRawResource(R.raw.chatapp_key)
        val googleCreds = GoogleCredentials.fromStream(inputStream)
            .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
        return googleCreds.refreshAccessToken().tokenValue
    }

    private fun isPrivateChat(channelID: String): Boolean {
        return channelID.contains("_") && channelID.split("_").size == 2
    }

    private fun postNotificationToPrivateChat(channelID: String, senderName: String, message: String) {
        val currentUser = Firebase.auth.currentUser ?: return
        val userIds = channelID.split("_")
        val otherUserId = if (userIds[0] == currentUser.uid) userIds[1] else userIds[0]
        
        // Para chats privados, apenas enviamos notificação para o outro usuário
        // Aqui você pode implementar notificações push personalizadas se necessário
        Log.d("ChatViewModel", "Private chat notification to user: $otherUserId")
    }

    private fun updatePrivateChatLastMessage(channelID: String, message: String) {
        val updates = mapOf(
            "lastMessage" to message,
            "lastMessageTime" to System.currentTimeMillis()
        )
        
        db.reference.child("private_chats").child(channelID).updateChildren(updates)
    }

    private fun updateGroupLastMessage(channelID: String, message: String, senderName: String) {
        val updates = mapOf(
            "lastMessage" to message,
            "lastMessageSender" to senderName,
            "lastMessageTime" to System.currentTimeMillis()
        )
        
        db.reference.child("groups").child(channelID).updateChildren(updates)
    }

    fun getOtherUserIdFromPrivateChat(channelID: String): String? {
        val currentUser = Firebase.auth.currentUser ?: return null
        if (!isPrivateChat(channelID)) return null
        
        val userIds = channelID.split("_")
        return if (userIds[0] == currentUser.uid) userIds[1] else userIds[0]
    }

    fun isPrivateChatPublic(channelID: String): Boolean {
        return isPrivateChat(channelID)
    }

    fun updateMessageStatus(channelID: String, messageId: String, status: MessageStatus) {
        val messagesRef = if (isPrivateChat(channelID)) {
            db.reference.child("private_messages").child(channelID)
        } else {
            db.reference.child("messages").child(channelID)
        }
        
        messagesRef.orderByChild("id").equalTo(messageId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { messageSnapshot ->
                    messageSnapshot.ref.child("status").setValue(status.name)
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatViewModel", "Erro ao atualizar status da mensagem", error.toException())
            }
        })
    }

    fun markMessageAsRead(channelID: String, messageId: String) {
        val currentUser = Firebase.auth.currentUser ?: return
        val messagesRef = if (isPrivateChat(channelID)) {
            db.reference.child("private_messages").child(channelID)
        } else {
            db.reference.child("messages").child(channelID)
        }
        
        messagesRef.orderByChild("id").equalTo(messageId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { messageSnapshot ->
                    val readByData = messageSnapshot.child("readBy").getValue()
                    val readByMap = if (readByData is Map<*, *>) {
                        readByData as Map<String, Long>
                    } else {
                        emptyMap<String, Long>()
                    }
                    
                    val updatedReadBy = readByMap.toMutableMap()
                    updatedReadBy[currentUser.uid] = System.currentTimeMillis()
                    
                    messageSnapshot.ref.child("readBy").setValue(updatedReadBy)
                    messageSnapshot.ref.child("status").setValue(MessageStatus.READ.name)
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatViewModel", "Erro ao marcar mensagem como lida", error.toException())
            }
        })
    }

    fun toggleSearch() {
        _isSearchActive.value = !_isSearchActive.value
        if (!_isSearchActive.value) {
            clearSearch()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterMessages()
    }

    private fun filterMessages() {
        val query = _searchQuery.value.trim()
        if (query.isEmpty()) {
            _messages.value = _allMessages.value
        } else {
            val filteredMessages = _allMessages.value.filter { message ->
                message.message?.contains(query, ignoreCase = true) == true ||
                message.senderName.contains(query, ignoreCase = true) ||
                message.fileName?.contains(query, ignoreCase = true) == true ||
                // Incluir mensagens de áudio, vídeo, arquivos, stickers e emojis na busca
                (message.messageType == MessageType.AUDIO.name && "áudio".contains(query, ignoreCase = true)) ||
                (message.messageType == MessageType.VIDEO.name && "vídeo".contains(query, ignoreCase = true)) ||
                (message.messageType == MessageType.FILE.name && "arquivo".contains(query, ignoreCase = true)) ||
                (message.messageType == MessageType.STICKER.name && "sticker".contains(query, ignoreCase = true)) ||
                (message.messageType == MessageType.EMOJI.name && "emoji".contains(query, ignoreCase = true)) ||
                // Incluir mensagens que só têm conteúdo visual (stickers, emojis, imagens)
                (message.messageType == MessageType.STICKER.name && message.imageUrl != null) ||
                (message.messageType == MessageType.EMOJI.name && message.message != null) ||
                (message.messageType == MessageType.IMAGE.name && "imagem".contains(query, ignoreCase = true))
            }
            _messages.value = filteredMessages
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _messages.value = _allMessages.value
    }

    private fun markUnreadMessagesAsRead(channelID: String, messages: List<Message>) {
        val currentUser = Firebase.auth.currentUser ?: return
        
        // Marcar apenas mensagens de outros usuários como lidas
        messages.filter { message ->
            message.senderId != currentUser.uid && 
            message.status != MessageStatus.READ.name &&
            !message.readBy.containsKey(currentUser.uid)
        }.forEach { message ->
            // Simular um delay para parecer mais realístico
            viewModelScope.launch {
                kotlinx.coroutines.delay((500..1500).random().toLong()) // Delay aleatório
                markMessageAsRead(channelID, message.id)
            }
        }
    }
    
    fun loadGroupData(groupId: String) {
        db.reference.child("groups").child(groupId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val group = snapshot.getValue(Group::class.java)?.copy(id = groupId)
                    _currentGroup.value = group
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatViewModel", "Erro ao carregar grupo: ${error.message}")
                }
            })
    }

}