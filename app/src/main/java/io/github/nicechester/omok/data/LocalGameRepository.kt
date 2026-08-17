package io.github.nicechester.omok.data

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue
import io.github.nicechester.omok.game.AIDifficulty
import io.github.nicechester.omok.game.AIPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

class LocalGameRepository(
    private val difficulty: AIDifficulty,
    private val humanUid: String
) : FirebaseGameRepository() {

    companion object {
        const val AI_UID = "ai-player"
    }

    // Override createGame to add AI as a real player and start as "playing"
    override suspend fun createGame(ref: DatabaseReference, uid: String, playerName: String, timerDuration: Int) {
        val sanitized = playerName.trim().take(20)
        val humanData = mutableMapOf<String, Any>(
            "color" to "black",
            "joinedAt" to ServerValue.TIMESTAMP,
            "active" to true
        )
        if (sanitized.isNotEmpty()) humanData["name"] = sanitized

        val aiData = mapOf<String, Any>(
            "color" to "white",
            "joinedAt" to ServerValue.TIMESTAMP,
            "name" to "AI (${difficulty.displayName})",
            "active" to true
        )

        val data = mutableMapOf<String, Any>(
            "status" to "playing",
            "turn" to "black",
            "round" to 0,
            "moveCount" to 0,
            "players" to mapOf(uid to humanData, AI_UID to aiData),
            "scores" to emptyMap<String, Any>(),
            "createdBy" to uid,
            "aiDifficulty" to difficulty.name,
            "createdAt" to ServerValue.TIMESTAMP,
            "updatedAt" to ServerValue.TIMESTAMP
        )
        if (timerDuration > 0) {
            data["timerDuration"] = timerDuration
            data["turnStartedAt"] = ServerValue.TIMESTAMP
        }
        ref.setValue(data).await()
    }

    // After human places a stone, trigger AI move
    override suspend fun makeMove(row: Int, col: Int): Boolean {
        val success = makeMoveAs(humanUid, row, col)
        if (success) performAIMove()
        return success
    }

    // AI auto-approves undo: undo 2 moves (player + AI) so it's the player's turn again
    override suspend fun requestUndo() {
        val gameId = currentGameId ?: return
        val ref = gamesRef.child(gameId)
        val snapshot = ref.get().await()
        val dict = snapshot.value as? Map<String, Any> ?: return
        if (dict["status"] as? String != "playing") return
        val moveCount = ((dict["moveCount"] as? Number)?.toInt() ?: 0)
        if (moveCount < 2) return

        val newMoveCount = moveCount - 2
        val updates = mutableMapOf<String, Any?>(
            "moveCount" to newMoveCount,
            "turn" to "black",
            "previousLastMove" to null,
            "updatedAt" to ServerValue.TIMESTAMP
        )
        if (dict["timerDuration"] != null) updates["turnStartedAt"] = ServerValue.TIMESTAMP

        val lastMoveDict = dict["lastMove"] as? Map<String, Any>
        if (lastMoveDict != null) {
            val r = (lastMoveDict["r"] as? Number)?.toInt()
            val c = (lastMoveDict["c"] as? Number)?.toInt()
            if (r != null && c != null) updates["board/${r}_${c}"] = null
        }
        val prevMoveDict = dict["previousLastMove"] as? Map<String, Any>
        if (prevMoveDict != null) {
            val r = (prevMoveDict["r"] as? Number)?.toInt()
            val c = (prevMoveDict["c"] as? Number)?.toInt()
            if (r != null && c != null) updates["board/${r}_${c}"] = null
        }
        updates["lastMove"] = null

        @Suppress("UNCHECKED_CAST")
        ref.updateChildren(updates as Map<String, Any>).await()
    }

    // AI auto-votes rematch
    override suspend fun voteRematch() {
        super.voteRematch()
        val gameId = currentGameId ?: return
        gamesRef.child(gameId).child("rematch").child(AI_UID).setValue(true).await()
    }

    private suspend fun performAIMove() {
        val gameId = currentGameId ?: return
        val snapshot = gamesRef.child(gameId).get().await()
        val dict = snapshot.value as? Map<String, Any> ?: return
        if (dict["status"] as? String != "playing") return

        val players = dict["players"] as? Map<String, Any> ?: return
        val aiColor = (players[AI_UID] as? Map<String, Any>)?.get("color") as? String ?: return
        if (dict["turn"] as? String != aiColor) return

        val board = buildBoardMap(dict["board"] as? Map<String, Any> ?: emptyMap())
        val moveCount = (dict["moveCount"] as? Number)?.toInt() ?: 0

        delay(500) // brief pause so the human's move renders first
        val move = AIPlayer.findBestMove(board, moveCount, aiColor, difficulty) ?: return
        try { makeMoveAs(AI_UID, move.first, move.second) } catch (_: Exception) {}
    }
}
