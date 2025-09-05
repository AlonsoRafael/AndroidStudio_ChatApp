package com.example.chatapp.feature.groups

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.chatapp.R
import com.example.chatapp.model.Contact
import com.example.chatapp.model.Group
import com.example.chatapp.ui.component.DefaultAvatar
import com.example.chatapp.ui.theme.Blue
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupProfileScreen(
    navController: NavController,
    groupId: String,
    viewModel: GroupsViewModel = hiltViewModel()
) {
    val currentUser = Firebase.auth.currentUser
    val group by viewModel.currentGroup.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    
    var showAddMembersDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoMenu by remember { mutableStateOf(false) }
    
    // Launcher para seleção de imagem
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        uri?.let {
            group?.let { currentGroup ->
                viewModel.updateGroup(
                    groupId = currentGroup.id,
                    name = currentGroup.name,
                    description = currentGroup.description,
                    imageUri = it,
                    onComplete = { success, error ->
                        if (!success) {
                            // Handle error if needed
                        }
                    }
                )
            }
        }
    }
    
    LaunchedEffect(groupId) {
        viewModel.loadGroup(groupId)
        viewModel.loadContacts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil do Grupo") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    // Mostrar botão de edição para todos os membros do grupo
                    if (group?.participants?.containsKey(currentUser?.uid) == true) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar grupo")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        group?.let { g ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cabeçalho do grupo
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Foto do grupo
                            Box(
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                if (g.imageUrl != null && g.imageUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = g.imageUrl,
                                        contentDescription = "Foto do grupo",
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
                                
                                // Botão de editar foto (visível apenas para membros do grupo)
                                if (g.participants.containsKey(currentUser?.uid)) {
                                    Box {
                                        IconButton(
                                            onClick = { 
                                                if (g.imageUrl != null) {
                                                    showPhotoMenu = true
                                                } else {
                                                    imagePickerLauncher.launch("image/*")
                                                }
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Blue, CircleShape)
                                        ) {
                                            Icon(
                                                if (g.imageUrl != null) Icons.Default.MoreVert else Icons.Default.Add,
                                                contentDescription = if (g.imageUrl != null) "Opções da foto" else "Alterar foto do grupo",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        
                                        // Menu dropdown para foto existente
                                        DropdownMenu(
                                            expanded = showPhotoMenu,
                                            onDismissRequest = { showPhotoMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Alterar foto") },
                                                onClick = {
                                                    showPhotoMenu = false
                                                    imagePickerLauncher.launch("image/*")
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.Default.Add, contentDescription = null)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Remover foto") },
                                                onClick = {
                                                    showPhotoMenu = false
                                                    viewModel.updateGroup(
                                                        groupId = g.id,
                                                        name = g.name,
                                                        description = g.description,
                                                        removeImage = true,
                                                        onComplete = { success, error ->
                                                            if (!success) {
                                                                // Handle error if needed
                                                            }
                                                        }
                                                    )
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.Default.Delete, contentDescription = null)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Nome do grupo
                            Text(
                                text = g.name,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            
                            // Descrição do grupo
                            if (g.description.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = g.description,
                                    fontSize = 16.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Informações do grupo
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.1f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Membros: ${g.participants.size}",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "Criado em: ${java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(g.createdAt))}",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Ações do grupo (para todos os membros)
                if (g.participants.containsKey(currentUser?.uid)) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Ações",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                
                                // Adicionar membros
                                Button(
                                    onClick = { showAddMembersDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Blue)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Adicionar Membros")
                                }
                                
                                // Deletar grupo (apenas para o criador)
                                if (g.createdBy == currentUser?.uid) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    OutlinedButton(
                                        onClick = { showDeleteConfirmation = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color.Red
                                        )
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Deletar Grupo")
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Lista de membros
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Membros (${g.participants.size})",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }
                }
                
                // Lista de participantes
                items(g.participants.entries.toList()) { (userId, userName) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .let { modifier ->
                                // Só permite clique se não for o próprio usuário
                                if (userId != currentUser?.uid) {
                                    modifier.clickable {
                                        // Navegar para o perfil do usuário
                                        navController.navigate("user-profile/$userId")
                                    }
                                } else {
                                    modifier // Sem clique para o próprio usuário
                                }
                            },
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (userId == currentUser?.uid) {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) // Mais sutil para o próprio usuário
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar do membro
                            DefaultAvatar(
                                size = 48.dp,
                                backgroundColor = Color(0xFFF5F5F5)
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = userName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                // Mostrar se é admin
                                if (g.admins.contains(userId)) {
                                    Text(
                                        text = "Administrador",
                                        fontSize = 14.sp,
                                        color = Blue
                                    )
                                }
                                
                                // Mostrar "Você" se for o usuário atual
                                if (userId == currentUser?.uid) {
                                    Text(
                                        text = "Você",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            
                            // Ícone para indicar que é clicável (apenas se não for o próprio usuário)
                            if (userId != currentUser?.uid) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Ver perfil",
                                    tint = Color.Gray.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            
                            // Botão de remover (apenas para admins e não o próprio usuário)
                            if (g.admins.contains(currentUser?.uid) && userId != currentUser?.uid) {
                                IconButton(onClick = {
                                    viewModel.removeMemberFromGroup(groupId, userId)
                                }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remover membro",
                                        tint = Color.Red
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } ?: run {
            // Loading state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
    
    // Dialog para adicionar membros
    if (showAddMembersDialog) {
        AddMembersDialog(
            contacts = contacts.filter { contact -> 
                group?.participants?.containsKey(contact.uid) != true
            },
            onDismiss = { showAddMembersDialog = false },
            onAddMembers = { selectedContacts ->
                selectedContacts.forEach { contact ->
                    viewModel.addMemberToGroup(groupId, contact.uid, contact.name)
                }
                showAddMembersDialog = false
            }
        )
    }
    
    // Dialog para editar grupo
    if (showEditDialog && group != null) {
        EditGroupDialog(
            group = group!!,
            onDismiss = { showEditDialog = false },
            onSave = { name, description ->
                viewModel.updateGroup(groupId, name, description, null) { success, error ->
                    // Handle result if needed
                }
                showEditDialog = false
            }
        )
    }
    
    // Dialog para confirmar exclusão do grupo
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Deletar Grupo") },
            text = { Text("Tem certeza que deseja deletar este grupo? Esta ação não pode ser desfeita.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGroup(groupId) { success, message ->
                            if (success) {
                                navController.popBackStack()
                            }
                        }
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Deletar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun AddMembersDialog(
    contacts: List<Contact>,
    onDismiss: () -> Unit,
    onAddMembers: (List<Contact>) -> Unit
) {
    val selectedContacts = remember { mutableStateListOf<Contact>() }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Membros") },
        text = {
            LazyColumn {
                items(contacts) { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selectedContacts.contains(contact)) {
                                    selectedContacts.remove(contact)
                                } else {
                                    selectedContacts.add(contact)
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedContacts.contains(contact),
                            onCheckedChange = { checked ->
                                if (checked) {
                                    selectedContacts.add(contact)
                                } else {
                                    selectedContacts.remove(contact)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(contact.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAddMembers(selectedContacts.toList()) },
                enabled = selectedContacts.isNotEmpty()
            ) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun EditGroupDialog(
    group: Group,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(group.name) }
    var description by remember { mutableStateOf(group.description) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Grupo") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do grupo") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, description) },
                enabled = name.isNotBlank()
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
