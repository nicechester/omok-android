package io.github.nicechester.omok.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import io.github.nicechester.omok.firebase.FirebaseManager
import io.github.nicechester.omok.ui.navigation.OmokNavHost
import io.github.nicechester.omok.ui.navigation.BottomNavBar

@Composable
fun OmokApp() {
    val navController = rememberNavController()
    val isConnected = FirebaseManager.isConnected.collectAsState()

    Scaffold(
        topBar = {
            ConnectionStatusBar(isConnected = isConnected.value)
        },
        bottomBar = { BottomNavBar(navController) }
    ) { paddingValues ->
        OmokNavHost(navController = navController, paddingValues = paddingValues)
    }
}

@Composable
fun ConnectionStatusBar(isConnected: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (isConnected) Color.Green else Color.Red,
                    shape = CircleShape
                )
        )
        Text(
            text = if (isConnected) "Connected" else "Disconnected",
            modifier = Modifier.padding(start = 8.dp),
            fontSize = 12.sp,
            color = if (isConnected) Color.Green else Color.Red
        )
    }
}
