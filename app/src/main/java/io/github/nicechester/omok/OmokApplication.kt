package io.github.nicechester.omok

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import io.github.nicechester.omok.firebase.FirebaseManager
import io.github.nicechester.omok.firebase.OmokFirebaseMessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class OmokApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("OmokApplication", "Initializing Firebase")
        FirebaseApp.initializeApp(this)
        OmokFirebaseMessagingService.createNotificationChannel(this)
        CoroutineScope(Dispatchers.IO).launch {
            FirebaseManager.initialize()
            FirebaseManager.isAuthenticated.first { it }
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                Log.d("OmokApplication", "FCM token: $token")
                OmokFirebaseMessagingService.saveToken(token)
            } catch (e: Exception) {
                Log.e("OmokApplication", "FCM token fetch failed: ${e.message}")
            }
        }
    }
}
