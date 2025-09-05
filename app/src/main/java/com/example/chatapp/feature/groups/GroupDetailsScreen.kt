package com.example.chatapp.feature.groups

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    navController: NavController,
    groupId: String
) {
    val viewModel: GroupsViewModel = hiltViewModel()
    val contacts by viewModel.contacts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var group by remember { mutableStateOf<Group?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddMembersDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showLeaveConfirmation by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val currentUser = Firebase.auth.currentUser

    // Load group details
    LaunchedEffect(groupId) {
        Firebase.database.reference.child("groups").child(groupId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    group = snapshot.getValue(Group::class.java)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes do Grupo") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    group?.let { g ->
                        // Debug logs
                        android.util.Log.d("GroupDetails", "Current user ID: ${currentUser?.uid}")
                        android.util.Log.d("GroupDetails", "Group createdBy: ${g.createdBy}")
                        android.util.Log.d("GroupDetails", "Is creator: ${g.createdBy == currentUser?.uid}")
                        
                        // Show edit button for any group member
                        if (g.participants.containsKey(currentUser?.uid)) {
                            IconButton(onClick = { showEditDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar")
                            }
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
                // Group header
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (g.imageUrl != null && g.imageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = g.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                DefaultAvatar(
                                    size = 100.dp,
                                    backgroundColor = Color(0xFFF5F5F5)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = g.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            if (g.description.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = g.description,
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "${g.participants.size} participantes",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Action buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Add members (only for admins)
                        if (g.admins.contains(currentUser?.uid)) {
                            Button(
                                onClick = { showAddMembersDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Adicionar")
                            }
                        }

                        // Leave group
                        OutlinedButton(
                            onClick = { showLeaveConfirmation = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sair")
                        }

                        // Delete group (only for creator)
                        android.util.Log.d("GroupDetails", "Checking delete permissions - Is creator: ${g.createdBy == currentUser?.uid}")
                        
                        if (g.createdBy == currentUser?.uid) {
                            android.util.Log.d("GroupDetails", "Showing delete button")
                            OutlinedButton(
                                onClick = { showDeleteConfirmation = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.Red
                                )
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Deletar")
                            }
                        } else {
                            android.util.Log.d("GroupDetails", "NOT showing delete button - not creator")
                        }
                    }
                }

                // Participants section
                item {
                    Text(
                        text = "Participantes",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Participants list
                items(g.participants.toList()) { (participantId, participantName) ->
                    ParticipantItem(
                        participantId = participantId,
                        participantName = participantName,
                        isAdmin = g.admins.contains(participantId),
                        isCreator = g.createdBy == participantId,
                        canRemove = g.admins.contains(currentUser?.uid) && 
                                   participantId != currentUser?.uid && 
                                   participantId != g.createdBy,
                        onRemove = {
                            viewModel.removeParticipant(groupId, participantId) { success, message ->
                                Toast.makeText(
                                    context,
                                    if (success) "Participante removido" else message ?: "Erro",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }
            }
        } ?: run {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    // Dialogs
    group?.let { g ->
        if (showEditDialog) {
            EditGroupDialog(
                group = g,
                viewModel = viewModel,
                onDismiss = { showEditDialog = false }
            )
        }

        if (showAddMembersDialog) {
            AddMembersDialog(
                groupId = groupId,
                currentParticipants = g.participants.keys.toList(),
                contacts = contacts,
                viewModel = viewModel,
                onDismiss = { showAddMembersDialog = false }
            )
        }

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Deletar Grupo") },
                text = { Text("Tem certeza que deseja deletar este grupo? Esta ação não pode ser desfeita.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteGroup(groupId) { success, message ->
                                if (success) {
                                    Toast.makeText(context, "Grupo deletado", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                } else {
                                    Toast.makeText(context, message ?: "Erro", Toast.LENGTH_SHORT).show()
                                }
                            }
                            showDeleteConfirmation = false
                        }
                    ) {
                        Text("Deletar", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (showLeaveConfirmation) {
            AlertDialog(
                onDismissRequest = { showLeaveConfirmation = false },
                title = { Text("Sair do Grupo") },
                text = { Text("Tem certeza que deseja sair deste grupo?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.leaveGroup(groupId) { success, message ->
                                if (success) {
                                    Toast.makeText(context, "Você saiu do grupo", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                } else {
                                    Toast.makeText(context, message ?: "Erro", Toast.LENGTH_SHORT).show()
                                }
                            }
                            showLeaveConfirmation = false
                        }
                    ) {
                        Text("Sair")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLeaveConfirmation = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun ParticipantItem(
    participantId: String,
    participantName: String,
    isAdmin: Boolean,
    isCreator: Boolean,
    canRemove: Boolean,
    onRemove: () -> Unit
) {
    var participantProfile by remember { mutableStateOf<com.example.chatapp.model.UserProfile?>(null) }

    LaunchedEffect(participantId) {
        Firebase.database.reference.child("users").child(participantId).get()
            .addOnSuccessListener { snapshot ->
                participantProfile = snapshot.getValue(com.example.chatapp.model.UserProfile::class.java)
            }
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            AsyncImage(
                model = participantProfile?.profileImageUrl ?: R.drawable.logo,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = participantName,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    if (isCreator) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Criador",
                            fontSize = 10.sp,
                            color = Blue,
                            modifier = Modifier
                                .background(
                                    Blue.copy(alpha = 0.1f),
                                    CircleShape
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    } else if (isAdmin) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admin",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            modifier = Modifier
                                .background(
                                    Color.Gray.copy(alpha = 0.1f),
                                    CircleShape
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                participantProfile?.let {
                    Text(
                        text = it.email,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            // Remove button (only for removable participants)
            if (canRemove) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remover",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}

@Composable
fun EditGroupDialog(
    group: Group,
    viewModel: GroupsViewModel,
    onDismiss: () -> Unit
) {
    var groupName by remember { mutableStateOf(group.name) }
    var groupDescription by remember { mutableStateOf(group.description) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { imageUri = it }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Grupo") },
        text = {
            Column {
                // Group image with options
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.Gray.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = when {
                                imageUri == Uri.EMPTY -> R.drawable.logo // Showing removal preview
                                imageUri != null -> imageUri // New image selected
                                else -> group.imageUrl ?: R.drawable.logo // Current or default
                            },
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Photo options (any group member can edit)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Change photo button
                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Alterar", fontSize = 12.sp)
                        }
                        
                        // Remove photo button (only if there's a custom photo or new image selected)
                        if ((group.imageUrl != null && group.imageUrl!!.isNotEmpty()) || 
                            (imageUri != null && imageUri != Uri.EMPTY)) {
                            OutlinedButton(
                                onClick = { imageUri = Uri.EMPTY }, // This will signal removal
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Remover", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Nome do Grupo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = groupDescription,
                    onValueChange = { groupDescription = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.updateGroup(
                        groupId = group.id,
                        name = groupName,
                        description = groupDescription,
                        imageUri = imageUri
                    ) { success, message ->
                        if (success) {
                            Toast.makeText(context, "Grupo atualizado!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, message ?: "Erro", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = groupName.isNotEmpty() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White
                    )
                } else {
                    Text("Salvar")
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
fun AddMembersDialog(
    groupId: String,
    currentParticipants: List<String>,
    contacts: List<Contact>,
    viewModel: GroupsViewModel,
    onDismiss: () -> Unit
) {
    var selectedContacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    val availableContacts = contacts.filter { !currentParticipants.contains(it.uid) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Membros") },
        text = {
            LazyColumn(
                modifier = Modifier.height(300.dp)
            ) {
                items(availableContacts) { contact ->
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
                    if (selectedContacts.isNotEmpty()) {
                        viewModel.addParticipants(groupId, selectedContacts) { success, message ->
                            if (success) {
                                Toast.makeText(context, "Membros adicionados!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, message ?: "Erro", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
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
