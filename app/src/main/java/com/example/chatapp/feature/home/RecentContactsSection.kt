package com.example.chatapp.feature.home

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
import com.example.chatapp.model.Contact
import com.example.chatapp.ui.theme.Blue
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

@Composable
fun RecentContactsSection(navController: NavController) {
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    val currentUser = Firebase.auth.currentUser

    LaunchedEffect(Unit) {
        if (currentUser != null) {
            Firebase.database.reference
                .child("user_contacts")
                .child(currentUser.uid)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val contactsList = mutableListOf<Contact>()
                        
                        snapshot.children.forEach { contactSnapshot ->
                            val contact = contactSnapshot.getValue(Contact::class.java)
                            contact?.let { contactsList.add(it) }
                        }
                        
                        contacts = contactsList.sortedByDescending { it.addedAt }.take(5) // Mostrar apenas 5 contatos recentes
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    if (contacts.isNotEmpty()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Contatos Recentes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Text(
                    text = "Ver todos",
                    fontSize = 14.sp,
                    color = Blue,
                    modifier = Modifier.clickable {
                        navController.navigate("contacts")
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(contacts) { contact ->
                    ContactCard(
                        contact = contact,
                        onClick = {
                            navController.navigate("user-profile/${contact.uid}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ContactCard(
    contact: Contact,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(120.dp)
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
                model = contact.profileImageUrl ?: R.drawable.logo,
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = contact.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
