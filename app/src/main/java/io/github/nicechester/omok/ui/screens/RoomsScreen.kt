package io.github.nicechester.omok.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nicechester.omok.data.RecentRoomsManager
import io.github.nicechester.omok.data.model.RecentRoom
import java.util.concurrent.TimeUnit
import com.google.firebase.Firebase
import com.google.firebase.database.database
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomsScreen(
    paddingValues: PaddingValues,
    onJoinRoom: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recentRooms = RecentRoomsManager.getRecentRooms(context).collectAsState(initial = emptyList())
    val pendingDelete = remember { mutableStateOf<Pair<RecentRoom, Int>?>(null) }
    val showNewGame = remember { mutableStateOf(false) }
    val undoTask = remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    if (showNewGame.value) {
        ModalBottomSheet(
            onDismissRequest = { showNewGame.value = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            GameSetupSheet(
                onDismiss = { showNewGame.value = false },
                onStartGame = { gameId, _, _, _ ->
                    onJoinRoom(gameId)
                    showNewGame.value = false
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        if (recentRooms.value.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Recent Rooms",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "No recent rooms",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showNewGame.value = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF))
                ) {
                    Text("New Game", fontSize = 16.sp)
                }
            }
        } else {
            // Recent rooms list
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Rooms",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showNewGame.value = true }) {
                        Icon(Icons.Default.Add, contentDescription = "New Game")
                    }
                }

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(recentRooms.value, key = { it.code }) { room ->
                        val dismissState = rememberSwipeToDismissBoxState()
                        val isDismissed = dismissState.currentValue == SwipeToDismissBoxValue.EndToStart
                        LaunchedEffect(isDismissed) {
                            if (isDismissed) {
                                val index = recentRooms.value.indexOf(room)
                                pendingDelete.value = Pair(room, index)
                                RecentRoomsManager.remove(context, room.code)
                                undoTask.value?.cancel()
                                undoTask.value = scope.launch {
                                    delay(3000)
                                    pendingDelete.value = null
                                }
                            }
                        }
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.White)
                                        .padding(end = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDD0000))
                                }
                            }
                        ) {
                            RoomCell(
                                room = room,
                                onTap = { onJoinRoom(room.code) }
                            )
                        }
                        if (room != recentRooms.value.last()) {
                            Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                        }
                    }
                }
            }
        }

        // Undo toast for deleted room
        if (pendingDelete.value != null) {
            val (deletedRoom, index) = pendingDelete.value!!
            LaunchedEffect(deletedRoom.code) {
                // Auto-dismiss after 3 seconds
                delay(3000)
                pendingDelete.value = null
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color(0xFF333333), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Room ${deletedRoom.code.uppercase()} deleted",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                RecentRoomsManager.restore(context, deletedRoom, index)
                                pendingDelete.value = null
                                undoTask.value?.cancel()
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF0066FF))
                    ) {
                        Text("Undo", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomCell(
    room: RecentRoom,
    onTap: () -> Unit = {}
) {
    val context = LocalContext.current
    val gameState = remember { mutableStateOf<Map<String, Any>?>(null) }
    val timeElapsed = formatTimeElapsed(room.lastPlayedAt)

    // Fetch live game state from Firebase
    LaunchedEffect(room.code) {
        fetchGameState(room.code) { state ->
            gameState.value = state
        }
    }

    val playerMatchup = gameState.value?.let { state ->
        val players = (state["players"] as? Map<String, Any>) ?: emptyMap()
        val playerNames = players.values.mapNotNull {
            (it as? Map<String, Any>)?.get("name") as? String
        }
        when {
            playerNames.size == 2 -> "${playerNames[0]} vs ${playerNames[1]}"
            playerNames.size == 1 -> "${playerNames[0]} vs —"
            else -> "Player vs Player"
        }
    } ?: "Loading..."

    val currentPlayerInfo = gameState.value?.let { state ->
        val turn = state["turn"] as? String ?: return@let null
        val players = (state["players"] as? Map<String, Any>) ?: emptyMap()
        val currentPlayer = players.values.find {
            (it as? Map<String, Any>)?.get("color") as? String == turn
        } as? Map<String, Any>
        val playerName = currentPlayer?.get("name") as? String ?: "Player"
        val symbol = if (turn == "black") "●" else "○"
        "$symbol $playerName"
    } ?: ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable { onTap() }
            .padding(16.dp)
    ) {
        Text(
            text = room.code,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0066FF)
        )
        Text(
            text = playerMatchup,
            fontSize = 14.sp,
            color = Color.Gray
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = timeElapsed,
                fontSize = 12.sp,
                color = Color.Gray
            )
            if (currentPlayerInfo.isNotEmpty()) {
                Text(
                    text = currentPlayerInfo,
                    fontSize = 12.sp,
                    color = Color.Blue
                )
            }
        }
    }
}

private suspend fun fetchGameState(
    gameCode: String,
    onStateReceived: (Map<String, Any>?) -> Unit
) {
    try {
        val snapshot = Firebase.database("https://omok-5-in-a-row-default-rtdb.firebaseio.com").getReference("omok/games/$gameCode").get().await()
        onStateReceived(snapshot.value as? Map<String, Any>)
    } catch (e: Exception) {
        onStateReceived(null)
    }
}


private fun formatTimeElapsed(lastPlayedAt: Long): String {
    val now = System.currentTimeMillis()
    val elapsedMs = now - lastPlayedAt
    val hours = TimeUnit.MILLISECONDS.toHours(elapsedMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMs) % 60

    return when {
        hours > 0 -> "$hours hr${if (hours > 1) "s" else ""}, $minutes min"
        minutes > 0 -> "$minutes min"
        else -> "now"
    }
}
