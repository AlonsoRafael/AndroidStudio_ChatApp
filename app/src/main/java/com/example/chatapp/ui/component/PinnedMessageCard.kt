package com.example.chatapp.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.chatapp.R
import com.example.chatapp.model.Message
import com.example.chatapp.model.MessageType
import com.example.chatapp.ui.theme.Blue
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinnedMessageCard(
    message: Message,
    onUnpin: () -> Unit,
    onClick: () -> Unit,
    canUnpin: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Blue.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(0.dp, 0.dp, 12.dp, 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone de pin
            Icon(
                painter = painterResource(id = R.drawable.ic_pin),
                contentDescription = "Mensagem fixada",
                tint = Blue,
                modifier = Modifier.size(16.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Conteúdo da mensagem
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Nome do remetente e data
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.senderName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Blue
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = formatPinnedDate(message.pinnedAt ?: message.createdAt),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Conteúdo da mensagem baseado no tipo
                when (MessageType.valueOf(message.messageType)) {
                    MessageType.TEXT -> {
                        Text(
                            text = message.message ?: "",
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    MessageType.IMAGE -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            message.imageUrl?.let { imageUrl ->
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Imagem fixada",
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = "📷 Imagem",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                    MessageType.VIDEO -> {
                        Text(
                            text = "🎥 Vídeo",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    MessageType.AUDIO -> {
                        Text(
                            text = "🎵 Áudio",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    MessageType.FILE -> {
                        Text(
                            text = "📄 ${message.fileName ?: "Arquivo"}",
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    else -> {
                        Text(
                            text = "Mensagem",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // Botão de desafixar (apenas se permitido)
            if (canUnpin) {
                IconButton(
                    onClick = onUnpin,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Desafixar mensagem",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun formatPinnedDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
