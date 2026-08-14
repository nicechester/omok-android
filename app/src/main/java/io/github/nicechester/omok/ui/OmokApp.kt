package io.github.nicechester.omok.ui

import androidx.compose.runtime.Composable
import androidx.compose.material3.Scaffold
import androidx.navigation.compose.rememberNavController
import io.github.nicechester.omok.ui.navigation.OmokNavHost
import io.github.nicechester.omok.ui.navigation.BottomNavBar

@Composable
fun OmokApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { paddingValues ->
        OmokNavHost(navController = navController, paddingValues = paddingValues)
    }
}
