package com.example.chatapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.chatapp.R

enum class PickerTab {
    EMOJIS, STICKERS
}

@Composable
fun EmojiPicker(
    isVisible: Boolean,
    onEmojiSelected: (String) -> Unit,
    onStickerSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(PickerTab.EMOJIS) }
    
    if (isVisible) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = modifier
                    .width(340.dp)
                    .height(450.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Emojis & Stickers",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Abas para alternar entre Emojis e Stickers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TabButton(
                            text = "Emojis",
                            icon = Icons.Default.Face,
                            isSelected = selectedTab == PickerTab.EMOJIS,
                            onClick = { selectedTab = PickerTab.EMOJIS },
                            modifier = Modifier.weight(1f)
                        )
                        
                        TabButton(
                            text = "Stickers",
                            icon = Icons.Default.Star,
                            isSelected = selectedTab == PickerTab.STICKERS,
                            onClick = { selectedTab = PickerTab.STICKERS },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Conteúdo baseado na aba selecionada
                    when (selectedTab) {
                        PickerTab.EMOJIS -> {
                            EmojiGrid(
                                onEmojiSelected = { emoji ->
                                    onEmojiSelected(emoji)
                                    onDismiss()
                                }
                            )
                        }
                        PickerTab.STICKERS -> {
                            StickerGrid(
                                onStickerSelected = { sticker ->
                                    onStickerSelected(sticker)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, fontSize = 12.sp)
    }
}

@Composable
private fun EmojiGrid(
    onEmojiSelected: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        contentPadding = PaddingValues(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(getPopularEmojis()) { emoji ->
            EmojiItem(
                emoji = emoji,
                onClick = { onEmojiSelected(emoji) }
            )
        }
    }
}

@Composable
private fun StickerGrid(
    onStickerSelected: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(getPopularStickers()) { sticker ->
            StickerItem(
                stickerUrl = sticker,
                onClick = { onStickerSelected(sticker) }
            )
        }
    }
}

@Composable
private fun EmojiItem(
    emoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StickerItem(
    stickerUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .size(80.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(stickerUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Sticker",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.ic_launcher_foreground),
            error = painterResource(R.drawable.ic_launcher_foreground)
        )
    }
}

private fun getPopularStickers(): List<String> {
    return listOf(
        // Stickers animados populares do Telegram/WhatsApp style
        "https://media.giphy.com/media/3oEjI6SIIHBdRxXI40/giphy.gif", // Coração pulsando
        "https://media.giphy.com/media/26ufdipQqU2lhNA4g/giphy.gif", // Polegar para cima
        "https://media.giphy.com/media/ZvLUtG6BZkBi0/giphy.gif", // Palmas
        "https://media.giphy.com/media/3o6fJ1BM7R2EBRDnxK/giphy.gif", // OK
        "https://media.giphy.com/media/26gsjCZpPolPr3sBy/giphy.gif", // Rindo
        "https://media.giphy.com/media/l0MYGb1LuZ3n7dRnO/giphy.gif", // Chorando de rir
        "https://media.giphy.com/media/3o7abKhOpu0NwenH3O/giphy.gif", // Beijo
        "https://media.giphy.com/media/26gspvTRJXosDwi1a/giphy.gif", // Surpreso
        "https://media.giphy.com/media/3o6fJeAiIpk5EeoC8o/giphy.gif", // Triste
        "https://media.giphy.com/media/3o6vY6C9cBJCb41g40/giphy.gif", // Pensando
        "https://media.giphy.com/media/26gsv1iextbg5Gm6Q/giphy.gif", // Dançando
        "https://media.giphy.com/media/l0HlPystfePnAI3G8/giphy.gif", // Feliz
        // Stickers estáticos como fallback
        "https://cdn.pixabay.com/photo/2016/11/21/12/42/emoticon-1845981_960_720.png",
        "https://cdn.pixabay.com/photo/2016/11/21/12/42/emoticon-1845986_960_720.png",
        "https://cdn.pixabay.com/photo/2016/11/21/12/42/emoticon-1845987_960_720.png"
    )
}

private fun getPopularEmojis(): List<String> {
    return listOf(
        "😀", "😃", "😄", "😁", "😆", "😅",
        "🤣", "😂", "🙂", "🙃", "😉", "😊",
        "😇", "🥰", "😍", "🤩", "😘", "😗",
        "😚", "😙", "😋", "😛", "😜", "🤪",
        "😝", "🤑", "🤗", "🤭", "🤫", "🤔",
        "🤐", "🤨", "😐", "😑", "😶", "😏",
        "😒", "🙄", "😬", "🤥", "😔", "😪",
        "🤤", "😴", "😷", "🤒", "🤕", "🤢",
        "🤮", "🤧", "🥵", "🥶", "🥴", "😵",
        "🤯", "🤠", "🥳", "😎", "🤓", "🧐",
        "❤️", "🧡", "💛", "💚", "💙", "💜",
        "🖤", "💔", "❣️", "💕", "💞", "💓",
        "💗", "💖", "💘", "💝", "💟", "♥️",
        "👍", "👎", "👌", "✌️", "🤞", "🤟",
        "🤘", "🤙", "👈", "👉", "👆", "🖕",
        "👇", "☝️", "👋", "🤚", "🖐", "✋",
        "🔥", "⭐", "🌟", "✨", "⚡", "💯"
    )
}
