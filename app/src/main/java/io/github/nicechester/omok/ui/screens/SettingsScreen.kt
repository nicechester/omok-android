package io.github.nicechester.omok.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nicechester.omok.data.PreferencesManager
import io.github.nicechester.omok.firebase.FirebaseManager
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(paddingValues: PaddingValues) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isConnected = FirebaseManager.isConnected.collectAsState()
    val isAuthenticated = FirebaseManager.isAuthenticated.collectAsState()
    val notificationsEnabled = PreferencesManager.getNotificationsEnabled(context).collectAsState(initial = true)
    val playerName = PreferencesManager.getPlayerName(context).collectAsState(initial = "")

    val nameInput = remember(playerName.value) { mutableStateOf(playerName.value) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // NOTIFICATIONS
        SectionLabel("Notifications")
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Notifications")
                Switch(
                    checked = notificationsEnabled.value,
                    onCheckedChange = { scope.launch { PreferencesManager.setNotificationsEnabled(context, it) } }
                )
            }
        }

        // CONNECTION
        SectionLabel("Connection")
        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    !isAuthenticated.value -> CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    isConnected.value -> Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF34C759),
                        modifier = Modifier.size(16.dp)
                    )
                    else -> Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = when {
                        !isAuthenticated.value -> "Connecting…"
                        isConnected.value -> "Connected"
                        else -> "Connection Error"
                    },
                    modifier = Modifier.padding(start = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }

        // CHANGE NAME
        SettingsCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Change name", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = nameInput.value,
                    onValueChange = { if (it.length <= 20) nameInput.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(
                    "${nameInput.value.length}/20",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Start)
                )
                Button(
                    onClick = {
                        val trimmed = nameInput.value.trim()
                        if (trimmed.isNotBlank()) scope.launch { PreferencesManager.setPlayerName(context, trimmed) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF)),
                    enabled = nameInput.value.isNotBlank()
                ) {
                    Text("Save", modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }

        // APP INFO
        SettingsCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Omok", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                val pm = context.packageManager
                val info = pm.getPackageInfo(context.packageName, 0)
                @Suppress("DEPRECATION")
                Text(
                    "Version ${info.versionName} (${info.versionCode})",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text("© 2026 Chester Kim", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 12.sp,
        color = Color.Gray,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
