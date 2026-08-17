package io.github.nicechester.omok.data

import io.github.nicechester.omok.data.model.GameRoom
import kotlinx.coroutines.flow.StateFlow

interface GameRepository {
    val currentRoom: StateFlow<GameRoom?>
    suspend fun joinOrCreateGame(gameId: String, playerName: String, timerDuration: Int = 0): Boolean
    suspend fun makeMove(row: Int, col: Int): Boolean
    suspend fun forfeit()
    suspend fun voteRematch()
    suspend fun resetForRematch()
    suspend fun sendReaction(emoji: String)
    suspend fun requestUndo()
    suspend fun approveUndo()
    suspend fun rejectUndo()
    suspend fun autoPassTurn(gameId: String, expectedTurn: String, expectedTurnStartedAt: Long)
    fun listenToGame(gameId: String)
    fun stopListening()
}
