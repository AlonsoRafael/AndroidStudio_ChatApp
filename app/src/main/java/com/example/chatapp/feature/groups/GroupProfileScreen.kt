package com.example.chatapp.feature.groups

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
                            if (g.imageUrl != null) {
                                AsyncImage(
                                    model = g.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(CircleShape)
                                        .background(Blue.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(60.dp),
                                        tint = Blue
                                    )
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
                                
                                Button(
                                    onClick = { showAddMembersDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Blue)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Adicionar Membros")
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
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Blue.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Blue
                                )
                            }
                            
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
