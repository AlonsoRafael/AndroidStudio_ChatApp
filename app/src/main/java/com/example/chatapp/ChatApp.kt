package com.example.chatapp

import android.app.Application
import android.util.Log
import com.example.chatapp.config.FirebaseConfig
import com.google.firebase.Firebase
import com.google.firebase.initialize
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ChatApp: Application() {
    
    @Inject
    lateinit var firebaseConfig: FirebaseConfig
    
    companion object {
        private const val TAG = "ChatApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            // Inicializar Firebase
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                Firebase.initialize(this)
                Log.d(TAG, "Firebase inicializado na aplicação")
            }
            
            Log.d(TAG, "ChatApp inicializado com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar ChatApp: ${e.message}", e)
        }
    }
}