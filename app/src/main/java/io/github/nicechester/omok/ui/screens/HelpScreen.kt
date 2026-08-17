package io.github.nicechester.omok.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HelpScreen(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        HelpSection("How to Play") {
            Text(
                "Omok (오목) is a strategy board game where two players take turns placing stones on a 15×15 board. The first player to get 5 stones in a row (horizontally, vertically, or diagonally) wins the game.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BoardDiagram()
        }

        HelpSection("Game Rules") {
            listOf(
                "Board Size" to "15×15 intersection board",
                "Players" to "Two players (Black and White)",
                "Starting Move" to "Black always goes first",
                "Win Condition" to "Exactly 5 stones in a row (overlines don't count)",
                "Draw" to "Board fills up with no winner",
                "Alternating Turns" to "Players take turns placing one stone per turn"
            ).forEachIndexed { i, (title, desc) ->
                RuleRow(title, desc)
                if (i < 5) HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            WinConditionExample()
        }

        HelpSection("Game Modes") {
            RuleRow("vs Player", "Play against another person online")
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            RuleRow("vs AI", "Play against the computer (Easy, Normal, Hard)")
        }

        HelpSection("Features") {
            listOf(
                "Undo" to "Take back your last move",
                "Timer" to "Optional turn timer for faster games",
                "Rematch" to "Play again with the same opponent",
                "Player Names" to "Set and see player nicknames",
                "Turn Indicator" to "See whose turn it is at a glance"
            ).forEachIndexed { i, (title, desc) ->
                RuleRow(title, desc)
                if (i < 4) HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            }
        }

        HelpSection("Tips") {
            listOf(
                "Control the center of the board for strategic advantage",
                "Block your opponent's winning moves while building your own",
                "An open three (3 in a row with empty space on both ends) is valuable",
                "Play against different AI difficulties to improve your skills"
            ).forEach { tip ->
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text("• ", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(tip, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// 5×3 grid with a black stone at (0,2) and white stone at (0,3)
@Composable
private fun BoardDiagram() {
    val cols = 5
    val rows = 3
    val lineColor = Color.Gray.copy(alpha = 0.4f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(5f / 3f)
            .background(Color.Gray.copy(alpha = 0.05f))
            .padding(8.dp)
    ) {
        val cellW = size.width / cols
        val cellH = size.height / rows

        // grid lines
        for (c in 0..cols) {
            val x = c * cellW
            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
        }
        for (r in 0..rows) {
            val y = r * cellH
            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }

        // black stone at col=2, row=0
        val blackCenter = Offset(2 * cellW + cellW / 2, cellH / 2)
        val radius = (minOf(cellW, cellH) / 2) * 0.8f
        drawCircle(Color.Black, radius, blackCenter)

        // white stone at col=3, row=0
        val whiteCenter = Offset(3 * cellW + cellW / 2, cellH / 2)
        drawCircle(Color.White, radius, whiteCenter)
        drawCircle(Color.Gray.copy(alpha = 0.5f), radius, whiteCenter, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
    }
}

@Composable
private fun WinConditionExample() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("5 in a Row = Win", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(5) {
                Box(modifier = Modifier.size(20.dp).background(Color.Black, CircleShape))
            }
        }
        Text("6 in a Row = No Win (Overline)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(6) {
                Box(modifier = Modifier.size(20.dp).background(Color.Gray, CircleShape))
            }
        }
    }
}

@Composable
private fun HelpSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        content()
    }
}

@Composable
private fun RuleRow(title: String, description: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
