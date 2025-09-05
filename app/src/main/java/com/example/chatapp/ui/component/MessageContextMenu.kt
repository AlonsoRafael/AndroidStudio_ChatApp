package com.example.chatapp.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.chatapp.R
import com.example.chatapp.model.Message

@Composable
fun MessageContextMenu(
    message: Message,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
    canPin: Boolean = true,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = isVisible,
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        if (canPin) {
            if (message.isPinned) {
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_pin),
                            contentDescription = "Desafixar mensagem"
                        )
                    },
                    text = { Text("Desafixar mensagem") },
                    onClick = {
                        onUnpin()
                        onDismiss()
                    }
                )
            } else {
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_pin),
                            contentDescription = "Fixar mensagem"
                        )
                    },
                    text = { Text("Fixar mensagem") },
                    onClick = {
                        onPin()
                        onDismiss()
                    }
                )
            }
        }
    }
}
