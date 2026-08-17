package io.github.nicechester.omok.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.github.nicechester.omok.MainActivity

class OmokFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d("OmokFCM", "onNewToken: $token")
        saveToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("OmokFCM", "onMessageReceived: data=${message.data}, notification=${message.notification?.title}")
        val gameId = message.data["gameId"] ?: return
        if (ActiveGameTracker.activeGameId == gameId) {
            Log.d("OmokFCM", "Game already active, suppressing notification")
            return
        }
        val title = message.notification?.title ?: "Your Turn!"
        val body = message.notification?.body ?: "Opponent made a move"
        showNotification(gameId, title, body)
    }

    private fun showNotification(gameId: String, title: String, body: String) {
        val channelId = "omok_turns"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(nm, channelId)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("gameId", gameId)
        }
        val pi = PendingIntent.getActivity(this, gameId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        nm.notify(gameId.hashCode(), notification)
    }

    companion object {
        private const val CHANNEL_ID = "omok_turns"
        private const val CHANNEL_NAME = "Your Turn"

        fun createNotificationChannel(nm: NotificationManager, channelId: String = CHANNEL_ID) {
            if (nm.getNotificationChannel(channelId) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(channelId, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
                )
            }
        }

        fun createNotificationChannel(context: Context) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            createNotificationChannel(nm, CHANNEL_ID)
        }

        fun saveToken(token: String) {
            val uid = Firebase.auth.currentUser?.uid ?: run {
                Log.w("OmokFCM", "saveToken: no authenticated user")
                return
            }
            Log.d("OmokFCM", "saveToken: uid=$uid")
            Firebase.database("https://omok-5-in-a-row-default-rtdb.firebaseio.com")
                .getReference("omok/users/$uid/fcmToken")
                .setValue(token)
        }
    }
}
