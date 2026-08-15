package io.github.nicechester.omok.firebase

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private val tag = "FirebaseManager"
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    suspend fun initialize() {
        try {
            Log.d(tag, "Initializing Firebase...")

            val auth = Firebase.auth
            // Explicitly configure database with URL from google-services.json
            val database = com.google.firebase.Firebase.database("https://omok-5-in-a-row-default-rtdb.firebaseio.com")

            // Trigger database connection by attempting to read
            Log.d(tag, "Attempting to read from database...")
            try {
                database.getReference("omok/games").limitToFirst(1).get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(tag, "Database read successful, connection established")
                        _isConnected.value = true
                    } else {
                        Log.e(tag, "Database read failed: ${task.exception?.message}")
                        _errorMessage.value = task.exception?.message
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Database read error: ${e.message}", e)
            }

            // Also set up connection state monitoring
            database.getReference(".info/connected").addValueEventListener(
                object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        val connected = snapshot.getValue(Boolean::class.java) ?: false
                        Log.d(tag, "Firebase connection state: $connected")
                        _isConnected.value = connected
                    }

                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                        Log.e(tag, "Connection monitoring error: ${error.message}")
                        _errorMessage.value = error.message
                    }
                }
            )

            // Sign-in (blocking, like iOS) - only wait for auth, not database connection
            if (auth.currentUser == null) {
                Log.d(tag, "Signing in anonymously...")
                auth.signInAnonymously().await()
                Log.d(tag, "Signed in as: ${auth.currentUser?.uid}")
                _isAuthenticated.value = true
            } else {
                Log.d(tag, "Already authenticated as: ${auth.currentUser?.uid}")
                _isAuthenticated.value = true
            }

            Log.d(tag, "Firebase initialization complete (auth only)")
        } catch (e: Exception) {
            Log.e(tag, "Firebase initialization failed", e)
            _errorMessage.value = e.message
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
