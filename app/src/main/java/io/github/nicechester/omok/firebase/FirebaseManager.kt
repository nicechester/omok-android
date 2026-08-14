package io.github.nicechester.omok.firebase

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    suspend fun initialize() {
        if (_isInitialized.value) return

        try {
            val auth = Firebase.auth
            val database = Firebase.database

            // Set up connection state monitoring
            database.getReference(".info/connected").addValueEventListener(
                object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        val connected = snapshot.getValue(Boolean::class.java) ?: false
                        _isConnected.value = connected
                    }

                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                        _errorMessage.value = error.message
                    }
                }
            )

            // Attempt anonymous sign-in if not already authenticated
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
            }

            _isInitialized.value = true
        } catch (e: Exception) {
            _errorMessage.value = "Firebase initialization failed: ${e.message}"
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
