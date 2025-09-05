package com.example.chatapp.manager

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.chatapp.model.UserStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserStatusManager @Inject constructor(
    @ApplicationContext private val context: Context
) : DefaultLifecycleObserver {

    private val db = Firebase.database
    private val auth = Firebase.auth

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        setUserOnline()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        setUserOffline()
    }

    private fun setUserOnline() {
        val currentUser = auth.currentUser ?: return
        updateUserStatus(currentUser.uid, UserStatus.ONLINE)
    }

    private fun setUserOffline() {
        val currentUser = auth.currentUser ?: return
        updateUserStatus(currentUser.uid, UserStatus.OFFLINE)
    }

    private fun updateUserStatus(userId: String, status: UserStatus) {
        val updates = mapOf(
            "status" to status,
            "lastSeen" to System.currentTimeMillis()
        )
        
        db.reference.child("users").child(userId).updateChildren(updates)
    }

    fun setUserStatus(status: UserStatus) {
        val currentUser = auth.currentUser ?: return
        updateUserStatus(currentUser.uid, status)
    }
}
