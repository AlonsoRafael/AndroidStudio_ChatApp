package com.example.chatapp.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
            group.name.contains(searchQuery, ignoreCase = true)
        } // Mostrar todos os resultados filtrados quando há busca
    }

    if (filteredGroups.isNotEmpty()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Seus Grupos",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Text(
                    text = "Ver todos",
                    fontSize = 14.sp,
                    color = Blue,
                    modifier = Modifier.clickable {
                        navController.navigate("groups")
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredGroups) { group ->
                    GroupCard(
                        group = group,
                        onClick = {
                            navController.navigate("group-chat/${group.id}&${group.name}")
                        }
                    )
                }
            }
        }
    } else if (searchQuery.isNotEmpty()) {
        // Mostrar mensagem quando há busca ativa mas nenhum grupo encontrado
        Column(
            modifier = Modifier.padding(16.dp)
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
fun GroupCard(
    group: Group,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(100.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model = group.imageUrl ?: R.drawable.logo,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Blue),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = group.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${group.participants.size} membros",
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}
