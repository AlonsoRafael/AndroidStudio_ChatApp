package com.example.chatapp.feature.groups

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.SupabaseStorageUtils
import com.example.chatapp.model.Contact
import com.example.chatapp.model.Group
import com.example.chatapp.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupsViewModel @Inject constructor(@ApplicationContext val context: Context) : ViewModel() {

    private val db = Firebase.database
    private val auth = Firebase.auth

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups = _groups.asStateFlow()

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts = _contacts.asStateFlow()

    private val _currentGroup = MutableStateFlow<Group?>(null)
    val currentGroup = _currentGroup.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadGroups()
        loadContacts()
    }

    private fun loadGroups() {
        val currentUser = auth.currentUser ?: return
        
        db.reference.child("groups")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val groupsList = mutableListOf<Group>()
                    
                    snapshot.children.forEach { groupSnapshot ->
                        val group = groupSnapshot.getValue(Group::class.java)
                        // Mostrar apenas grupos onde o usuário é participante
                        if (group != null && group.participants.containsKey(currentUser.uid)) {
                            groupsList.add(group)
                        }
                    }
                    
                    _groups.value = groupsList.sortedByDescending { it.lastMessageTime }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    fun loadContacts() {
        val currentUser = auth.currentUser ?: return
        
        db.reference.child("user_contacts").child(currentUser.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val contactsList = mutableListOf<Contact>()
                    
                    snapshot.children.forEach { contactSnapshot ->
                        val contact = contactSnapshot.getValue(Contact::class.java)
                        contact?.let { contactsList.add(it) }
                    }
                    
                    _contacts.value = contactsList.sortedBy { it.name }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    fun createGroup(
        name: String,
        description: String,
        selectedContacts: List<Contact>,
        imageUri: Uri? = null,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val currentUser = auth.currentUser ?: return
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val groupId = db.reference.child("groups").push().key ?: return@launch

                // Participants map includes creator + selected contacts
                val participants = mutableMapOf<String, String>()
                participants[currentUser.uid] = currentUser.displayName ?: currentUser.email ?: "Usuário"
                
                selectedContacts.forEach { contact ->
                    participants[contact.uid] = contact.name
                }

                var imageUrl: String? = null
                if (imageUri != null) {
                    imageUrl = SupabaseStorageUtils.uploadImageToSupabase(context, imageUri)
                }

                val group = Group(
                    id = groupId,
                    name = name,
                    description = description,
                    imageUrl = imageUrl,
                    createdBy = currentUser.uid,
                    participants = participants,
                    admins = listOf(currentUser.uid) // Creator is admin by default
                )

                db.reference.child("groups").child(groupId).setValue(group)
                    .addOnCompleteListener { task ->
                        _isLoading.value = false
                        if (task.isSuccessful) {
                            onComplete(true, null)
                        } else {
                            onComplete(false, "Erro ao criar grupo")
                        }
                    }

            } catch (e: Exception) {
                _isLoading.value = false
                onComplete(false, e.message)
            }
        }
    }

    fun updateGroup(
        groupId: String,
        name: String,
        description: String,
        imageUri: Uri? = null,
        removeImage: Boolean = false,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onComplete(false, "Usuário não autenticado")
            return
        }
        
        _isLoading.value = true

        // Primeiro verificar se o usuário é membro do grupo
        db.reference.child("groups").child(groupId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val group = snapshot.getValue(Group::class.java)
                    if (group != null && group.participants.containsKey(currentUser.uid)) {
                        // Qualquer membro pode editar
                        viewModelScope.launch {
                            try {
                                val updates = mutableMapOf<String, Any>()
                                updates["name"] = name
                                updates["description"] = description

                                when {
                                    // Remover imagem se removeImage for true
                                    removeImage -> {
                                        updates["imageUrl"] = ""
                                    }
                                    // Fazer upload de nova imagem se imageUri não for null
                                    imageUri != null -> {
                                        val imageUrl = SupabaseStorageUtils.uploadImageToSupabase(context, imageUri)
                                        if (imageUrl != null) {
                                            updates["imageUrl"] = imageUrl
                                        }
                                    }
                                    // Se imageUri for null e removeImage for false, manter imagem atual (não atualizar)
                                }

                                db.reference.child("groups").child(groupId).updateChildren(updates)
                                    .addOnCompleteListener { task ->
                                        _isLoading.value = false
                                        if (task.isSuccessful) {
                                            // Recarregar a lista de grupos para refletir mudanças
                                            loadGroups()
                                            // Recarregar o grupo atual se estiver carregado
                                            if (_currentGroup.value?.id == groupId) {
                                                loadGroup(groupId)
                                            }
                                            onComplete(true, null)
                                        } else {
                                            onComplete(false, "Erro ao atualizar grupo")
                                        }
                                    }

                                } catch (e: Exception) {
                                    _isLoading.value = false
                                    onComplete(false, e.message)
                                }
                            }
                    } else {
                        _isLoading.value = false
                        onComplete(false, "Apenas membros do grupo podem editá-lo")
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    _isLoading.value = false
                    onComplete(false, "Erro ao verificar permissões")
                }
            })
    }

    fun addParticipants(
        groupId: String,
        selectedContacts: List<Contact>,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val currentUser = auth.currentUser ?: return

        // Get current group data first
        db.reference.child("groups").child(groupId).get()
            .addOnSuccessListener { snapshot ->
                val group = snapshot.getValue(Group::class.java)
                if (group != null) {
                    // Check if current user is admin
                    if (!group.admins.contains(currentUser.uid)) {
                        onComplete(false, "Apenas administradores podem adicionar participantes")
                        return@addOnSuccessListener
                    }

                    // Add new participants
                    val updatedParticipants = group.participants.toMutableMap()
                    selectedContacts.forEach { contact ->
                        updatedParticipants[contact.uid] = contact.name
                    }

                    db.reference.child("groups").child(groupId)
                        .child("participants")
                        .setValue(updatedParticipants)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onComplete(true, null)
                            } else {
                                onComplete(false, "Erro ao adicionar participantes")
                            }
                        }
                } else {
                    onComplete(false, "Grupo não encontrado")
                }
            }
            .addOnFailureListener {
                onComplete(false, "Erro ao carregar dados do grupo")
            }
    }

    fun removeParticipant(
        groupId: String,
        participantId: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val currentUser = auth.currentUser ?: return

        db.reference.child("groups").child(groupId).get()
            .addOnSuccessListener { snapshot ->
                val group = snapshot.getValue(Group::class.java)
                if (group != null) {
                    // Check if current user is admin
                    if (!group.admins.contains(currentUser.uid)) {
                        onComplete(false, "Apenas administradores podem remover participantes")
                        return@addOnSuccessListener
                    }

                    // Cannot remove group creator
                    if (participantId == group.createdBy) {
                        onComplete(false, "Não é possível remover o criador do grupo")
                        return@addOnSuccessListener
                    }

                    // Remove participant
                    db.reference.child("groups").child(groupId)
                        .child("participants")
                        .child(participantId)
                        .removeValue()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onComplete(true, null)
                            } else {
                                onComplete(false, "Erro ao remover participante")
                            }
                        }
                } else {
                    onComplete(false, "Grupo não encontrado")
                }
            }
    }

    fun leaveGroup(groupId: String, onComplete: (Boolean, String?) -> Unit) {
        val currentUser = auth.currentUser ?: return

        db.reference.child("groups").child(groupId).get()
            .addOnSuccessListener { snapshot ->
                val group = snapshot.getValue(Group::class.java)
                if (group != null) {
                    // Group creator cannot leave (must transfer ownership or delete group)
                    if (group.createdBy == currentUser.uid) {
                        onComplete(false, "O criador do grupo não pode sair. Transfira a propriedade ou delete o grupo")
                        return@addOnSuccessListener
                    }

                    // Remove user from participants
                    db.reference.child("groups").child(groupId)
                        .child("participants")
                        .child(currentUser.uid)
                        .removeValue()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onComplete(true, null)
                            } else {
                                onComplete(false, "Erro ao sair do grupo")
                            }
                        }
                } else {
                    onComplete(false, "Grupo não encontrado")
                }
            }
    }

    fun deleteGroup(groupId: String, onComplete: (Boolean, String?) -> Unit) {
        val currentUser = auth.currentUser ?: return

        db.reference.child("groups").child(groupId).get()
            .addOnSuccessListener { snapshot ->
                val group = snapshot.getValue(Group::class.java)
                if (group != null) {
                    // Only group creator can delete
                    if (group.createdBy != currentUser.uid) {
                        onComplete(false, "Apenas o criador do grupo pode deletá-lo")
                        return@addOnSuccessListener
                    }

                    // Delete group and its messages
                    val updates = mapOf<String, Any?>(
                        "/groups/$groupId" to null,
                        "/group_messages/$groupId" to null
                    )

                    db.reference.updateChildren(updates)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onComplete(true, null)
                            } else {
                                onComplete(false, "Erro ao deletar grupo")
                            }
                        }
                } else {
                    onComplete(false, "Grupo não encontrado")
                }
            }
    }
    
    // Função para obter um grupo específico
    fun getGroup(groupId: String): kotlinx.coroutines.flow.StateFlow<Group?> {
        return currentGroup
    }
    
    // Função para carregar um grupo específico
    fun loadGroup(groupId: String) {
        db.reference.child("groups").child(groupId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val group = snapshot.getValue(Group::class.java)?.copy(id = groupId)
                    _currentGroup.value = group
                }
                
                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }
    
    // Função para adicionar membro ao grupo
    fun addMemberToGroup(groupId: String, userId: String, userName: String) {
        val currentUser = auth.currentUser ?: return
        
        // Verificar se o usuário atual é membro do grupo
        db.reference.child("groups").child(groupId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val group = snapshot.getValue(Group::class.java)
                    if (group != null && group.participants.containsKey(currentUser.uid)) {
                        // Adicionar o novo membro
                        val updates = mutableMapOf<String, Any>()
                        updates["participants/$userId"] = userName
                        
                        db.reference.child("groups").child(groupId)
                            .updateChildren(updates)
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }
    
    // Função para remover membro do grupo
    fun removeMemberFromGroup(groupId: String, userId: String) {
        val currentUser = auth.currentUser ?: return
        
        // Verificar se o usuário atual é admin do grupo
        db.reference.child("groups").child(groupId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val group = snapshot.getValue(Group::class.java)
                    if (group != null && group.admins.contains(currentUser.uid)) {
                        // Remover o membro
                        db.reference.child("groups").child(groupId)
                            .child("participants").child(userId)
                            .removeValue()
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }
}
