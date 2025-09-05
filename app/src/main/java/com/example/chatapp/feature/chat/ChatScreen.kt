package com.example.chatapp.feature.chat

import android.Manifest
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.chatapp.R
import com.example.chatapp.feature.home.ChannelItem
import com.example.chatapp.model.Message
import com.example.chatapp.model.MessageType
import com.example.chatapp.ui.theme.Blue
import com.example.chatapp.ui.theme.DarkGrey
import com.example.chatapp.ui.theme.LightGrey
import com.example.chatapp.ui.components.MessageStatusIcon
import com.example.chatapp.ui.components.HighlightedText
import com.example.chatapp.ui.components.AudioRecorder
import com.example.chatapp.ui.components.AudioPlayer
import com.example.chatapp.ui.components.VideoPlayer
import com.example.chatapp.ui.components.FileAttachment
import com.example.chatapp.ui.components.EmojiPicker
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton
import com.zegocloud.uikit.service.defines.ZegoUIKitUser
import org.jetbrains.annotations.Async
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(navController: NavController, channelId: String, channelName: String, isGroup: Boolean = false) {
    Scaffold(
        containerColor = Color.White
    ) {
        val viewModel: ChatViewModel = hiltViewModel()
        val attachmentDialog = remember { mutableStateOf(false) }
        val emojiPickerVisible = remember { mutableStateOf(false) }

        val cameraImageUri = remember { mutableStateOf<Uri?>(null) }

        // Launchers para diferentes tipos de mídia
        val cameraImageLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                cameraImageUri.value?.let {
                    viewModel.sendImageMessage(it, channelId)
                }
            }
        }

        val imageLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { viewModel.sendImageMessage(it, channelId) }
        }

        val videoLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { viewModel.sendVideoMessage(it, channelId) }
        }

        val fileLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { viewModel.sendFileMessage(it, channelId) }
        }

        fun createImageUri(): Uri {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = ContextCompat.getExternalFilesDirs(
                navController.context, Environment.DIRECTORY_PICTURES
            ).first()
            return FileProvider.getUriForFile(navController.context,
                "${navController.context.packageName}.provider",
                File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
                    cameraImageUri.value = Uri.fromFile(this)
                })
        }

        val permissionLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    cameraImageLauncher.launch(createImageUri())
                }
            }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            LaunchedEffect(key1 = true) {
                viewModel.listenForMessages(channelId)
            }
            val messages = viewModel.message.collectAsState()
            ChatMessages(
                messages = messages.value,
                onSendMessage = { message ->
                    viewModel.sendMessage(channelId, message)
                },
                onAttachmentClicked = {
                    attachmentDialog.value = true
                },
                onEmojiClicked = {
                    emojiPickerVisible.value = true
                },
                onAudioRecorded = { audioUri, duration ->
                    viewModel.sendAudioMessage(audioUri, channelId, duration)
                },
                channelName = channelName,
                viewModel = viewModel,
                channelID = channelId,
                navController = navController,
                isGroup = isGroup
            )
        }

        // Dialog de seleção de anexos
        if (attachmentDialog.value) {
            AttachmentSelectionDialog(
                onCameraSelected = {
                    attachmentDialog.value = false
                    if (navController.context.checkSelfPermission(Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        cameraImageLauncher.launch(createImageUri())
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onGallerySelected = {
                    attachmentDialog.value = false
                    imageLauncher.launch("image/*")
                },
                onVideoSelected = {
                    attachmentDialog.value = false
                    videoLauncher.launch("video/*")
                },
                onFileSelected = {
                    attachmentDialog.value = false
                    fileLauncher.launch("*/*")
                },
                onDismiss = {
                    attachmentDialog.value = false
                }
            )
        }

        // Picker de emojis e stickers
        EmojiPicker(
            isVisible = emojiPickerVisible.value,
            onEmojiSelected = { emoji ->
                viewModel.sendMessage(channelId, emoji, messageType = MessageType.EMOJI)
            },
            onStickerSelected = { stickerUrl ->
                viewModel.sendStickerMessage(stickerUrl, channelId)
            },
            onDismiss = {
                emojiPickerVisible.value = false
            }
        )
    }
}


@Composable
fun AttachmentSelectionDialog(
    onCameraSelected: () -> Unit,
    onGallerySelected: () -> Unit,
    onVideoSelected: () -> Unit,
    onFileSelected: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Selecionar anexo") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Câmera
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onCameraSelected() }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.attach),
                            contentDescription = "Câmera",
                            modifier = Modifier.size(48.dp)
                        )
                        Text("Câmera", fontSize = MaterialTheme.typography.bodySmall.fontSize)
                    }
                    
                    // Galeria
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onGallerySelected() }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.attach),
                            contentDescription = "Galeria",
                            modifier = Modifier.size(48.dp)
                        )
                        Text("Galeria", fontSize = MaterialTheme.typography.bodySmall.fontSize)
                    }
                }
                
                Spacer(modifier = Modifier.padding(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Vídeo
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onVideoSelected() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Vídeo",
                            modifier = Modifier.size(48.dp)
                        )
                        Text("Vídeo", fontSize = MaterialTheme.typography.bodySmall.fontSize)
                    }
                    
                    // Arquivo
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onFileSelected() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Arquivo",
                            modifier = Modifier.size(48.dp)
                        )
                        Text("Arquivo", fontSize = MaterialTheme.typography.bodySmall.fontSize)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ChatMessages(
    channelName: String,
    channelID: String,
    messages: List<Message>,
    onSendMessage: (String) -> Unit,
    onAttachmentClicked: () -> Unit,
    onEmojiClicked: () -> Unit,
    onAudioRecorded: (Uri, Long?) -> Unit,
    viewModel: ChatViewModel,
    navController: NavController,
    isGroup: Boolean = false
) {
    val hideKeyboardController = LocalSoftwareKeyboardController.current
    val lazyListState = remember { LazyListState() }
    
    val msg = remember {
        mutableStateOf("")
    }
    
    // Scroll automático para a última mensagem quando novas mensagens chegarem
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size)
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // TOP BAR FIXA - Sempre visível no topo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            ChannelItem(
                channelName = channelName, 
                Modifier, 
                true, 
                onClick = {},
                onCall = { callButton->
                    viewModel.getAllUserEmails(channelID) {
                        val list: MutableList<ZegoUIKitUser> = mutableListOf()
                        it.forEach { email ->
                            Firebase.auth.currentUser?.email?.let { em ->
                                if(email != em){
                                    list.add(
                                        ZegoUIKitUser(
                                            email, email
                                        )
                                    )
                                }
                            }
                        }
                        callButton.setInvitees(list)
                    }
                },
                onProfileClick = if (isGroup) {
                    {
                        // Para grupos, navegar para o perfil do grupo
                        Log.d("ChatScreen", "Clique no perfil do grupo! ChannelID: $channelID")
                        navController.navigate("group-profile/$channelID")
                    }
                } else if (viewModel.isPrivateChatPublic(channelID)) {
                    {
                        // Para chats privados, navegar para o perfil do outro usuário
                        Log.d("ChatScreen", "Clique na topbar! ChannelID: $channelID")
                        viewModel.getOtherUserIdFromPrivateChat(channelID)?.let { otherUserId ->
                            Log.d("ChatScreen", "Navegando para perfil do usuário: $otherUserId")
                            navController.navigate("user-profile/$otherUserId")
                        }
                    }
                } else null // Para chats de canal público, não navegar para perfil
            )
        }
        
        // Barra de busca (condicional) - também fixa
        val isSearchActive = viewModel.isSearchActive.collectAsState()
        val searchQuery = viewModel.searchQuery.collectAsState()
        
        if (isSearchActive.value) {
            SearchBar(
                query = searchQuery.value,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                onClose = { viewModel.toggleSearch() }
            )
        }

        // MENSAGENS - Área rolável
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = lazyListState
        ) {
            items(messages) { message ->
                ChatBubble(
                    message = message, 
                    navController = navController,
                    searchQuery = viewModel.searchQuery.collectAsState().value
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LightGrey)
                .padding(8.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botão de busca
            IconButton(onClick = {
                viewModel.toggleSearch()
            }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar mensagens",
                    tint = Color.Blue
                )
            }
            
            // Botão de anexo
            IconButton(onClick = {
                msg.value = ""
                onAttachmentClicked()
            }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Anexar arquivo",
                    tint = Color.Blue
                )
            }

            // Campo de texto
            TextField(
                value = msg.value,
                onValueChange = { msg.value = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(text = "Digite uma mensagem") },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    hideKeyboardController?.hide()
                }),
                colors = TextFieldDefaults.colors().copy(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedPlaceholderColor = Color.Gray,
                    unfocusedPlaceholderColor = Color.Gray
                )
            )
            
            // Botão de emoji
            IconButton(onClick = onEmojiClicked) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "Emojis",
                    tint = Color.Blue
                )
            }
            
            // Gravador de áudio
            AudioRecorder(
                onAudioRecorded = { audioUri, duration ->
                    onAudioRecorded(audioUri, duration)
                }
            )
            
            // Botão de enviar
            IconButton(onClick = {
                if (msg.value.isNotBlank()) {
                    onSendMessage(msg.value)
                    msg.value = ""
                }
            }) {
                Image(painter = painterResource(id = R.drawable.send), contentDescription = "send")
            }
        }
    }
}

@Composable
fun ChatBubble(message: Message, navController: NavController, searchQuery: String = "") {
    // Log para debug de renderização
    Log.d("ChatScreen", "🎨 Renderizando mensagem: type=${message.messageType}, sender=${message.senderName}")
    if (message.messageType == MessageType.STICKER.name) {
        Log.d("ChatScreen", "🎨 RENDERIZANDO STICKER: imageUrl=${message.imageUrl}")
    }
    
    val isCurrentUser = message.senderId == Firebase.auth.currentUser?.uid
    val bubbleColor = if (isCurrentUser) {
        Blue
    } else {
        LightGrey
    }
    val textColor = if (isCurrentUser) {
        Color.White
    } else {
        Color.Black
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        val alignment = if (!isCurrentUser) Alignment.CenterStart else Alignment.CenterEnd
        
        if (!isCurrentUser) {
            // Layout para mensagens de outros usuários (foto à esquerda)
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .align(alignment),
                verticalAlignment = Alignment.Top
            ) {
                // Foto do usuário (separada e clicável)
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            Log.d("ChatBubble", "Foto clicada! SenderId: ${message.senderId}")
                            navController.navigate("user-profile/${message.senderId}")
                        }
                ) {
                    if (message.senderImage != null) {
                        AsyncImage(
                            model = message.senderImage,
                            contentDescription = "Perfil de ${message.senderName}",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Perfil de ${message.senderName}",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                        )
                    }
                }
                
                // Conteúdo da mensagem
                Column {
                    Box(
                        modifier = Modifier
                            .background(
                                color = bubbleColor, 
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(16.dp)
                    ) {
                        MessageContent(message = message, searchQuery = searchQuery, textColor = textColor)
                    }
                    
                    // Status da mensagem (para mensagens de outros usuários também)
                    MessageStatusIcon(
                        status = message.status,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp)
                    )
                }
            }
        } else {
            // Layout para mensagens do usuário atual (apenas bubble à direita)
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .align(alignment),
                horizontalAlignment = Alignment.End
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = bubbleColor, 
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                ) {
                    MessageContent(message = message, searchQuery = searchQuery, textColor = textColor)
                }
                
                // Status da mensagem (apenas para mensagens do usuário atual)
                MessageStatusIcon(
                    status = message.status,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun MessageContent(
    message: Message,
    searchQuery: String = "",
    textColor: Color = Color.Black
) {
    when (MessageType.valueOf(message.messageType)) {
        MessageType.TEXT -> {
            HighlightedText(
                text = message.message?.trim() ?: "",
                searchQuery = searchQuery,
                textColor = textColor
            )
        }
        MessageType.IMAGE -> {
            message.imageUrl?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Imagem",
                    modifier = Modifier.size(200.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }
        MessageType.VIDEO -> {
            message.videoUrl?.let { videoUrl ->
                VideoPlayer(
                    videoUrl = videoUrl,
                    modifier = Modifier.width(250.dp),
                    showControls = true
                )
            }
        }
        MessageType.AUDIO -> {
            Log.d("ChatScreen", "Renderizando mensagem de áudio: ${message.audioUrl}")
            message.audioUrl?.let { audioUrl ->
                AudioPlayer(
                    audioUrl = audioUrl,
                    duration = message.audioDuration,
                    modifier = Modifier.width(200.dp)
                )
            } ?: run {
                Log.e("ChatScreen", "audioUrl é null para mensagem de áudio")
            }
        }
        MessageType.FILE -> {
            message.fileUrl?.let { fileUrl ->
                FileAttachment(
                    fileUrl = fileUrl,
                    fileName = message.fileName ?: "Arquivo",
                    fileSize = message.fileSize,
                    modifier = Modifier.width(250.dp)
                )
            }
        }
        MessageType.EMOJI -> {
            Text(
                text = message.message ?: "",
                fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                color = textColor
            )
        }
        MessageType.STICKER -> {
            Log.d("ChatScreen", "🎨 Renderizando STICKER component: imageUrl=${message.imageUrl}")
            // Para stickers, você pode implementar lógica similar às imagens
            message.imageUrl?.let { stickerUrl ->
                Log.d("ChatScreen", "🎨 Carregando AsyncImage do sticker: $stickerUrl")
                AsyncImage(
                    model = stickerUrl,
                    contentDescription = "Sticker",
                    modifier = Modifier.size(120.dp),
                    contentScale = ContentScale.Fit,
                    onSuccess = {
                        Log.d("ChatScreen", "✅ Sticker carregado com sucesso!")
                    },
                    onError = { error ->
                        Log.e("ChatScreen", "❌ Erro ao carregar sticker: $error")
                    }
                )
            } ?: Log.e("ChatScreen", "❌ ERRO: Sticker sem imageUrl!")
        }
    }
}

@Composable
fun CallButton(isVideoCall: Boolean, onClick: (ZegoSendCallInvitationButton) -> Unit) {
    AndroidView(factory = { context ->
        val button = ZegoSendCallInvitationButton(context)
        button.setIsVideoCall(isVideoCall)
        button.resourceID = "zego_data"
        button
    }, modifier = Modifier.size(50.dp)) { zegoCallButton ->
        zegoCallButton.setOnClickListener { _ -> onClick(zegoCallButton) }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(text = "Buscar mensagens...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar"
                )
            },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Limpar"
                        )
                    }
                }
            } else null,
            singleLine = true,
            colors = TextFieldDefaults.colors().copy(
                focusedContainerColor = Color.Gray.copy(alpha = 0.1f),
                unfocusedContainerColor = Color.Gray.copy(alpha = 0.1f)
            )
        )
        
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Fechar busca"
            )
        }
    }
}