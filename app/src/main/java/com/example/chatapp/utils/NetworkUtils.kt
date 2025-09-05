package com.example.chatapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.database.database
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkUtils @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        private const val TAG = "NetworkUtils"
    }

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return try {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network availability: ${e.message}")
            false
        }
    }

    fun getNetworkType(): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return try {
            val network = connectivityManager.activeNetwork ?: return "None"
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "Unknown"
            
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Other"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting network type: ${e.message}")
            "Error"
        }
    }

    fun testFirebaseConnection(onResult: (Boolean, String) -> Unit) {
        if (!isNetworkAvailable()) {
            onResult(false, "No network connection available")
            return
        }

        try {
            val connectedRef = Firebase.database.reference.child(".info/connected")
            connectedRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    val networkType = getNetworkType()
                    val message = "Firebase connected: $connected, Network: $networkType"
                    
                    Log.d(TAG, message)
                    onResult(connected, message)
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    val message = "Firebase connection test failed: ${error.message}"
                    Log.e(TAG, message)
                    onResult(false, message)
                }
            })
        } catch (e: Exception) {
            val message = "Error testing Firebase connection: ${e.message}"
            Log.e(TAG, message)
            onResult(false, message)
        }
    }
}
