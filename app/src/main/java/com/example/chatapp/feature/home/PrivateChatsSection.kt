package com.example.chatapp.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.chatapp.model.UserProfile
import com.example.chatapp.ui.component.UserAvatarWithStatus
import com.example.chatapp.ui.theme.LightGrey
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PrivateChat(
    val id: String = "",
    val participants: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L
)

@Composable
fun PrivateChatsSection(navController: NavController, searchQuery: String = "") {
    var privateChats by remember { mutableStateOf<List<PrivateChat>>(emptyList()) }
    var userProfiles by remember { mutableStateOf<Map<String, UserProfile>>(emptyMap()) }
    val currentUser = Firebase.auth.currentUser
    
    LaunchedEffect(Unit) {
        if (currentUser != null) {
            // Escutar chats privados do usuário atual
            Firebase.database.reference
                .child("private_chats")
                .orderByChild("lastMessageTime")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val chats = mutableListOf<PrivateChat>()
                        snapshot.children.forEach { chatSnapshot ->
                            val chat = chatSnapshot.getValue(PrivateChat::class.java)
                            if (chat != null && chat.participants.containsKey(currentUser.uid)) {
                                chats.add(chat)
                            }
                        }
                        privateChats = chats.reversed() // Mais recentes primeiro
                        
                        // Carregar perfis dos outros usuários
                        val otherUserIds = chats.flatMap { chat ->
                            chat.participants.keys.filter { it != currentUser.uid }
                        }.distinct()
                        
                        loadUserProfiles(otherUserIds) { profiles ->
                            userProfiles = profiles
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }
    
    // Filtrar chats privados baseado na query de busca
    val filteredPrivateChats = if (searchQuery.isEmpty()) {
        privateChats.take(5) // Mostrar apenas 5 chats quando não há busca
    } else {
        privateChats.filter { chat ->
            val otherUserId = chat.participants.keys.find { it != currentUser?.uid } ?: ""
            val otherUserName = chat.participants[otherUserId] ?: ""
            val otherUserProfile = userProfiles[otherUserId]
            
            // Buscar pelo nome do usuário ou pela mensagem
            otherUserName.contains(searchQuery, ignoreCase = true) ||
            otherUserProfile?.name?.contains(searchQuery, ignoreCase = true) == true ||
            chat.lastMessage.contains(searchQuery, ignoreCase = true)
        }
    }
    
    if (filteredPrivateChats.isNotEmpty()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Conversas Privadas",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            filteredPrivateChats.forEach { chat ->
                val otherUserId = chat.participants.keys.find { it != currentUser?.uid } ?: ""
                val otherUserName = chat.participants[otherUserId] ?: "Usuário"
                val otherUserProfile = userProfiles[otherUserId]
                
                PrivateChatItem(
                    chatId = chat.id,
                    otherUserId = otherUserId,
                    otherUserName = otherUserName,
                    otherUserProfile = otherUserProfile,
                    lastMessage = chat.lastMessage,
                    lastMessageTime = chat.lastMessageTime,
                    onClick = {
                        navController.navigate("chat/${chat.id}&Chat com $otherUserName")
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    } else if (searchQuery.isNotEmpty()) {
        // Mostrar mensagem quando há busca ativa mas nenhum chat encontrado
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Nenhuma conversa encontrada para \"$searchQuery\"",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun PrivateChatItem(
    chatId: String,
    otherUserId: String,
    otherUserName: String,
    otherUserProfile: UserProfile?,
    lastMessage: String,
    lastMessageTime: Long,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LightGrey)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatarWithStatus(
                userId = otherUserId,
                userName = otherUserName,
                showName = false,
                size = 48
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = otherUserProfile?.name ?: otherUserName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                
                if (lastMessage.isNotEmpty()) {
                    Text(
                        text = lastMessage,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = "Iniciar conversa...",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
            
            // Mostrar horário da última mensagem se existir
            if (lastMessageTime > 0) {
                Text(
                    text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(lastMessageTime)),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

private fun loadUserProfiles(userIds: List<String>, onComplete: (Map<String, UserProfile>) -> Unit) {
    if (userIds.isEmpty()) {
        onComplete(emptyMap())
        return
    }
    
    val profiles = mutableMapOf<String, UserProfile>()
    var loadedCount = 0
    
    userIds.forEach { userId ->
        Firebase.database.reference
            .child("users")
            .child(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.getValue(UserProfile::class.java)?.let { profile ->
                    profiles[userId] = profile
                }
                loadedCount++
                if (loadedCount == userIds.size) {
                    onComplete(profiles)
                }
            }
            .addOnFailureListener {
                loadedCount++
                if (loadedCount == userIds.size) {
                    onComplete(profiles)
                }
            }
    }
}
