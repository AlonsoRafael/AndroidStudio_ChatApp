package com.example.chatapp.feature.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.SupabaseStorageUtils
import com.example.chatapp.model.UserProfile
import com.example.chatapp.model.UserStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
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
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _updateStatus = MutableStateFlow<ProfileUpdateState>(ProfileUpdateState.Nothing)
    val updateStatus = _updateStatus.asStateFlow()

    private val db = Firebase.database
    private val auth = Firebase.auth

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser ?: return
        
        db.reference.child("users").child(currentUser.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val profile = snapshot.getValue(UserProfile::class.java)
                    if (profile != null) {
                        _userProfile.value = profile
                    } else {
                        // Criar perfil inicial se não existir
                        createInitialProfile(currentUser.uid)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    _updateStatus.value = ProfileUpdateState.Error("Erro ao carregar perfil")
                }
            })
    }

    private fun createInitialProfile(uid: String) {
        val currentUser = auth.currentUser ?: return
        val initialProfile = UserProfile(
            uid = uid,
            name = currentUser.displayName ?: "",
            email = currentUser.email ?: "",
            status = UserStatus.ONLINE
        )
        
        db.reference.child("users").child(uid).setValue(initialProfile)
    }

    fun updateProfile(name: String, bio: String) {
        val currentUser = auth.currentUser ?: return
        _isLoading.value = true
        _updateStatus.value = ProfileUpdateState.Loading

        val updates = mapOf(
            "name" to name,
            "bio" to bio
        )

        // Atualizar no Firebase Database
        db.reference.child("users").child(currentUser.uid).updateChildren(updates)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    // Atualizar também no Firebase Auth
                    val profileUpdates = userProfileChangeRequest {
                        displayName = name
                    }
                    currentUser.updateProfile(profileUpdates)
                    _updateStatus.value = ProfileUpdateState.Success
                } else {
                    _updateStatus.value = ProfileUpdateState.Error("Erro ao atualizar perfil")
                }
            }
    }

    fun updateProfileImage(imageUri: Uri) {
        val currentUser = auth.currentUser ?: return
        _isLoading.value = true
        _updateStatus.value = ProfileUpdateState.Loading

        viewModelScope.launch {
            try {
                val storageUtils = SupabaseStorageUtils(context)
                val imageUrl = storageUtils.uploadImage(imageUri)
                
                if (imageUrl != null) {
                    // Atualizar URL da imagem no database
                    db.reference.child("users").child(currentUser.uid)
                        .child("profileImageUrl").setValue(imageUrl)
                        .addOnCompleteListener { task ->
                            _isLoading.value = false
                            if (task.isSuccessful) {
                                _updateStatus.value = ProfileUpdateState.Success
                            } else {
                                _updateStatus.value = ProfileUpdateState.Error("Erro ao atualizar foto")
                            }
                        }
                } else {
                    _isLoading.value = false
                    _updateStatus.value = ProfileUpdateState.Error("Erro ao fazer upload da imagem")
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _updateStatus.value = ProfileUpdateState.Error("Erro ao fazer upload: ${e.message}")
            }
        }
    }

    fun updateUserStatus(status: UserStatus) {
        val currentUser = auth.currentUser ?: return
        
        val updates = mapOf(
            "status" to status,
            "lastSeen" to System.currentTimeMillis()
        )

        db.reference.child("users").child(currentUser.uid).updateChildren(updates)
    }

    fun setUserOnline() {
        updateUserStatus(UserStatus.ONLINE)
    }

    fun setUserOffline() {
        updateUserStatus(UserStatus.OFFLINE)
    }

    fun clearUpdateStatus() {
        _updateStatus.value = ProfileUpdateState.Nothing
    }
}

sealed class ProfileUpdateState {
    object Nothing : ProfileUpdateState()
    object Loading : ProfileUpdateState()
    object Success : ProfileUpdateState()
    data class Error(val message: String) : ProfileUpdateState()
}
