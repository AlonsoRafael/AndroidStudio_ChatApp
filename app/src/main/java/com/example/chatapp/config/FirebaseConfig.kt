package com.example.chatapp.config

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.database.database
import com.google.firebase.FirebaseOptions
import com.google.firebase.initialize
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseConfig @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        private const val TAG = "FirebaseConfig"
        private const val DATABASE_URL = "https://chatapp-b2036-default-rtdb.firebaseio.com"
    }

    init {
        configureFirebase()
    }

    private fun configureFirebase() {
        try {
            // Verificar se Firebase já está inicializado
            if (FirebaseApp.getApps(context).isEmpty()) {
                Log.d(TAG, "Inicializando Firebase...")
                Firebase.initialize(context)
            }

            // Configurar Firebase Realtime Database
            val database = Firebase.database
            
            // Configurações para melhor performance em emuladores
            database.setPersistenceEnabled(true)
            database.reference.keepSynced(true)
            
            // Configurar timeout de conexão
            database.reference.database.goOnline()
            
            Log.d(TAG, "Firebase configurado com sucesso")
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao configurar Firebase: ${e.message}", e)
        }
    }

    fun testConnection(onResult: (Boolean) -> Unit) {
        try {
            val testRef = Firebase.database.reference.child(".info/connected")
            testRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    Log.d(TAG, "Firebase connection status: $connected")
                    onResult(connected)
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    Log.e(TAG, "Connection test failed: ${error.message}")
                    onResult(false)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error testing connection: ${e.message}")
            onResult(false)
        }
    }

    fun retryConnection() {
        try {
            Firebase.database.goOffline()
            Thread.sleep(1000) // Wait 1 second
            Firebase.database.goOnline()
            Log.d(TAG, "Firebase reconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Error retrying connection: ${e.message}")
        }
    }
}
