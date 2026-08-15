package io.github.nicechester.omok

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import io.github.nicechester.omok.firebase.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OmokApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("OmokApplication", "Initializing Firebase")
        FirebaseApp.initializeApp(this)
        CoroutineScope(Dispatchers.IO).launch {
            FirebaseManager.initialize()
        }
    }
}
