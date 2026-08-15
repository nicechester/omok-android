package io.github.nicechester.omok.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import io.github.nicechester.omok.data.PreferencesManager
import io.github.nicechester.omok.firebase.FirebaseManager
import io.github.nicechester.omok.ui.navigation.BottomNavBar
import io.github.nicechester.omok.ui.navigation.OmokNavHost
import io.github.nicechester.omok.ui.screens.OnboardingScreen
import kotlinx.coroutines.launch

@Composable
fun OmokApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val isConnected = FirebaseManager.isConnected.collectAsState()
    val isAuthenticated = FirebaseManager.isAuthenticated.collectAsState()
    val playerName = PreferencesManager.getPlayerName(context).collectAsState(initial = "")

    if (!isAuthenticated.value) {
        // Show loading screen while Firebase authenticates (like iOS)
        LaunchStatusView()
    } else if (playerName.value.isEmpty()) {
        OnboardingScreen(
            onPlayerNameSet = { name ->
                scope.launch {
                    PreferencesManager.setPlayerName(context, name)
                }
            }
        )
    } else {
        Scaffold(
            topBar = {
                if (!isConnected.value) {
                    ConnectionStatusBar()
                }
            },
            bottomBar = { BottomNavBar(navController) }
        ) { paddingValues ->
            OmokNavHost(navController = navController, paddingValues = paddingValues)
        }
    }
}

@Composable
fun LaunchStatusView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Connecting to server...",
                fontSize = 18.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ConnectionStatusBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFEEEE))
            .padding(8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(Color.Red, CircleShape)
        )
        Text(
            text = "Disconnected",
            modifier = Modifier.padding(start = 8.dp),
            fontSize = 12.sp,
            color = Color.Red
        )
    }
}
