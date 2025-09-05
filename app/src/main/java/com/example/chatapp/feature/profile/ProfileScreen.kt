package com.example.chatapp.feature.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.chatapp.R
import com.example.chatapp.model.UserStatus
import com.example.chatapp.ui.component.DefaultAvatar
import com.example.chatapp.ui.theme.Blue
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val viewModel: ProfileViewModel = hiltViewModel()
    val userProfile by viewModel.userProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()
    val context = LocalContext.current

    var showEditDialog by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.updateProfileImage(it) }
    }

    LaunchedEffect(updateStatus) {
        when (val status = updateStatus) {
            is ProfileUpdateState.Success -> {
                Toast.makeText(context, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                viewModel.clearUpdateStatus()
            }
            is ProfileUpdateState.Error -> {
                Toast.makeText(context, status.message, Toast.LENGTH_SHORT).show()
                viewModel.clearUpdateStatus()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meu Perfil") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Foto do perfil
            Box(
                contentAlignment = Alignment.BottomEnd
            ) {
                if (userProfile?.profileImageUrl != null && userProfile?.profileImageUrl!!.isNotEmpty()) {
                    AsyncImage(
                        model = userProfile?.profileImageUrl,
                        contentDescription = "Foto do perfil",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(2.dp, Blue, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    DefaultAvatar(
                        size = 120.dp,
                        backgroundColor = Color(0xFFF5F5F5)
                    )
                }
                
                IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Blue, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Alterar foto",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nome do usuário
            Text(
                text = userProfile?.name ?: "Nome não definido",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Email
            Text(
                text = userProfile?.email ?: "",
                fontSize = 16.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Status do usuário
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStatusMenu = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Status",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusIndicator(status = userProfile?.status ?: UserStatus.OFFLINE)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = userProfile?.status?.displayName ?: "Offline",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Alterar status",
                        tint = Color.Gray
                    )
                }

                DropdownMenu(
                    expanded = showStatusMenu,
                    onDismissRequest = { showStatusMenu = false }
                ) {
                    UserStatus.values().forEach { status ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StatusIndicator(status = status)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(status.displayName)
                                }
                            },
                            onClick = {
                                viewModel.updateUserStatus(status)
                                showStatusMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Informações do perfil
            ProfileInfoCard(
                title = "Informações Pessoais",
                content = {
                    ProfileInfoItem("Nome", userProfile?.name ?: "")
                    ProfileInfoItem("Bio", userProfile?.bio ?: "Adicione uma bio")
                },
                onEditClick = { showEditDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botão de logout
            Button(
                onClick = {
                    viewModel.setUserOffline()
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color.Red
                )
            ) {
                Text("Sair", color = Color.White)
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }
        }

        // Dialog de edição
        if (showEditDialog) {
            EditProfileDialog(
                currentName = userProfile?.name ?: "",
                currentBio = userProfile?.bio ?: "",
                onDismiss = { showEditDialog = false },
                onConfirm = { name, bio ->
                    viewModel.updateProfile(name, bio)
                    showEditDialog = false
                }
            )
        }
    }
}

@Composable
fun StatusIndicator(status: UserStatus) {
    val color = when (status) {
        UserStatus.ONLINE -> Color.Green
        UserStatus.OFFLINE -> Color.Gray
        UserStatus.BUSY -> Color.Red
        UserStatus.AWAY -> Color.Yellow
        UserStatus.DO_NOT_DISTURB -> Color.Red
    }
    
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(color, CircleShape)
    )
}

@Composable
fun ProfileInfoCard(
    title: String,
    content: @Composable () -> Unit,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Blue)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            content()
        }
    }
}

@Composable
fun ProfileInfoItem(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray
        )
        Text(
            text = value.ifEmpty { "Não informado" },
            fontSize = 16.sp,
            color = if (value.isEmpty()) Color.Gray else Color.Black
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun EditProfileDialog(
    currentName: String,
    currentBio: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var bio by remember { mutableStateOf(currentBio) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Perfil") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, bio) },
                enabled = name.isNotEmpty()
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
