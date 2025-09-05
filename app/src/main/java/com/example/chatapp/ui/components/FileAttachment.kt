package com.example.chatapp.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun FileAttachment(
    fileUrl: String,
    fileName: String,
    fileSize: Long?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fileIcon = getFileIcon(fileName)
    val fileSizeText = fileSize?.let { formatFileSize(it) } ?: ""
    
    Card(
        modifier = modifier
            .clickable {
                // Abrir arquivo com aplicativo padrão
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(fileUrl)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Ícone do arquivo
            Icon(
                imageVector = fileIcon,
                contentDescription = "Arquivo",
                tint = getFileIconColor(fileName),
                modifier = Modifier.size(40.dp)
            )
            
            // Informações do arquivo
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (fileSizeText.isNotEmpty()) {
                    Text(
                        text = fileSizeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Ícone de download/abrir
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Abrir arquivo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun getFileIcon(fileName: String): ImageVector {
    return Icons.Default.Add // Usando um ícone simples que existe
}

private fun getFileIconColor(fileName: String): Color {
    val extension = fileName.substringAfterLast(".", "").lowercase()
    
    return when (extension) {
        "pdf" -> Color(0xFFE53E3E)
        "doc", "docx" -> Color(0xFF2B6CB0)
        "xls", "xlsx" -> Color(0xFF38A169)
        "ppt", "pptx" -> Color(0xFFD69E2E)
        "txt" -> Color(0xFF718096)
        "zip", "rar", "7z" -> Color(0xFF9F7AEA)
        "mp3", "wav", "m4a", "ogg" -> Color(0xFFE53E3E)
        "mp4", "avi", "mov", "mkv" -> Color(0xFF3182CE)
        "jpg", "jpeg", "png", "gif", "bmp" -> Color(0xFF38A169)
        else -> Color(0xFF718096)
    }
}

private fun formatFileSize(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unitIndex = 0
    
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    
    return if (size < 10 && unitIndex > 0) {
        String.format("%.1f %s", size, units[unitIndex])
    } else {
        String.format("%.0f %s", size, units[unitIndex])
    }
}
