package com.example.chatapp.feature.chat

import android.Manifest
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.chatapp.R
import com.example.chatapp.model.Message
import com.example.chatapp.model.MessageType
import com.example.chatapp.ui.component.PinnedMessageCard
import com.example.chatapp.ui.component.MessageContextMenu
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
import com.example.chatapp.ui.component.UserAvatarWithStatus
import com.example.chatapp.ui.component.DefaultAvatar
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton
import com.zegocloud.uikit.service.defines.ZegoUIKitUser
import kotlinx.coroutines.delay
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController, channelId: String, channelName: String, isGroup: Boolean = false) {
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

    Scaffold(
        topBar = {
            ModernChatTopBar(
                channelName = channelName,
                channelId = channelId,
                isGroup = isGroup,
                viewModel = viewModel,
                navController = navController,
                onBackClick = { navController.navigateUp() }
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LaunchedEffect(key1 = true) {
                viewModel.listenForMessages(channelId)
                if (isGroup) {
                    viewModel.loadGroupData(channelId)
                }
            }
            
            val messages = viewModel.message.collectAsState()
            val pinnedMessage = viewModel.pinnedMessage.collectAsState()
            
            // Mensagem fixada (se existir)
            pinnedMessage.value?.let { pinned ->
                PinnedMessageCard(
                    message = pinned,
                    onUnpin = { viewModel.unpinMessage(channelId, pinned) },
                    onClick = {
                        // Scroll para a mensagem fixada na lista
                        // Implementar se necessário
                    },
                    canUnpin = true
                )
            }
            
            ChatMessages(
                messages = messages.value,
                channelId = channelId,
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
                viewModel = viewModel,
                navController = navController
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernChatTopBar(
    channelName: String,
    channelId: String,
    isGroup: Boolean,
    viewModel: ChatViewModel,
    navController: NavController,
    onBackClick: () -> Unit
) {
    val isSearchActive = viewModel.isSearchActive.collectAsState()
    val searchQuery = viewModel.searchQuery.collectAsState()
    val currentGroup = viewModel.currentGroup.collectAsState()
    
    if (isSearchActive.value) {
        // Barra de busca ativa
        TopAppBar(
            title = {
                TextField(
                    value = searchQuery.value,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Buscar mensagens...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar"
                        )
                    },
                    trailingIcon = if (searchQuery.value.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Limpar"
                                )
                            }
                        }
                    } else null,
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            navigationIcon = {
                IconButton(onClick = { viewModel.toggleSearch() }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Fechar busca"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
    } else {
        // TopBar normal
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        if (isGroup) {
                            navController.navigate("group-profile/$channelId")
                        } else if (viewModel.isPrivateChatPublic(channelId)) {
                            viewModel.getOtherUserIdFromPrivateChat(channelId)?.let { otherUserId ->
                                navController.navigate("user-profile/$otherUserId")
                            }
                        }
                    }
                ) {
                    // Avatar do canal/usuário
                    if (!isGroup) {
                        // Para chat privado, mostrar avatar do outro usuário
                        viewModel.getOtherUserIdFromPrivateChat(channelId)?.let { otherUserId ->
                            UserAvatarWithStatus(
                                userId = otherUserId,
                                userName = channelName.removePrefix("Chat com "),
                                showName = false,
                                size = 40
                            )
                        } ?: run {
                            // Fallback se não conseguir obter o ID do usuário
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0055FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = channelName.removePrefix("Chat com ").firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        // Para grupo, mostrar foto real do grupo
                        val group = currentGroup.value
                        if (group?.imageUrl != null && group.imageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = group.imageUrl,
                                contentDescription = "Foto do grupo",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Fallback para avatar com logo
                            DefaultAvatar(
                                size = 40.dp,
                                backgroundColor = Color(0xFFF5F5F5)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = channelName.removePrefix("Chat com "),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isGroup) {
                            Text(
                                text = "Grupo",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            actions = {
                // Botão de busca
                IconButton(onClick = { viewModel.toggleSearch() }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar mensagens",
                        tint = Color(0xFF0055FF)
                    )
                }
                
                // Botão de chamada de voz
                CallButton(isVideoCall = false) { callButton ->
                    viewModel.getAllUserEmails(channelId) { emails ->
                        val list: MutableList<ZegoUIKitUser> = mutableListOf()
                        emails.forEach { email ->
                            Firebase.auth.currentUser?.email?.let { currentEmail ->
                                if (email != currentEmail) {
                                    list.add(ZegoUIKitUser(email, email))
                                }
                            }
                        }
                        callButton.setInvitees(list)
                    }
                }
                
                // Botão de videochamada
                CallButton(isVideoCall = true) { callButton ->
                    viewModel.getAllUserEmails(channelId) { emails ->
                        val list: MutableList<ZegoUIKitUser> = mutableListOf()
                        emails.forEach { email ->
                            Firebase.auth.currentUser?.email?.let { currentEmail ->
                                if (email != currentEmail) {
                                    list.add(ZegoUIKitUser(email, email))
                                }
                            }
                        }
                        callButton.setInvitees(list)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface
            )
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
                            painter = painterResource(id = R.drawable.ic_camera),
                            contentDescription = "Câmera",
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF0055FF)
                        )
                        Text("Câmera", fontSize = MaterialTheme.typography.bodySmall.fontSize)
                    }
                    
                    // Galeria
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onGallerySelected() }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_image),
                            contentDescription = "Galeria",
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF0055FF)
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
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF0055FF)
                        )
                        Text("Vídeo", fontSize = MaterialTheme.typography.bodySmall.fontSize)
                    }
                    
                    // Arquivo
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onFileSelected() }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.attach),
                            contentDescription = "Arquivo",
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF0055FF)
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
    messages: List<Message>,
    channelId: String,
    onSendMessage: (String) -> Unit,
    onAttachmentClicked: () -> Unit,
    onEmojiClicked: () -> Unit,
    onAudioRecorded: (Uri, Long?) -> Unit,
    viewModel: ChatViewModel,
    navController: NavController
) {
    val hideKeyboardController = LocalSoftwareKeyboardController.current
    val lazyListState = remember { LazyListState() }
    val msg = remember { mutableStateOf("") }
    
    // Scroll automático para a última mensagem quando novas mensagens chegarem
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size)
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // MENSAGENS - Área rolável
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = lazyListState,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(messages) { message ->
                MessageWithContextMenu(
                    message = message,
                    navController = navController,
                    searchQuery = viewModel.searchQuery.collectAsState().value,
                    onPinMessage = { viewModel.pinMessage(channelId, message) },
                    onUnpinMessage = { viewModel.unpinMessage(channelId, message) },
                    canPin = true // ou baseado em permissões do usuário
                )
            }
        }
        
        // BARRA DE INPUT MODERNA - Fixa na parte inferior
        ModernInputBar(
            message = msg.value,
            onMessageChange = { msg.value = it },
            onSendMessage = {
                if (msg.value.isNotBlank()) {
                    onSendMessage(msg.value)
                    msg.value = ""
                }
            },
            onAttachmentClicked = onAttachmentClicked,
            onEmojiClicked = onEmojiClicked,
            onAudioRecorded = onAudioRecorded,
            hideKeyboardController = hideKeyboardController
        )
    }
}

@Composable
fun ModernInputBar(
    message: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onAttachmentClicked: () -> Unit,
    onEmojiClicked: () -> Unit,
    onAudioRecorded: (Uri, Long?) -> Unit,
    hideKeyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Botão de anexo
            IconButton(
                onClick = onAttachmentClicked,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Color.Gray.copy(alpha = 0.1f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Anexar arquivo",
                    tint = Color(0xFF0055FF)
                )
            }

            // Área central - Campo de texto OU UI de gravação
            ModernInputContent(
                message = message,
                onMessageChange = onMessageChange,
                onSendMessage = onSendMessage,
                onEmojiClicked = onEmojiClicked,
                onAudioRecorded = onAudioRecorded,
                hideKeyboardController = hideKeyboardController,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ModernInputContent(
    message: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onEmojiClicked: () -> Unit,
    onAudioRecorded: (Uri, Long?) -> Unit,
    hideKeyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var recordingTime by remember { mutableStateOf(0L) }
    var mediaRecorder: MediaRecorder? by remember { mutableStateOf(null) }
    var audioFile: File? by remember { mutableStateOf(null) }
    
    // Animação para o botão de gravação
    val infiniteTransition = rememberInfiniteTransition(label = "recording")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
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

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isRecording) {
            // UI de gravação compacta
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        Color.Red.copy(alpha = 0.1f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Ícone de microfone pulsante
                    Icon(
                        painter = painterResource(id = R.drawable.ic_microphone),
                        contentDescription = "Gravando",
                        tint = Color.Red,
                        modifier = Modifier
                            .size(16.dp)
                            .scale(scale)
                    )
                    
                    // Timer
                    Text(
                        text = formatAudioTime(recordingTime),
                        color = Color.Red,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Ondas sonoras animadas
                    repeat(3) { 
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height((8..12).random().dp)
                                .background(
                                    Color.Red.copy(alpha = 0.7f),
                                    RoundedCornerShape(1.dp)
                                )
                                .scale(if (isRecording) scale else 1f)
                        )
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    // Botão cancelar
                    IconButton(
                        onClick = {
                            stopAudioRecording(mediaRecorder, null) { }
                            isRecording = false
                            mediaRecorder = null
                            audioFile = null
                            recordingTime = 0L
                        },
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.Red.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancelar",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    // Botão parar e enviar
                    IconButton(
                        onClick = {
                            stopAudioRecording(mediaRecorder, audioFile) { uri ->
                                onAudioRecorded(uri, recordingTime * 1000)
                                isRecording = false
                                mediaRecorder = null
                                audioFile = null
                                recordingTime = 0L
                            }
                        },
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFF0055FF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Parar e enviar",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else {
            // Campo de texto normal
            TextField(
                value = message,
                onValueChange = onMessageChange,
                modifier = Modifier.weight(1f),
                placeholder = { 
                    Text(
                        text = "Digite uma mensagem...",
                        color = Color.Gray.copy(alpha = 0.6f)
                    ) 
                },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (message.isNotBlank()) {
                            onSendMessage()
                        }
                        hideKeyboardController?.hide()
                    }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Gray.copy(alpha = 0.1f),
                    unfocusedContainerColor = Color.Gray.copy(alpha = 0.05f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                shape = RoundedCornerShape(20.dp),
                maxLines = 3
            )
            
            // Botão de emoji
            IconButton(
                onClick = onEmojiClicked,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Color.Gray.copy(alpha = 0.1f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "Emojis",
                    tint = Color(0xFF0055FF)
                )
            }
        }
        
        // Gravador de áudio ou botão de enviar
        if (message.isBlank() && !isRecording) {
            IconButton(
                onClick = {
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
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Color.Gray.copy(alpha = 0.1f),
                        CircleShape
                    )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_microphone),
                    contentDescription = "Gravar áudio",
                    tint = Color(0xFF0055FF),
                    modifier = Modifier.size(20.dp)
                )
            }
        } else if (message.isNotBlank()) {
            IconButton(
                onClick = onSendMessage,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF0055FF), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun MessageWithContextMenu(
    message: Message,
    navController: NavController,
    searchQuery: String = "",
    onPinMessage: () -> Unit,
    onUnpinMessage: () -> Unit,
    canPin: Boolean = true
) {
    var showContextMenu by remember { mutableStateOf(false) }
    
    Box {
        ChatBubble(
            message = message,
            navController = navController,
            searchQuery = searchQuery,
            onLongClick = { showContextMenu = true }
        )
        
        MessageContextMenu(
            message = message,
            isVisible = showContextMenu,
            onDismiss = { showContextMenu = false },
            onPin = onPinMessage,
            onUnpin = onUnpinMessage,
            canPin = canPin
        )
    }
}

@Composable
fun ChatBubble(message: Message, navController: NavController, searchQuery: String = "", onLongClick: (() -> Unit)? = null) {
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
            .let { modifier ->
                if (onLongClick != null) {
                    modifier.pointerInput(message.id) {
                        detectTapGestures(
                            onLongPress = { onLongClick() }
                        )
                    }
                } else {
                    modifier
                }
            }
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
    AndroidView(
        factory = { context ->
            val button = ZegoSendCallInvitationButton(context)
            button.setIsVideoCall(isVideoCall)
            button.resourceID = "zego_data"
            
            // Usar ícones padrão do ZEGO (originais)
            // Removemos a personalização para voltar aos ícones originais
            
            button
        },
        modifier = Modifier.size(36.dp)
    ) { zegoCallButton ->
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
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Gray.copy(alpha = 0.1f),
                unfocusedContainerColor = Color.Gray.copy(alpha = 0.1f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
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
        Log.d("AudioRecorder", "Gravação iniciada: ${audioFile.absolutePath}")
    } catch (e: IOException) {
        e.printStackTrace()
        Log.e("AudioRecorder", "Erro ao iniciar gravação: ${e.message}")
    } catch (e: RuntimeException) {
        e.printStackTrace()
        Log.e("AudioRecorder", "Erro de runtime na gravação: ${e.message}")
    }
}

private fun stopAudioRecording(
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

private fun formatAudioTime(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}