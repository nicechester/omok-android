package io.github.nicechester.omok.firebase

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.android.gms.appset.AppSet
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database
import io.github.nicechester.omok.data.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

object FirebaseManager {
    private val tag = "FirebaseManager"
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    suspend fun initialize(context: Context) {
        try {
            Log.d(tag, "Initializing Firebase...")

            val auth = Firebase.auth
            val database = com.google.firebase.Firebase.database("https://omok-5-in-a-row-default-rtdb.firebaseio.com")

            try {
                database.getReference("omok/games").limitToFirst(1).get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _isConnected.value = true
                    } else {
                        _errorMessage.value = task.exception?.message
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Database read error: ${e.message}", e)
            }

            database.getReference(".info/connected").addValueEventListener(
                object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        _isConnected.value = snapshot.getValue(Boolean::class.java) ?: false
                    }
                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                        _errorMessage.value = error.message
                    }
                }
            )

            // Ensure device UID is generated and stored
            ensureDeviceUID(context)

            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
                _isAuthenticated.value = true
            } else {
                _isAuthenticated.value = true
            }

            // Record mapping between device UID and Firebase UID
            auth.currentUser?.uid?.let { firebaseUID ->
                recordUIDMapping(context, firebaseUID)
            }

            Log.d(tag, "Firebase initialization complete")
        } catch (e: Exception) {
            Log.e(tag, "Firebase initialization failed", e)
            _errorMessage.value = e.message
        }
    }

    /** Returns the stable device UID (App Set ID → ANDROID_ID → random UUID fallback). */
    suspend fun getDeviceUID(context: Context): String {
        val stored = PreferencesManager.getDeviceUIDOnce(context)
        if (stored.isNotEmpty()) return stored
        return generateAndStoreDeviceUID(context)
    }

    private suspend fun ensureDeviceUID(context: Context) {
        val stored = PreferencesManager.getDeviceUIDOnce(context)
        if (stored.isEmpty()) generateAndStoreDeviceUID(context)
    }

    private suspend fun generateAndStoreDeviceUID(context: Context): String {
        val uid = resolveDeviceUID(context)
        PreferencesManager.setDeviceUID(context, uid)
        Log.d(tag, "Device UID stored: $uid")
        return uid
    }

    private suspend fun resolveDeviceUID(context: Context): String {
        // Primary: App Set ID
        return try {
            AppSet.getClient(context).appSetIdInfo.await().id
        } catch (e: Exception) {
            Log.w(tag, "App Set ID unavailable, falling back to ANDROID_ID: ${e.message}")
            // Fallback: ANDROID_ID
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?.takeIf { it.isNotEmpty() && it != "9774d56d682e549c" } // filter known bad value
                ?: UUID.randomUUID().toString()
        }
    }

    private suspend fun recordUIDMapping(context: Context, firebaseUID: String) {
        val deviceUID = PreferencesManager.getDeviceUIDOnce(context)
        if (deviceUID.isEmpty() || deviceUID == firebaseUID) return
        try {
            val ref = Firebase.database("https://omok-5-in-a-row-default-rtdb.firebaseio.com")
                .getReference("omok/uidMappings").child(deviceUID).child("firebaseUids")
            val snapshot = ref.get().await()
            @Suppress("UNCHECKED_CAST")
            val existing = (snapshot.value as? List<String>) ?: emptyList()
            if (!existing.contains(firebaseUID)) {
                ref.setValue(existing + firebaseUID).await()
            }
        } catch (e: Exception) {
            Log.w(tag, "recordUIDMapping failed: ${e.message}")
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
