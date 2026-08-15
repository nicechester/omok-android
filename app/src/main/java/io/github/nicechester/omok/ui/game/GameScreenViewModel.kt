package io.github.nicechester.omok.ui.game

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import io.github.nicechester.omok.data.GameRepository
import io.github.nicechester.omok.data.PreferencesManager
import io.github.nicechester.omok.data.RecentRoomsManager
import io.github.nicechester.omok.data.model.GameRoom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GameScreenViewModel(private val context: Context? = null) : ViewModel() {
    val currentRoom: StateFlow<GameRoom?> = GameRepository.currentRoom

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun joinOrCreateGame(gameId: String, timerSeconds: Int = 0) {
        viewModelScope.launch {
            try {
                // Wait for auth if not ready yet
                if (com.google.firebase.Firebase.auth.currentUser == null) {
                    android.util.Log.d("GameScreenViewModel", "Waiting for auth...")
                    kotlinx.coroutines.delay(2000)
                }
                val playerName = context?.let { PreferencesManager.getPlayerNameOnce(it) } ?: "Player"
                android.util.Log.d("GameScreenViewModel", "joinOrCreateGame: gameId=$gameId, player=$playerName")
                val success = GameRepository.joinOrCreateGame(gameId, playerName)
                if (success) {
                    context?.let { RecentRoomsManager.recordPlay(it, gameId) }
                } else {
                    _errorMessage.value = "Failed to join or create game"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun makeMove(row: Int, col: Int) {
        viewModelScope.launch {
            try {
                GameRepository.makeMove(row, col)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to make move: ${e.message}"
            }
        }
    }

    fun forfeit() {
        viewModelScope.launch {
            try {
                GameRepository.forfeit()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to forfeit: ${e.message}"
            }
        }
    }

    fun voteRematch() {
        viewModelScope.launch {
            try {
                GameRepository.voteRematch()
                // Only creator drives reset to avoid race — mirrors iOS
                val room = currentRoom.value ?: return@launch
                val uid = Firebase.auth.currentUser?.uid ?: return@launch
                if (room.createdBy == uid) {
                    GameRepository.resetForRematch()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to rematch: ${e.message}"
            }
        }
    }

    fun leaveGame() {
        GameRepository.stopListening()
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
