package com.example.chatapp.feature.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.chatapp.R
import com.example.chatapp.model.UserProfile
import com.example.chatapp.model.UserStatus
import com.example.chatapp.ui.theme.Blue
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileViewScreen(navController: NavController, userId: String) {
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    
    LaunchedEffect(userId) {
        com.google.firebase.Firebase.database.reference
            .child("users")
            .child(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userProfile = snapshot.getValue(UserProfile::class.java)
                }
                
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(userProfile?.name ?: "Perfil do Usuário") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Foto do perfil
            if (userProfile?.profileImageUrl != null) {
                AsyncImage(
                    model = userProfile?.profileImageUrl,
                    contentDescription = "Foto do perfil",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(2.dp, Blue, CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_person),
                    contentDescription = "Foto do perfil",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(2.dp, Blue, CircleShape)
                        .background(Color.LightGray, CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nome do usuário
            Text(
                text = userProfile?.name ?: "Nome não definido",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Email
            Text(
                text = userProfile?.email ?: "",
                fontSize = 16.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Status do usuário
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusIndicator(status = userProfile?.status ?: UserStatus.OFFLINE)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = userProfile?.status?.displayName ?: "Offline",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bio
            if (!userProfile?.bio.isNullOrEmpty()) {
                Text(
                    text = "Bio",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = userProfile?.bio ?: "",
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Última vez online (se offline)
            if (userProfile?.status == UserStatus.OFFLINE) {
                val lastSeenText = userProfile?.lastSeen?.let { timestamp ->
                    val date = Date(timestamp)
                    val formatter = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault())
                    "Visto por último em ${formatter.format(date)}"
                } ?: "Offline"
                
                Text(
                    text = lastSeenText,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Botão para iniciar conversa
            Button(
                onClick = {
                    createPrivateChat(userId, userProfile?.name ?: "Usuario") { channelId ->
                        navController.navigate("chat/${channelId}&Chat com ${userProfile?.name ?: "Usuario"}")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Mensagem",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Iniciar Conversa",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}

private fun createPrivateChat(otherUserId: String, otherUserName: String, onChannelCreated: (String) -> Unit) {
    val currentUser = Firebase.auth.currentUser ?: return
    val currentUserId = currentUser.uid
    
    // Criar um ID único para o chat privado (ordenado para ser consistente)
    val chatId = if (currentUserId < otherUserId) {
        "${currentUserId}_${otherUserId}"
    } else {
        "${otherUserId}_${currentUserId}"
    }
    
    val database = Firebase.database
    val chatRef = database.reference.child("private_chats").child(chatId)
    
    // Verificar se o chat já existe
    chatRef.get().addOnSuccessListener { snapshot ->
        if (snapshot.exists()) {
            // Chat já existe, apenas navegar
            onChannelCreated(chatId)
        } else {
            // Criar novo chat privado
            val participants = mapOf(
                currentUserId to (currentUser.displayName ?: currentUser.email ?: "Usuario"),
                otherUserId to otherUserName
            )
            
            val chatData = mapOf(
                "id" to chatId,
                "participants" to participants,
                "createdAt" to System.currentTimeMillis(),
                "lastMessage" to "",
                "lastMessageTime" to System.currentTimeMillis()
            )
            
            chatRef.setValue(chatData).addOnSuccessListener {
                onChannelCreated(chatId)
            }.addOnFailureListener {
                // Em caso de erro, ainda tenta navegar com o ID
                onChannelCreated(chatId)
            }
        }
    }.addOnFailureListener {
        // Em caso de erro, ainda tenta navegar com o ID
        onChannelCreated(chatId)
    }
}
