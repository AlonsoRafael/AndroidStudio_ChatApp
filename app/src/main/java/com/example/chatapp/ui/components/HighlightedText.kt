package com.example.chatapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun HighlightedText(
    text: String,
    searchQuery: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    if (searchQuery.isEmpty()) {
        Text(
            text = text,
            color = textColor,
            modifier = modifier
        )
    } else {
        val annotatedText = buildAnnotatedString {
            val lowercaseText = text.lowercase()
            val lowercaseQuery = searchQuery.lowercase()
            var lastIndex = 0
            
            while (lastIndex < text.length) {
                val index = lowercaseText.indexOf(lowercaseQuery, lastIndex)
                if (index == -1) {
                    // Adicionar resto do texto sem destaque
                    append(text.substring(lastIndex))
                    break
                } else {
                    // Adicionar texto antes do match
                    append(text.substring(lastIndex, index))
                    
                    // Adicionar texto com destaque
                    withStyle(
                        style = SpanStyle(
                            background = Color.Yellow.copy(alpha = 0.7f),
                            color = Color.Black
                        )
                    ) {
                        append(text.substring(index, index + searchQuery.length))
                    }
                    
                    lastIndex = index + searchQuery.length
                }
            }
        }
        
        Text(
            text = annotatedText,
            color = textColor,
            modifier = modifier
        )
    }
}
