package com.example.chatapp.feature.contacts

import android.content.Context
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.model.Contact
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
class ContactsViewModel @Inject constructor(@ApplicationContext val context: Context) : ViewModel() {

    private val db = Firebase.database
    private val auth = Firebase.auth

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts = _contacts.asStateFlow()

    private val _deviceContacts = MutableStateFlow<List<Contact>>(emptyList())
    val deviceContacts = _deviceContacts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadContacts()
    }

    private fun loadContacts() {
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

    fun addContactByEmail(email: String, onComplete: (Boolean, String?) -> Unit) {
        val currentUser = auth.currentUser ?: return
        _isLoading.value = true

        // Verificar se o email existe na base de usuários
        addContactWithRetry(email, currentUser, onComplete, 0)
    }

    private fun addContactWithRetry(
        email: String, 
        currentUser: com.google.firebase.auth.FirebaseUser, 
        onComplete: (Boolean, String?) -> Unit, 
        retryCount: Int
    ) {
        val maxRetries = 3
        val retryDelay = 2000L // 2 seconds
        
        db.reference.child("users")
            .orderByChild("email")
            .equalTo(email)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val userSnapshot = snapshot.children.first()
                    val userProfile = userSnapshot.getValue(UserProfile::class.java)
                    
                    if (userProfile != null && userProfile.uid != currentUser.uid) {
                        val contact = Contact(
                            uid = userProfile.uid,
                            name = userProfile.name,
                            email = userProfile.email,
                            profileImageUrl = userProfile.profileImageUrl,
                            isFromDevice = false
                        )

                        // Adicionar contato na lista do usuário
                        db.reference.child("user_contacts")
                            .child(currentUser.uid)
                            .child(userProfile.uid)
                            .setValue(contact)
                            .addOnCompleteListener { task ->
                                _isLoading.value = false
                                if (task.isSuccessful) {
                                    onComplete(true, null)
                                } else {
                                    if (retryCount < maxRetries) {
                                        // Tentar novamente após delay
                                        viewModelScope.launch {
                                            kotlinx.coroutines.delay(retryDelay)
                                            addContactWithRetry(email, currentUser, onComplete, retryCount + 1)
                                        }
                                    } else {
                                        onComplete(false, "Erro ao adicionar contato após $maxRetries tentativas")
                                    }
                                }
                            }
                    } else {
                        _isLoading.value = false
                        if (userProfile?.uid == currentUser.uid) {
                            onComplete(false, "Você não pode adicionar a si mesmo")
                        } else {
                            onComplete(false, "Usuário não encontrado")
                        }
                    }
                } else {
                    _isLoading.value = false
                    onComplete(false, "Usuário com este email não existe no ChatApp")
                }
            }
            .addOnFailureListener { exception ->
                _isLoading.value = false
                
                if (retryCount < maxRetries && isNetworkRelatedError(exception)) {
                    // Tentar novamente para erros de rede
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(retryDelay)
                        addContactWithRetry(email, currentUser, onComplete, retryCount + 1)
                    }
                } else {
                    val errorMessage = when {
                        exception.message?.contains("network", true) == true -> 
                            "Erro de conexão. Verifique sua internet e tente novamente."
                        exception.message?.contains("timeout", true) == true -> 
                            "Timeout de conexão. Tente novamente."
                        exception.message?.contains("dns", true) == true -> 
                            "Erro de DNS. Verifique a conexão."
                        retryCount >= maxRetries -> 
                            "Erro de conexão persistente após $maxRetries tentativas"
                        else -> 
                            "Erro de conexão: ${exception.message}"
                    }
                    onComplete(false, errorMessage)
                }
            }
    }

    private fun isNetworkRelatedError(exception: Exception): Boolean {
        val message = exception.message?.lowercase() ?: ""
        return message.contains("network") || 
               message.contains("timeout") || 
               message.contains("connection") || 
               message.contains("dns") ||
               message.contains("unreachable")
    }

    fun removeContact(contactUid: String, onComplete: (Boolean) -> Unit) {
        val currentUser = auth.currentUser ?: return

        db.reference.child("user_contacts")
            .child(currentUser.uid)
            .child(contactUid)
            .removeValue()
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
    }

    fun importContactsFromDevice() {
        viewModelScope.launch {
            _isLoading.value = true
            val deviceContactsList = getDeviceContacts()
            _deviceContacts.value = deviceContactsList
            _isLoading.value = false
        }
    }

    private fun getDeviceContacts(): List<Contact> {
        val contactsList = mutableListOf<Contact>()
        
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Email.DATA,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Email.DISPLAY_NAME + " ASC"
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val name = it.getString(0) ?: "Nome desconhecido"
                    val email = it.getString(1) ?: ""
                    val phone = it.getString(2) ?: ""

                    if (email.isNotEmpty()) {
                        val contact = Contact(
                            name = name,
                            email = email,
                            phoneNumber = phone,
                            isFromDevice = true
                        )
                        contactsList.add(contact)
                    }
                }
            }
        } catch (e: SecurityException) {
            // Permissão negada
        } catch (e: Exception) {
            // Outro erro
        }

        return contactsList.distinctBy { it.email }
    }

    fun addDeviceContactToApp(deviceContact: Contact, onComplete: (Boolean, String?) -> Unit) {
        // Verificar se o contato existe no ChatApp e adicionar
        addContactByEmail(deviceContact.email) { success, message ->
            onComplete(success, message)
        }
    }
}
