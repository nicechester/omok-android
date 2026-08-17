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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GameScreenViewModel(private val context: Context? = null) : ViewModel() {
    val currentRoom: StateFlow<GameRoom?> = GameRepository.currentRoom

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _pendingReaction = MutableStateFlow<String?>(null)
    val pendingReaction: StateFlow<String?> = _pendingReaction

    private var reactionClearJob: Job? = null
    private var lastReactionTimestamp: Long = -1

    private val _remainingSeconds = MutableStateFlow<Int?>(null)
    val remainingSeconds: StateFlow<Int?> = _remainingSeconds

    private var timerJob: Job? = null
    private var timerAnchor: Pair<String, Long>? = null // turn to turnStartedAt
    private var isPaused = false

    init {
        viewModelScope.launch {
            GameRepository.currentRoom.collect { room ->
                updateTimerState(room, force = false)
                val reaction = room?.reaction
                if (reaction != null && reaction.timestamp != lastReactionTimestamp) {
                    val isFirstLoad = lastReactionTimestamp == -1L
                    lastReactionTimestamp = reaction.timestamp
                    if (!isFirstLoad) {
                        _pendingReaction.value = reaction.emoji
                        reactionClearJob?.cancel()
                        reactionClearJob = viewModelScope.launch {
                            delay(2500)
                            _pendingReaction.value = null
                        }
                    }
                } else if (reaction == null && lastReactionTimestamp == -1L) {
                    lastReactionTimestamp = 0
                }
            }
        }
    }

    fun onResume() {
        isPaused = false
        updateTimerState(GameRepository.currentRoom.value, force = true)
    }

    fun onPause() {
        isPaused = true
        timerJob?.cancel()
        timerJob = null
    }

    private fun updateTimerState(room: GameRoom?, force: Boolean) {
        if (isPaused) {
            timerJob?.cancel(); timerJob = null; return
        }
        if (room == null || !room.isPlaying()) {
            timerJob?.cancel(); timerJob = null; _remainingSeconds.value = null; return
        }
        val duration = room.timerDuration ?: run {
            timerJob?.cancel(); timerJob = null; _remainingSeconds.value = null; return
        }
        val turnStartedAt = room.turnStartedAt ?: run {
            timerJob?.cancel(); timerJob = null; _remainingSeconds.value = null; return
        }
        val needsRestart = force || timerAnchor?.first != room.turn || timerAnchor?.second != turnStartedAt
        if (needsRestart) {
            timerJob?.cancel()
            timerAnchor = room.turn to turnStartedAt
            startTicking(room.turn, turnStartedAt, duration)
        }
    }

    private fun startTicking(turn: String, turnStartedAt: Long, duration: Int) {
        timerJob = viewModelScope.launch {
            val gameId = GameRepository.currentRoom.value?.id ?: return@launch
            while (true) {
                if (isPaused) return@launch
                val now = System.currentTimeMillis()
                val elapsed = now - turnStartedAt
                val remaining = maxOf(0L, duration * 1000L - elapsed)
                val remainingInt = minOf(duration, ((remaining + 999) / 1000).toInt())
                _remainingSeconds.value = remainingInt
                if (remainingInt <= 0) {
                    GameRepository.autoPassTurn(gameId, turn, turnStartedAt)
                    return@launch
                }
                delay(1000)
            }
        }
    }

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
            val success = GameRepository.joinOrCreateGame(gameId, playerName, timerSeconds)
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
            } catch (e: IllegalStateException) {
                if (e.message == "double_open_three") {
                    _errorMessage.value = "Cannot create two open threes in one move (3×3 rule)."
                } else {
                    _errorMessage.value = "Failed to make move: ${e.message}"
                }
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

    fun requestUndo() {
        viewModelScope.launch {
            try { GameRepository.requestUndo() } catch (e: Exception) { _errorMessage.value = "Failed to request undo: ${e.message}" }
        }
    }

    fun approveUndo() {
        viewModelScope.launch {
            try { GameRepository.approveUndo() } catch (e: Exception) { _errorMessage.value = "Failed to approve undo: ${e.message}" }
        }
    }

    fun rejectUndo() {
        viewModelScope.launch {
            try { GameRepository.rejectUndo() } catch (e: Exception) { _errorMessage.value = "Failed to reject undo: ${e.message}" }
        }
    }

    fun sendReaction(emoji: String) {
        viewModelScope.launch {
            try { GameRepository.sendReaction(emoji) } catch (e: Exception) { /* silent */ }
        }
    }

    fun leaveGame() {
        GameRepository.stopListening()
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
