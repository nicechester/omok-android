package io.github.nicechester.omok.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import io.github.nicechester.omok.data.model.GameRoom
import kotlin.math.roundToInt

private val BoardColor = Color(0xFFDCB468)
private val LineColor = Color(0xFF3A2A00)
private const val BOARD_SIZE = 15

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameBoardScreen(
    room: GameRoom,
    onMakeMove: (row: Int, col: Int) -> Unit,
    onForfeit: () -> Unit,
    onRematch: () -> Unit,
    onLeave: () -> Unit,
    onRequestUndo: () -> Unit = {},
    onApproveUndo: () -> Unit = {},
    onRejectUndo: () -> Unit = {}
) {
    val uid = Firebase.auth.currentUser?.uid
    val mySeat = uid?.let { room.seatOf(it) }
    val canPlay = room.isPlaying() && mySeat != null && room.turn == mySeat && room.undoRequest == null

    val undoRequest = room.undoRequest
    val iAmRequester = undoRequest != null && undoRequest.requestedBy == uid
    val iAmOpponent = undoRequest != null && undoRequest.requestedBy != uid && mySeat != null
    val canRequestUndo = room.isPlaying() && mySeat != null && room.moveCount >= 2 && undoRequest == null && room.turn != mySeat


    val blackSeat = room.blackSeat
    val whiteSeat = room.whiteSeat
    val blackScore = room.scores[blackSeat?.uid ?: ""] ?: 0
    val whiteScore = room.scores[whiteSeat?.uid ?: ""] ?: 0

    val statusText = when {
        room.isFinished() -> when {
            room.result == "draw" -> "Draw"
            mySeat != null && room.result == mySeat -> "You win"
            mySeat != null -> "You lose"
            room.result == "black" -> "● wins"
            else -> "○ wins"
        }
        room.isWaiting() -> "Waiting..."
        room.turn == "black" -> "${blackSeat?.name ?: "Black"}'s turn"
        else -> "${whiteSeat?.name ?: "White"}'s turn"
    }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Undo request dialog — shown to opponent
    if (iAmOpponent) {
        val requesterName = room.players[undoRequest!!.requestedBy]?.name
            ?: room.players[undoRequest.requestedBy]?.uid?.take(6)
            ?: "Opponent"
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Undo Request") },
            text = { Text("$requesterName wants to take back their last move.") },
            confirmButton = {
                TextButton(onClick = onApproveUndo) { Text("Approve") }
            },
            dismissButton = {
                TextButton(onClick = onRejectUndo) { Text("Deny") }
            }
        )
    }

    if (room.isFinished()) {
        val resultColor = when {
            room.result == "draw" -> Color(0xFF666666)
            mySeat != null && room.result == mySeat -> Color(0xFF00AA00)
            mySeat != null -> Color(0xFFDD0000)
            else -> Color(0xFF333333)
        }
        val resultLabel = when {
            room.result == "draw" -> "Draw"
            mySeat != null && room.result == mySeat -> "You win"
            mySeat != null -> "You lose"
            room.result == "black" -> "Black wins"
            else -> "White wins"
        }
        ModalBottomSheet(
            onDismissRequest = {},
            sheetState = sheetState,
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = resultLabel, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = resultColor)
                Button(
                    onClick = onRematch,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("↺  Rematch", fontSize = 17.sp, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header row 1: room code + icon buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = room.id,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            // Copy code
            IconButton(onClick = {
                clipboardManager.setText(AnnotatedString(room.id))
            }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy code", modifier = Modifier.size(22.dp))
            }
            // Share
            IconButton(onClick = {
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, "Join my Omok game: ${room.id}")
                }
                context.startActivity(android.content.Intent.createChooser(intent, "Share room code"))
            }) {
                Icon(Icons.Default.IosShare, contentDescription = "Share", modifier = Modifier.size(22.dp))
            }
            // Leave
            IconButton(onClick = onLeave) {
                Icon(Icons.Default.Logout, contentDescription = "Leave", modifier = Modifier.size(22.dp))
            }
        }

        // Header row 2: players + status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val blackDisconnected = blackSeat?.active == false
            val whiteDisconnected = whiteSeat?.active == false

            Text(
                text = "● ${blackSeat?.name ?: blackSeat?.uid?.take(6) ?: "—"}${if (blackDisconnected) " (disconnected)" else ""}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (blackDisconnected) Color.Gray else Color.Black
            )
            if (blackScore > 0) Text(" $blackScore", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6600))

            Text("  ", fontSize = 14.sp)

            Text(
                text = "○ ${whiteSeat?.name ?: whiteSeat?.uid?.take(6) ?: "—"}${if (whiteDisconnected) " (disconnected)" else ""}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (whiteDisconnected) Color.Gray else Color.Black
            )
            if (whiteScore > 0) Text(" $whiteScore", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6600))

            Spacer(modifier = Modifier.weight(1f))
            Text(text = statusText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black, textAlign = TextAlign.End)
        }

        // Board — full width, square, with inner padding so lines don't touch edges
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(BoardColor)
        ) {
            val winCells = room.winningLine.map { it.r to it.c }.toSet()
            val boardPadding = 20.dp

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(canPlay) {
                        detectTapGestures { offset ->
                            if (!canPlay) return@detectTapGestures
                            val pad = boardPadding.toPx()
                            val cellSize = (size.width - pad * 2) / (BOARD_SIZE - 1)
                            val col = ((offset.x - pad) / cellSize).roundToInt().coerceIn(0, BOARD_SIZE - 1)
                            val row = ((offset.y - pad) / cellSize).roundToInt().coerceIn(0, BOARD_SIZE - 1)
                            onMakeMove(row, col)
                        }
                    }
            ) {
                val pad = boardPadding.toPx()
                val cellSize = (size.width - pad * 2) / (BOARD_SIZE - 1)
                val stoneRadius = cellSize * 0.46f

                // Grid lines
                for (i in 0 until BOARD_SIZE) {
                    val pos = pad + i * cellSize
                    drawLine(LineColor, Offset(pos, pad), Offset(pos, size.height - pad), strokeWidth = 1.5f)
                    drawLine(LineColor, Offset(pad, pos), Offset(size.width - pad, pos), strokeWidth = 1.5f)
                }

                // Star points
                for (r in listOf(3, 7, 11)) for (c in listOf(3, 7, 11)) {
                    drawCircle(LineColor, radius = 4f, center = Offset(pad + c * cellSize, pad + r * cellSize))
                }

                // Stones
                for ((key, color) in room.board) {
                    val parts = key.split("_")
                    if (parts.size != 2) continue
                    val r = parts[0].toIntOrNull() ?: continue
                    val c = parts[1].toIntOrNull() ?: continue
                    val center = Offset(pad + c * cellSize, pad + r * cellSize)
                    val isWin = (r to c) in winCells
                    val isLast = room.lastMove?.r == r && room.lastMove?.c == c

                    if (color == "black") {
                        drawCircle(Color.Black, stoneRadius, center)
                    } else {
                        drawCircle(Color.White, stoneRadius, center)
                        drawCircle(Color.Black, stoneRadius, center, style = Stroke(width = 1.5f))
                    }
                    if (isLast) {
                        drawCircle(Color.Red, stoneRadius * 0.28f, center, style = Stroke(width = 1.5f))
                    }
                    if (isWin) {
                        drawCircle(Color(0xFFFFD700), stoneRadius, center, style = Stroke(width = 3f))
                    }
                }
            }
        }

        // Bottom action bar — Undo center, Resign right (matches iOS)
        if (!room.isFinished()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Placeholder mic button (left) — matches iOS layout
                Box(modifier = Modifier.size(48.dp))

                // Undo
                Button(
                    onClick = onRequestUndo,
                    modifier = Modifier.weight(1f),
                    enabled = canRequestUndo,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEEEEEE),
                        contentColor = Color(0xFF0066FF),
                        disabledContainerColor = Color(0xFFEEEEEE),
                        disabledContentColor = Color(0xFFAAAAAA)
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(if (iAmRequester) "↩ Pending…" else "↩ Undo")
                }

                // Resign — right
                Button(
                    onClick = onForfeit,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEEEEEE),
                        contentColor = Color(0xFFDD0000)
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("🚩 Resign")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
