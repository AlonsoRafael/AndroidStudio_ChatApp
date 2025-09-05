package com.example.chatapp.ui.components

import android.media.MediaPlayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.io.IOException

@Composable
fun AudioPlayer(
    audioUrl: String,
    duration: Long? = null,
    modifier: Modifier = Modifier
) {
    var mediaPlayer: MediaPlayer? by remember { mutableStateOf(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var audioDuration by remember { mutableStateOf(duration ?: 0L) }
    
    // Atualizar posição do áudio
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying && mediaPlayer?.isPlaying == true) {
                currentPosition = mediaPlayer?.currentPosition?.toLong() ?: 0L
                delay(100)
            }
        }
    }
    
    // Limpar MediaPlayer quando o componente for destruído
    DisposableEffect(audioUrl) {
        onDispose {
            mediaPlayer?.release()
        }
    }
    
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.medium
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Botão Play/Pause
        IconButton(
            onClick = {
                if (isPlaying) {
                    mediaPlayer?.pause()
                    isPlaying = false
                } else {
                    if (mediaPlayer == null) {
                        try {
                            mediaPlayer = MediaPlayer().apply {
                                setDataSource(audioUrl)
                                prepareAsync()
                                setOnPreparedListener { mp ->
                                    audioDuration = mp.duration.toLong()
                                    mp.start()
                                    isPlaying = true
                                }
                                setOnCompletionListener {
                                    isPlaying = false
                                    currentPosition = 0L
                                }
                                setOnErrorListener { _, _, _ ->
                                    isPlaying = false
                                    true
                                }
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    } else {
                        mediaPlayer?.start()
                        isPlaying = true
                    }
                }
            },
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Favorite else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                tint = Color.White
            )
        }
        
        // Barra de progresso
        Column(modifier = Modifier.weight(1f)) {
            LinearProgressIndicator(
                progress = {
                    if (audioDuration > 0) {
                        (currentPosition.toFloat() / audioDuration.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(MaterialTheme.shapes.small),
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatAudioTime(currentPosition),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatAudioTime(audioDuration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatAudioTime(milliseconds: Long): String {
    val seconds = (milliseconds / 1000) % 60
    val minutes = (milliseconds / (1000 * 60)) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
