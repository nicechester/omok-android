package io.github.nicechester.omok

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import io.github.nicechester.omok.firebase.FirebaseManager
import io.github.nicechester.omok.ui.theme.OmokTheme
import io.github.nicechester.omok.ui.OmokApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        lifecycleScope.launch {
            FirebaseManager.initialize()
        }

        setContent {
            OmokTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OmokApp()
                }
            }
        }
    }
}
