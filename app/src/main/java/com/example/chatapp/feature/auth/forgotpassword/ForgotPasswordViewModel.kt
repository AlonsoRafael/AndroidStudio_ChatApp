package com.example.chatapp.feature.auth.forgotpassword

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Nothing)
    val state = _state.asStateFlow()

    fun resetPassword(email: String) {
        if (email.isEmpty()) {
            _state.value = ForgotPasswordState.Error("Por favor, digite um email válido")
            return
        }

        _state.value = ForgotPasswordState.Loading
        
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _state.value = ForgotPasswordState.Success
                } else {
                    val errorMessage = when {
                        task.exception?.message?.contains("There is no user record") == true -> 
                            "Não existe conta com este email"
                        task.exception?.message?.contains("badly formatted") == true -> 
                            "Email inválido"
                        task.exception?.message?.contains("network") == true -> 
                            "Erro de conexão. Verifique sua internet"
                        else -> "Erro ao enviar email de recuperação"
                    }
                    _state.value = ForgotPasswordState.Error(errorMessage)
                }
            }
    }

    fun clearState() {
        _state.value = ForgotPasswordState.Nothing
    }
}

sealed class ForgotPasswordState {
    object Nothing : ForgotPasswordState()
    object Loading : ForgotPasswordState()
    object Success : ForgotPasswordState()
    data class Error(val message: String) : ForgotPasswordState()
}
