package io.github.nicechester.omok.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.nicechester.omok.ui.screens.GameScreen
import io.github.nicechester.omok.ui.screens.RoomsScreen
import io.github.nicechester.omok.ui.screens.SettingsScreen

@Composable
fun OmokNavHost(
    navController: NavHostController,
    paddingValues: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = "play"
    ) {
        composable("rooms") {
            RoomsScreen(paddingValues = paddingValues)
        }
        composable("play") {
            GameScreen(paddingValues = paddingValues)
        }
        composable("settings") {
            SettingsScreen(paddingValues = paddingValues)
        }
    }
}
