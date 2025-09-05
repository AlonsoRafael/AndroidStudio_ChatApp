package com.example.chatapp.feature.groups

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.chatapp.model.Group
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.chatapp.R
import com.example.chatapp.model.Contact
import com.example.chatapp.ui.theme.Blue
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(navController: NavController) {
    val viewModel: GroupsViewModel = hiltViewModel()
    val groups by viewModel.groups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grupos") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateGroupDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Criar Grupo")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (groups.isEmpty() && !isLoading) {
            // Empty state
            EmptyGroupsState(onCreateGroup = { showCreateGroupDialog = true })
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(groups) { group ->
                    GroupItem(
                        group = group,
                        onClick = {
                            navController.navigate("group-chat/${group.id}&${group.name}")
                        },
                        onLongClick = {
                            navController.navigate("group-details/${group.id}")
                        }
                    )
                }
            }
        }
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            viewModel = viewModel,
            onDismiss = { showCreateGroupDialog = false }
        )
    }
}

@Composable
fun GroupItem(
    group: Group,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Group image
            AsyncImage(
                model = group.imageUrl ?: R.drawable.logo,
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Blue),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Group info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (group.description.isNotEmpty()) {
                    Text(
                        text = group.description,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${group.participants.size} participantes",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            // Last message time
            if (group.lastMessageTime > 0) {
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(Date(group.lastMessageTime)),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun EmptyGroupsState(onCreateGroup: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Nenhum grupo criado",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Crie grupos para conversar com múltiplas pessoas",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onCreateGroup,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Criar Grupo")
        }
    }
}

@Composable
fun CreateGroupDialog(
    viewModel: GroupsViewModel,
    onDismiss: () -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var selectedContacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    
    val contacts by viewModel.contacts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { imageUri = it }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Criar Grupo") },
        text = {
            LazyColumn(
                modifier = Modifier.height(400.dp)
            ) {
                item {
                    // Group image
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.Gray.copy(alpha = 0.3f))
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUri != null) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Adicionar imagem",
                                tint = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Group name
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Nome do Grupo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Group description
                    OutlinedTextField(
                        value = groupDescription,
                        onValueChange = { groupDescription = it },
                        label = { Text("Descrição (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Selecionar Participantes:",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Contacts list
                items(contacts) { contact ->
                    ContactSelectionItem(
                        contact = contact,
                        isSelected = selectedContacts.contains(contact),
                        onSelectionChanged = { isSelected ->
                            selectedContacts = if (isSelected) {
                                selectedContacts + contact
                            } else {
                                selectedContacts - contact
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.isNotEmpty() && selectedContacts.isNotEmpty()) {
                        viewModel.createGroup(
                            name = groupName,
                            description = groupDescription,
                            selectedContacts = selectedContacts,
                            imageUri = imageUri
                        ) { success, message ->
                            if (success) {
                                Toast.makeText(context, "Grupo criado!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, message ?: "Erro", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                enabled = groupName.isNotEmpty() && selectedContacts.isNotEmpty() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White
                    )
                } else {
                    Text("Criar")
                }
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
fun ContactSelectionItem(
    contact: Contact,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectionChanged(!isSelected) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        AsyncImage(
            model = contact.profileImageUrl ?: R.drawable.logo,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Contact info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Text(
                text = contact.email,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }

        // Selection checkbox
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selecionado",
                tint = Blue,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Blue.copy(alpha = 0.2f))
                    .padding(4.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.Gray, CircleShape)
            )
        }
    }
}
