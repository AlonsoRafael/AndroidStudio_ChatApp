package com.example.chatapp.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.chatapp.model.UserProfile
import com.example.chatapp.model.UserStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

@Composable
fun UserAvatarWithStatus(
    userId: String,
    userName: String,
    showName: Boolean = true,
    size: Int = 40,
    onClick: (() -> Unit)? = null
) {
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
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    ) {
        Box {
            // Avatar
            if (userProfile?.profileImageUrl != null) {
                AsyncImage(
                    model = userProfile?.profileImageUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(size.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.LightGray, CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(size.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.firstOrNull()?.toString()?.uppercase() ?: "?",
                        color = Color.White,
                        fontSize = (size / 2).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Status indicator
            val statusColor = when (userProfile?.status) {
                UserStatus.ONLINE -> Color.Green
                UserStatus.BUSY -> Color.Red
                UserStatus.AWAY -> Color.Yellow
                UserStatus.DO_NOT_DISTURB -> Color.Red
                else -> Color.Gray
            }
            
            Box(
                modifier = Modifier
                    .size((size / 3).dp)
                    .clip(CircleShape)
                    .background(statusColor)
                    .border(2.dp, Color.White, CircleShape)
                    .align(Alignment.BottomEnd)
            )
        }
        
        if (showName) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = userProfile?.name ?: userName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
