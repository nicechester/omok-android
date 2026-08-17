package io.github.nicechester.omok.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.nicechester.omok.firebase.PendingGameNavigation
import io.github.nicechester.omok.ui.game.GameScreenViewModel
import io.github.nicechester.omok.ui.screens.GameScreen
import io.github.nicechester.omok.ui.screens.RoomsScreen
import io.github.nicechester.omok.ui.screens.SettingsScreen

@Composable
fun OmokNavHost(
    navController: NavHostController,
    paddingValues: PaddingValues
) {
    val context = LocalContext.current
    val gameViewModel: GameScreenViewModel = viewModel { GameScreenViewModel(context) }
    val pendingGameId by PendingGameNavigation.pendingGameId.collectAsState()

    LaunchedEffect(pendingGameId) {
        pendingGameId?.let { gameId ->
            gameViewModel.joinOrCreateGame(gameId)
            navController.navigate("play") { launchSingleTop = true }
            PendingGameNavigation.consume()
        }
    }

    NavHost(
        navController = navController,
        startDestination = "play"
    ) {
        composable("rooms") {
            RoomsScreen(
                paddingValues = paddingValues,
                onJoinRoom = { gameId ->
                    gameViewModel.joinOrCreateGame(gameId)
                    navController.navigate("play")
                }
            )
        }
        composable("play") {
            GameScreen(paddingValues = paddingValues, viewModel = gameViewModel)
        }
        composable("settings") {
            SettingsScreen(paddingValues = paddingValues)
        }
    }
}
