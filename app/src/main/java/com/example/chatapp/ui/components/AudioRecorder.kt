package com.example.chatapp.ui.components

import android.Manifest
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.io.File
import java.io.IOException

@Composable
fun AudioRecorder(
    onAudioRecorded: (Uri, Long?) -> Unit, // Adiciona duração como parâmetro
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var recordingTime by remember { mutableStateOf(0L) }
    var mediaRecorder: MediaRecorder? by remember { mutableStateOf(null) }
    var audioFile: File? by remember { mutableStateOf(null) }
    var recordedAudioUri: Uri? by remember { mutableStateOf(null) }
    
    // Animação para o botão de gravação
    val infiniteTransition = rememberInfiniteTransition(label = "recording")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ), label = "recording"
    )
    
    // Timer da gravação
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingTime = 0L
            while (isRecording) {
                delay(1000)
                recordingTime++
            }
        }
    }
    
    // Permissão para gravação de áudio
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRecording(context) { recorder, file ->
                mediaRecorder = recorder
                audioFile = file
                isRecording = true
            }
        }
    }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when {
            // Estado: Gravando áudio
            isRecording -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.Red.copy(alpha = 0.1f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Ícone de microfone
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Timer de gravação
                        Text(
                            text = formatTime(recordingTime),
                            color = Color.Red,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // Animação de ondas sonoras (simulação)
                        repeat(3) { index ->
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(12.dp + (index * 4).dp)
                                    .background(
                                        Color.Red.copy(alpha = 0.7f),
                                        RoundedCornerShape(2.dp)
                                    )
                                    .scale(if (isRecording) scale else 1f)
                            )
                            if (index < 2) Spacer(modifier = Modifier.width(2.dp))
                        }
                    }
                    
                    // Botão de parar gravação
                    IconButton(
                        onClick = {
                            stopRecording(mediaRecorder, audioFile) { uri ->
                                recordedAudioUri = uri
                                isRecording = false
                                mediaRecorder = null
                                audioFile = null
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Red, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Parar gravação",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            // Estado: Áudio gravado, aguardando confirmação
            recordedAudioUri != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Layout estilo WhatsApp para áudio gravado
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Ícone de áudio
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        // Visualização de forma de onda (simulação)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Barras simulando forma de onda
                                repeat(20) { index ->
                                    val height = (8..20).random()
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(height.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                                RoundedCornerShape(1.dp)
                                            )
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Duração do áudio
                            Text(
                                text = formatTime(recordingTime),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Botões de ação estilo WhatsApp
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Botão de cancelar
                        OutlinedButton(
                            onClick = {
                                recordedAudioUri = null
                                recordingTime = 0L
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.Red
                            ),
                            border = BorderStroke(1.dp, Color.Red)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Descartar")
                        }
                        
                        // Botão de enviar
                        Button(
                            onClick = {
                                recordedAudioUri?.let { uri ->
                                    Log.d("AudioRecorder", "Enviando áudio: $uri, duração: ${recordingTime}s")
                                    onAudioRecorded(uri, recordingTime * 1000)
                                    recordedAudioUri = null
                                    recordingTime = 0L
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enviar")
                        }
                    }
                }
            }
            
            // Estado: Pronto para gravar
            else -> {
                // Botão de iniciar gravação estilo WhatsApp
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            CircleShape
                        )
                        .clickable {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                startRecording(context) { recorder, file ->
                                    mediaRecorder = recorder
                                    audioFile = file
                                    isRecording = true
                                }
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Gravar áudio",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

private fun startRecording(
    context: Context,
    onRecordingStarted: (MediaRecorder, File) -> Unit
) {
    try {
        val audioFile = File(context.externalCacheDir, "audio_${System.currentTimeMillis()}.m4a")
        
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        
        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFile.absolutePath)
            setMaxDuration(300000) // 5 minutos max
            
            prepare()
            start()
        }
        
        onRecordingStarted(recorder, audioFile)
    } catch (e: IOException) {
        e.printStackTrace()
        Log.e("AudioRecorder", "Erro ao iniciar gravação: ${e.message}")
    } catch (e: RuntimeException) {
        e.printStackTrace()
        Log.e("AudioRecorder", "Erro de runtime na gravação: ${e.message}")
    }
}

private fun stopRecording(
    mediaRecorder: MediaRecorder?,
    audioFile: File?,
    onRecordingStopped: (Uri) -> Unit
) {
    try {
        mediaRecorder?.apply {
            stop()
            release()
        }
        
        audioFile?.let { file ->
            if (file.exists() && file.length() > 0) {
                Log.d("AudioRecorder", "Gravação salva: ${file.absolutePath}, tamanho: ${file.length()}")
                onRecordingStopped(Uri.fromFile(file))
            } else {
                Log.e("AudioRecorder", "Arquivo de áudio não existe ou está vazio")
            }
        }
    } catch (e: RuntimeException) {
        e.printStackTrace()
        Log.e("AudioRecorder", "Erro ao parar gravação: ${e.message}")
    }
}

private fun formatTime(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}
