package io.github.nicechester.omok.ui.game

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private data class EmojiEntry(val hexcode: String, val tags: String)

private fun loadEmojiData(context: Context): List<EmojiEntry> {
    val csv = context.assets.open("openmoji.csv").bufferedReader().readText()
    val result = mutableListOf<EmojiEntry>()
    for (line in csv.lines().drop(1)) {
        val cols = parseCSVLine(line)
        if (cols.size < 7) continue
        val hexcode = cols[1]
        if (hexcode.contains("-") && hexcode.length > 10) continue
        result += EmojiEntry(hexcode, "${cols[4]} ${cols[5]} ${cols[6]}".lowercase())
    }
    return result
}

private fun parseCSVLine(line: String): List<String> {
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    for (ch in line) {
        when {
            ch == '"' -> inQuotes = !inQuotes
            ch == ',' && !inQuotes -> { result += current.toString(); current.clear() }
            else -> current.append(ch)
        }
    }
    result += current.toString()
    return result
}

@Composable
private fun OpenMojiImage(hexcode: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(hexcode) {
        runCatching {
            context.assets.open("openmoji/$hexcode.png").use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = modifier)
    } else {
        Box(modifier = modifier)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiPickerSheet(onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val allEmoji = remember { loadEmojiData(context) }
    var search by remember { mutableStateOf("") }
    val filtered = remember(search) {
        if (search.isBlank()) allEmoji
        else allEmoji.filter { it.tags.contains(search.lowercase()) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Search emoji") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(filtered, key = { it.hexcode }) { emoji ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clickable { onSelect(emoji.hexcode); onDismiss() }
                ) {
                    OpenMojiImage(hexcode = emoji.hexcode, modifier = Modifier.size(40.dp))
                }
            }
        }
    }
}
