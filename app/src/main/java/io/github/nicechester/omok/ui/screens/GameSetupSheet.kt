package io.github.nicechester.omok.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameSetupSheet(
    onDismiss: () -> Unit,
    onStartGame: (gameId: String, isAI: Boolean, timerSeconds: Int, isCreating: Boolean) -> Unit
) {
    val isAIGame = remember { mutableStateOf(false) }
    val roomCode = remember { mutableStateOf(generateGameId()) }
    val timerSeconds = remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "New Game", fontSize = 24.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp))

        // Game mode
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Game Mode", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { isAIGame.value = false },
                    modifier = Modifier.weight(1f),
                    colors = if (!isAIGame.value)
                        ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF))
                    else ButtonDefaults.outlinedButtonColors()
                ) { Text("vs Player") }
                Button(
                    onClick = { isAIGame.value = true },
                    modifier = Modifier.weight(1f),
                    colors = if (isAIGame.value)
                        ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF))
                    else ButtonDefaults.outlinedButtonColors()
                ) { Text("vs AI") }
            }
        }

        // Room code (vs Player only)
        if (!isAIGame.value) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Room Code", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = roomCode.value,
                    onValueChange = { roomCode.value = it.lowercase().filter { c -> c.isLetterOrDigit() }.take(5) },
                    placeholder = { Text("Room code") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // Timer
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Turn Timer", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0, 10, 20, 30, 60).forEach { seconds ->
                    Button(
                        onClick = { timerSeconds.value = seconds },
                        modifier = Modifier.weight(1f),
                        colors = if (timerSeconds.value == seconds)
                            ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF))
                        else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text(
                            if (seconds == 0) "Off" else "${seconds}s",
                            fontSize = 10.sp,
                            color = if (timerSeconds.value == seconds) Color.White else Color.Black
                        )
                    }
                }
            }
        }

        // Join / Start button
        Button(
            onClick = { onStartGame(roomCode.value, isAIGame.value, timerSeconds.value, false) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF)),
            enabled = isAIGame.value || roomCode.value.isNotEmpty()
        ) {
            Text(if (isAIGame.value) "Start Game" else "Join Game", fontSize = 16.sp)
        }
    }
}

private fun generateGameId(): String {
    val chars = "0123456789abcdefghijklmnopqrstuvwxyz"
    return (0..<5).map { chars.random() }.joinToString("")
}
