package com.example.chatapp.feature.contacts

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
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
import com.example.chatapp.ui.theme.Blue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(navController: NavController) {
    val viewModel: ContactsViewModel = hiltViewModel()
    val contacts by viewModel.contacts.collectAsState()
    val deviceContacts by viewModel.deviceContacts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showImportContactsDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    var connectionStatus by remember { mutableStateOf("Verificando conexão...") }
    var isConnected by remember { mutableStateOf(true) }
    
    val context = LocalContext.current

    // Verificar conectividade Firebase
    LaunchedEffect(Unit) {
        val networkUtils = com.example.chatapp.utils.NetworkUtils(context)
        networkUtils.testFirebaseConnection { connected, message ->
            connectionStatus = if (connected) "Conectado" else "Desconectado"
            isConnected = connected
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contatos") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddContactDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Adicionar Contato")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Meus Contatos") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { 
                        selectedTabIndex = 1
                        viewModel.importContactsFromDevice()
                    },
                    text = { Text("Do Dispositivo") }
                )
            }

            // Content
            when (selectedTabIndex) {
                0 -> {
                    // Meus Contatos
                    if (contacts.isEmpty() && !isLoading) {
                        EmptyContactsState(
                            onAddContact = { showAddContactDialog = true },
                            onImportContacts = { showImportContactsDialog = true }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(contacts) { contact ->
                                ContactItem(
                                    contact = contact,
                                    onContactClick = { 
                                        navController.navigate("user-profile/${contact.uid}")
                                    },
                                    onDeleteContact = {
                                        viewModel.removeContact(contact.uid) { success ->
                                            if (success) {
                                                Toast.makeText(context, "Contato removido", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Contatos do Dispositivo
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (deviceContacts.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhum contato encontrado no dispositivo",
                                color = Color.Gray
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(deviceContacts) { contact ->
                                DeviceContactItem(
                                    contact = contact,
                                    onAddContact = {
                                        viewModel.addDeviceContactToApp(contact) { success, message ->
                                            Toast.makeText(
                                                context,
                                                if (success) "Contato adicionado!" else message ?: "Erro",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog para adicionar contato por email
    if (showAddContactDialog) {
        AddContactDialog(
            onDismiss = { showAddContactDialog = false },
            onAddContact = { email ->
                viewModel.addContactByEmail(email) { success, message ->
                    Toast.makeText(
                        context,
                        if (success) "Contato adicionado!" else message ?: "Erro",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                showAddContactDialog = false
            }
        )
    }
}

@Composable
fun ContactItem(
    contact: Contact,
    onContactClick: () -> Unit,
    onDeleteContact: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onContactClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            AsyncImage(
                model = contact.profileImageUrl ?: R.drawable.logo,
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = contact.email,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            // Delete button
            IconButton(onClick = onDeleteContact) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remover",
                    tint = Color.Gray
                )
            }
        }
    }
}

@Composable
fun DeviceContactItem(
    contact: Contact,
    onAddContact: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Default Avatar
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.firstOrNull()?.toString() ?: "?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = contact.email,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                if (contact.phoneNumber.isNotEmpty()) {
                    Text(
                        text = contact.phoneNumber,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            // Add button
            Button(
                onClick = onAddContact,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Adicionar")
            }
        }
    }
}

@Composable
fun EmptyContactsState(
    onAddContact: () -> Unit,
    onImportContacts: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Email,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Nenhum contato adicionado",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Adicione contatos para começar a conversar",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAddContact,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Adicionar por Email")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onImportContacts,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Phone, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Importar do Dispositivo")
        }
    }
}

@Composable
fun AddContactDialog(
    onDismiss: () -> Unit,
    onAddContact: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Contato") },
        text = {
            Column {
                Text("Digite o email do usuário que deseja adicionar:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAddContact(email.trim()) },
                enabled = email.trim().isNotEmpty()
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
