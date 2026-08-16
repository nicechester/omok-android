package io.github.nicechester.omok.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.nicechester.omok.ui.game.GameBoardScreen
import io.github.nicechester.omok.ui.game.GameScreenViewModel
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(paddingValues: PaddingValues, viewModel: GameScreenViewModel? = null) {
    val context = LocalContext.current
    val showGameSetup = remember { mutableStateOf(false) }
    val resolvedViewModel: GameScreenViewModel = viewModel ?: viewModel { GameScreenViewModel(context) }
    val currentRoom = resolvedViewModel.currentRoom.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> resolvedViewModel.onResume()
                Lifecycle.Event.ON_PAUSE -> resolvedViewModel.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (currentRoom.value != null) {
        GameBoardScreen(
            room = currentRoom.value!!,
            remainingSeconds = resolvedViewModel.remainingSeconds.collectAsState().value,
            onMakeMove = { row, col -> resolvedViewModel.makeMove(row, col) },
            onForfeit = { resolvedViewModel.forfeit() },
            onRematch = { resolvedViewModel.voteRematch() },
            onLeave = { resolvedViewModel.leaveGame() },
            onRequestUndo = { resolvedViewModel.requestUndo() },
            onApproveUndo = { resolvedViewModel.approveUndo() },
            onRejectUndo = { resolvedViewModel.rejectUndo() }
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 24.dp)
                ) {
                    repeat(3) {
                        androidx.compose.foundation.layout.Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            repeat(3) {
                                Icon(
                                    Icons.Default.Circle,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = Color(0xFFCCCCCC)
                                )
                            }
                        }
                    }
                }

                Text(text = "No Active Game", fontSize = 28.sp, textAlign = TextAlign.Center)
                Text(
                    text = "Join a recent room or start a new game",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = { showGameSetup.value = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF))
                ) {
                    Text(text = "New Game", fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }

        if (showGameSetup.value) {
            ModalBottomSheet(onDismissRequest = { showGameSetup.value = false }) {
                GameSetupSheet(
                    onDismiss = { showGameSetup.value = false },
                    onStartGame = { gameId, isAI, timerSeconds, _ ->
                        val code = if (isAI) generateGameId() else gameId
                        resolvedViewModel.joinOrCreateGame(code, timerSeconds)
                        showGameSetup.value = false
                    }
                )
            }
        }
    }
}

private fun generateGameId(): String {
    val chars = "0123456789abcdefghijklmnopqrstuvwxyz"
    return (0..<5).map { chars.random() }.joinToString("")
}
