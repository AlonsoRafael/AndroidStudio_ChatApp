package com.example.chatapp.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.chatapp.R
import com.example.chatapp.model.Group
import com.example.chatapp.ui.theme.Blue
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

@Composable
fun GroupsSection(navController: NavController, searchQuery: String = "") {
    var groups by remember { mutableStateOf<List<Group>>(emptyList()) }
    val currentUser = Firebase.auth.currentUser

    LaunchedEffect(Unit) {
        if (currentUser != null) {
            Firebase.database.reference
                .child("groups")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val groupsList = mutableListOf<Group>()
                        
                        snapshot.children.forEach { groupSnapshot ->
                            val group = groupSnapshot.getValue(Group::class.java)
                            // Mostrar apenas grupos onde o usuário é participante
                            if (group != null && group.participants.containsKey(currentUser.uid)) {
                                groupsList.add(group)
                            }
                        }
                        
                        groups = groupsList.sortedByDescending { it.lastMessageTime }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    // Filtrar grupos baseado na query de busca
    val filteredGroups = if (searchQuery.isEmpty()) {
        groups.take(5) // Mostrar apenas 5 grupos quando não há busca
    } else {
        groups.filter { group ->
            group.name.contains(searchQuery, ignoreCase = true) ||
            group.lastMessage.contains(searchQuery, ignoreCase = true) ||
            group.lastMessageSender.contains(searchQuery, ignoreCase = true)
        } // Mostrar todos os resultados filtrados quando há busca
    }

    if (filteredGroups.isNotEmpty()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Grupos",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Exibir grupos em formato de lista (comprido)
            filteredGroups.forEach { group ->
                GroupListItem(
                    group = group,
                    onClick = {
                        navController.navigate("group-chat/${group.id}&${group.name}")
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    } else if (searchQuery.isNotEmpty()) {
        // Mostrar mensagem quando há busca ativa mas nenhum grupo encontrado
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Nenhum grupo encontrado para \"$searchQuery\"",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun GroupListItem(
    group: Group,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(com.example.chatapp.ui.theme.LightGrey)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = group.imageUrl ?: R.drawable.logo,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Blue),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = group.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                
                if (group.lastMessage.isNotEmpty() && group.lastMessageSender.isNotEmpty()) {
                    Text(
                        text = "${group.lastMessageSender}: ${group.lastMessage}",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "${group.participants.size} participantes",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
            
            // Mostrar horário da última mensagem se existir
            if (group.lastMessageTime > 0) {
                Text(
                    text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(group.lastMessageTime)),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
