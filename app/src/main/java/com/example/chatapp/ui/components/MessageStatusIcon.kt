package com.example.chatapp.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.chatapp.model.MessageStatus

@Composable
fun MessageStatusIcon(status: String, modifier: Modifier = Modifier) {
    val messageStatus = try {
        MessageStatus.valueOf(status)
    } catch (e: IllegalArgumentException) {
        MessageStatus.SENDING
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WhatsAppStatusIcon(
            status = messageStatus.name,
            modifier = Modifier
        )
    }
}
