package com.example.chatapp.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun VideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    showControls: Boolean = true
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
            setMediaItem(mediaItem)
            prepare()
        }
    }
    
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .clip(MaterialTheme.shapes.medium)
    ) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = showControls
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        if (!showControls) {
            // Overlay personalizado para controle básico
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                            isPlaying = false
                        } else {
                            exoPlayer.play()
                            isPlaying = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!isPlaying && !exoPlayer.isPlaying) {
                    IconButton(
                        onClick = {
                            exoPlayer.play()
                            isPlaying = true
                        },
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                Color.Black.copy(alpha = 0.6f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Reproduzir vídeo",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
    
    // Atualizar estado de reprodução
    LaunchedEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        
        // Cleanup
        // Removido onDispose aqui pois já está sendo usado acima
    }
}
