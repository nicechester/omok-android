package io.github.nicechester.omok.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import io.github.nicechester.omok.data.model.GameRoom
import io.github.nicechester.omok.data.model.LastMove
import io.github.nicechester.omok.data.model.PlayerSeat
import io.github.nicechester.omok.data.model.UndoRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

object GameRepository {
    private val tag = "GameRepository"
    private val gamesRef = Firebase.database("https://omok-5-in-a-row-default-rtdb.firebaseio.com").getReference("omok/games")
    private val auth = Firebase.auth

    private val _currentRoom = MutableStateFlow<GameRoom?>(null)
    val currentRoom: StateFlow<GameRoom?> = _currentRoom

    private var roomListener: ValueEventListener? = null
    private var currentGameId: String? = null

    // Mirrors iOS GameViewModel.start(): claimSeat, create if not found
    suspend fun joinOrCreateGame(gameId: String, playerName: String): Boolean {
        val uid = auth.currentUser?.uid
        Log.d(tag, "joinOrCreateGame: gameId=$gameId, uid=$uid, playerName=$playerName")
        if (uid == null) {
            Log.e(tag, "joinOrCreateGame: not authenticated")
            return false
        }
        val ref = gamesRef.child(gameId)
        return try {
            val snapshot = ref.get().await()
            Log.d(tag, "joinOrCreateGame: snapshot exists=${snapshot.exists()}")
            if (!snapshot.exists()) {
                createGame(ref, uid, playerName)
            } else {
                claimSeat(ref, uid, playerName)
            }
            listenToGame(gameId)
            true
        } catch (e: Exception) {
            Log.e(tag, "joinOrCreateGame failed: ${e.javaClass.simpleName}: ${e.message}", e)
            false
        }
    }

    private suspend fun createGame(
        ref: com.google.firebase.database.DatabaseReference,
        uid: String,
        playerName: String
    ) {
        val sanitized = playerName.trim().take(20)
        val playerData = mutableMapOf<String, Any>(
            "color" to "black",
            "joinedAt" to ServerValue.TIMESTAMP,
            "active" to true
        )
        if (sanitized.isNotEmpty()) playerData["name"] = sanitized

        val data = mapOf(
            "status" to "waiting",
            "turn" to "black",
            "round" to 0,
            "moveCount" to 0,
            "players" to mapOf(uid to playerData),
            "speaking" to emptyMap<String, Any>(),
            "scores" to emptyMap<String, Any>(),
            "createdBy" to uid,
            "createdAt" to ServerValue.TIMESTAMP,
            "updatedAt" to ServerValue.TIMESTAMP
        )
        ref.setValue(data).await()
        Log.d(tag, "Game created: ${ref.key}")
    }

    private suspend fun claimSeat(
        ref: com.google.firebase.database.DatabaseReference,
        uid: String,
        playerName: String
    ) {
        val snapshot = ref.get().await()
        val dict = snapshot.value as? Map<String, Any> ?: return
        val players = dict["players"] as? Map<String, Any> ?: emptyMap()
        val sanitized = playerName.trim().take(20)

        if (players.containsKey(uid)) {
            val existingName = (players[uid] as? Map<String, Any>)?.get("name") as? String ?: ""
            if (existingName != sanitized && sanitized.isNotEmpty()) {
                ref.child("players/$uid/name").setValue(sanitized).await()
                ref.child("updatedAt").setValue(ServerValue.TIMESTAMP).await()
            }
            return
        }

        val takenColors = players.values
            .mapNotNull { (it as? Map<String, Any>)?.get("color") as? String }
            .toSet()
        check(takenColors.size < 2) { "Game is full" }

        val seatColor = if ("black" in takenColors) "white" else "black"
        val playerData = mutableMapOf<String, Any>(
            "color" to seatColor,
            "joinedAt" to ServerValue.TIMESTAMP,
            "active" to true
        )
        if (sanitized.isNotEmpty()) playerData["name"] = sanitized

        val updates = mutableMapOf<String, Any>(
            "players/$uid" to playerData,
            "updatedAt" to ServerValue.TIMESTAMP
        )
        if (takenColors.size == 1) updates["status"] = "playing"
        ref.updateChildren(updates).await()
    }

    suspend fun makeMove(row: Int, col: Int): Boolean {
        val gameId = currentGameId ?: return false
        val uid = auth.currentUser?.uid ?: return false
        val ref = gamesRef.child(gameId)

        return try {
            val snapshot = ref.get().await()
            val dict = snapshot.value as? Map<String, Any> ?: return false
            if (dict["status"] as? String != "playing") return false

            val turn = dict["turn"] as? String ?: return false
            val players = dict["players"] as? Map<String, Any> ?: return false
            val playerColor = (players[uid] as? Map<String, Any>)?.get("color") as? String ?: return false
            if (playerColor != turn) return false

            val cellKey = "${row}_${col}"
            val board = dict["board"] as? Map<String, Any> ?: emptyMap()
            if (board.containsKey(cellKey)) return false
            if (dict["undoRequest"] != null) return false

            val moveCount = ((dict["moveCount"] as? Number)?.toInt() ?: 0) + 1
            val updates = mutableMapOf<String, Any>(
                "board/$cellKey" to playerColor,
                "lastMove" to mapOf("r" to row, "c" to col, "color" to playerColor),
                "moveCount" to moveCount,
                "turn" to if (playerColor == "black") "white" else "black",
                "updatedAt" to ServerValue.TIMESTAMP
            )

            val lastMoveDict = dict["lastMove"]
            if (lastMoveDict != null) updates["previousLastMove"] = lastMoveDict

            // Win detection
            val boardCells = buildBoardMap(board).toMutableMap()
            boardCells[cellKey] = playerColor
            val winLine = checkWin(boardCells, row, col, playerColor)
            if (winLine != null) {
                updates["status"] = "finished"
                updates["result"] = playerColor
                updates["winningLine"] = winLine.map { mapOf("r" to it.first, "c" to it.second) }
                val currentScore = (dict["scores"] as? Map<String, Any>)?.get(uid) as? Int ?: 0
                updates["scores/$uid"] = currentScore + 1
            } else if (moveCount >= 225) {
                updates["status"] = "finished"
                updates["result"] = "draw"
            }

            ref.updateChildren(updates).await()
            true
        } catch (e: Exception) {
            Log.e(tag, "makeMove failed", e)
            false
        }
    }

    suspend fun forfeit() {
        val gameId = currentGameId ?: return
        val uid = auth.currentUser?.uid ?: return
        val ref = gamesRef.child(gameId)
        try {
            val snapshot = ref.get().await()
            val dict = snapshot.value as? Map<String, Any> ?: return
            if (dict["status"] as? String != "playing") return

            val players = dict["players"] as? Map<String, Any> ?: return
            val playerColor = (players[uid] as? Map<String, Any>)?.get("color") as? String ?: return
            val winner = if (playerColor == "black") "white" else "black"
            val winnerUid = players.entries
                .firstOrNull { (k, v) -> k != uid && (v as? Map<String, Any>)?.get("color") != null }
                ?.key

            val updates = mutableMapOf<String, Any>(
                "status" to "finished",
                "result" to winner,
                "updatedAt" to ServerValue.TIMESTAMP
            )
            if (winnerUid != null) {
                val currentScore = (dict["scores"] as? Map<String, Any>)?.get(winnerUid) as? Int ?: 0
                updates["scores/$winnerUid"] = currentScore + 1
            }
            ref.updateChildren(updates).await()
        } catch (e: Exception) {
            Log.e(tag, "forfeit failed", e)
        }
    }

    suspend fun requestUndo(): Boolean {
        val gameId = currentGameId ?: return false
        val uid = auth.currentUser?.uid ?: return false
        val ref = gamesRef.child(gameId)
        return try {
            val snapshot = ref.get().await()
            val dict = snapshot.value as? Map<String, Any> ?: return false
            if (dict["status"] as? String != "playing") return false
            val moveCount = (dict["moveCount"] as? Number)?.toInt() ?: 0
            if (moveCount < 2) return false
            if (dict["undoRequest"] != null) return false
            ref.updateChildren(mapOf(
                "undoRequest" to mapOf("requestedBy" to uid, "createdAt" to ServerValue.TIMESTAMP),
                "updatedAt" to ServerValue.TIMESTAMP
            )).await()
            true
        } catch (e: Exception) {
            Log.e(tag, "requestUndo failed", e)
            false
        }
    }

    suspend fun approveUndo(): Boolean {
        val gameId = currentGameId ?: return false
        val uid = auth.currentUser?.uid ?: return false
        val ref = gamesRef.child(gameId)
        return try {
            val snapshot = ref.get().await()
            val dict = snapshot.value as? Map<String, Any> ?: return false
            if (dict["status"] as? String != "playing") return false
            val undoReqDict = dict["undoRequest"] as? Map<String, Any> ?: return false
            val requestedBy = undoReqDict["requestedBy"] as? String ?: return false
            val players = dict["players"] as? Map<String, Any> ?: return false
            val approverColor = (players[uid] as? Map<String, Any>)?.get("color") as? String ?: return false
            val requesterColor = (players[requestedBy] as? Map<String, Any>)?.get("color") as? String ?: return false
            if (approverColor == requesterColor) return false // can't approve own request

            val moveCount = ((dict["moveCount"] as? Number)?.toInt() ?: 0) - 1
            val currentTurn = dict["turn"] as? String ?: return false
            val prevTurn = if (currentTurn == "black") "white" else "black"

            val updates = mutableMapOf<String, Any?>(
                "moveCount" to moveCount,
                "turn" to prevTurn,
                "undoRequest" to null,
                "updatedAt" to ServerValue.TIMESTAMP
            )
            if (dict["timerDuration"] != null) updates["turnStartedAt"] = ServerValue.TIMESTAMP

            val lastMoveDict = dict["lastMove"] as? Map<String, Any>
            if (lastMoveDict != null) {
                val r = (lastMoveDict["r"] as? Number)?.toInt()
                val c = (lastMoveDict["c"] as? Number)?.toInt()
                if (r != null && c != null) updates["board/${r}_${c}"] = null
            }
            updates["lastMove"] = dict["previousLastMove"]
            updates["previousLastMove"] = null

            // Firebase requires removeValue for nulls — split into two calls
            val nonNullUpdates = updates.filterValues { it != null }.mapValues { it.value!! }
            ref.updateChildren(nonNullUpdates).await()
            for ((key, value) in updates) {
                if (value == null) ref.child(key).removeValue().await()
            }
            true
        } catch (e: Exception) {
            Log.e(tag, "approveUndo failed", e)
            false
        }
    }

    suspend fun rejectUndo(): Boolean {
        val gameId = currentGameId ?: return false
        val ref = gamesRef.child(gameId)
        return try {
            val snapshot = ref.get().await()
            val dict = snapshot.value as? Map<String, Any> ?: return false
            if (dict["undoRequest"] == null) return false
            val updates = mutableMapOf<String, Any>("updatedAt" to ServerValue.TIMESTAMP)
            if (dict["timerDuration"] != null) updates["turnStartedAt"] = ServerValue.TIMESTAMP
            ref.updateChildren(updates).await()
            ref.child("undoRequest").removeValue().await()
            true
        } catch (e: Exception) {
            Log.e(tag, "rejectUndo failed", e)
            false
        }
    }

    suspend fun voteRematch() {
        val gameId = currentGameId ?: return
        val uid = auth.currentUser?.uid ?: return
        gamesRef.child(gameId).child("rematch").child(uid).setValue(true).await()
    }

    suspend fun resetForRematch() {
        val gameId = currentGameId ?: return
        val ref = gamesRef.child(gameId)
        try {
            val snapshot = ref.get().await()
            val dict = snapshot.value as? Map<String, Any> ?: return
            if (dict["status"] as? String != "finished") return

            val round = ((dict["round"] as? Number)?.toInt() ?: 0) + 1
            val updates = mutableMapOf<String, Any>(
                "round" to round,
                "moveCount" to 0,
                "turn" to "black",
                "status" to "playing",
                "updatedAt" to ServerValue.TIMESTAMP
            )
            // Swap colors
            val players = dict["players"] as? Map<String, Any> ?: emptyMap()
            for ((playerUid, playerData) in players) {
                val color = (playerData as? Map<String, Any>)?.get("color") as? String ?: continue
                updates["players/$playerUid/color"] = if (color == "black") "white" else "black"
            }
            ref.updateChildren(updates).await()
            // Clear transient fields separately (Firebase requires null via removeValue)
            for (field in listOf("board", "lastMove", "previousLastMove", "result", "winningLine", "rematch", "undoRequest")) {
                ref.child(field).removeValue().await()
            }
        } catch (e: Exception) {
            Log.e(tag, "resetForRematch failed", e)
        }
    }

    fun listenToGame(gameId: String) {
        currentGameId = gameId
        roomListener?.let { gamesRef.child(gameId).removeEventListener(it) }
        roomListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _currentRoom.value = if (snapshot.exists())
                    parseGameRoom(gameId, snapshot.value as? Map<String, Any> ?: return)
                else
                    null
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(tag, "listenToGame cancelled: ${error.message}")
            }
        }
        gamesRef.child(gameId).addValueEventListener(roomListener!!)
    }

    fun stopListening() {
        val gameId = currentGameId ?: return
        roomListener?.let { gamesRef.child(gameId).removeEventListener(it) }
        currentGameId = null
        _currentRoom.value = null
    }

    // MARK: - Parsing

    private fun parseGameRoom(gameId: String, dict: Map<String, Any>): GameRoom {
        val playersData = dict["players"] as? Map<String, Any> ?: emptyMap()
        val players = playersData.mapNotNull { (uid, data) ->
            val pd = data as? Map<String, Any> ?: return@mapNotNull null
            uid to PlayerSeat(
                uid = uid,
                color = pd["color"] as? String ?: "",
                name = pd["name"] as? String,
                active = pd["active"] as? Boolean ?: true,
                joinedAt = (pd["joinedAt"] as? Number)?.toLong() ?: 0
            )
        }.toMap()

        val lastMoveDict = dict["lastMove"] as? Map<String, Any>
        val lastMove = lastMoveDict?.let {
            LastMove(
                r = (it["r"] as? Number)?.toInt() ?: 0,
                c = (it["c"] as? Number)?.toInt() ?: 0,
                color = it["color"] as? String ?: ""
            )
        }

        val winningLine = (dict["winningLine"] as? List<*>)?.mapNotNull { entry ->
            val e = entry as? Map<String, Any> ?: return@mapNotNull null
            LastMove(
                r = (e["r"] as? Number)?.toInt() ?: return@mapNotNull null,
                c = (e["c"] as? Number)?.toInt() ?: return@mapNotNull null,
                color = ""
            )
        } ?: emptyList()

        val scores = (dict["scores"] as? Map<String, Any>)
            ?.mapNotNull { (k, v) -> (v as? Number)?.let { k to it.toInt() } }
            ?.toMap() ?: emptyMap()

        val undoReqDict = dict["undoRequest"] as? Map<String, Any>
        val undoRequest = undoReqDict?.let {
            UndoRequest(
                requestedBy = it["requestedBy"] as? String ?: "",
                createdAt = (it["createdAt"] as? Number)?.toLong() ?: 0
            )
        }

        return GameRoom(
            id = gameId,
            status = dict["status"] as? String ?: "waiting",
            turn = dict["turn"] as? String ?: "black",
            round = (dict["round"] as? Number)?.toInt() ?: 0,
            moveCount = (dict["moveCount"] as? Number)?.toInt() ?: 0,
            board = buildBoardMap(dict["board"] as? Map<String, Any> ?: emptyMap()),
            lastMove = lastMove,
            result = dict["result"] as? String,
            winningLine = winningLine,
            players = players,
            scores = scores,
            createdBy = dict["createdBy"] as? String ?: "",
            timerDuration = (dict["timerDuration"] as? Number)?.toInt(),
            turnStartedAt = (dict["turnStartedAt"] as? Number)?.toLong(),
            undoRequest = undoRequest,
            createdAt = (dict["createdAt"] as? Number)?.toLong() ?: 0,
            updatedAt = (dict["updatedAt"] as? Number)?.toLong() ?: 0
        )
    }

    private fun buildBoardMap(raw: Map<String, Any>): Map<String, String> =
        raw.mapNotNull { (k, v) -> (v as? String)?.let { k to it } }.toMap()

    // Mirrors iOS GomokuRules.winningLine — exactly 5, no overline
    private fun checkWin(board: Map<String, String>, row: Int, col: Int, color: String): List<Pair<Int, Int>>? {
        val directions = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)
        for ((dr, dc) in directions) {
            val line = mutableListOf(row to col)
            var r = row + dr; var c = col + dc
            while (r in 0..14 && c in 0..14 && board["${r}_${c}"] == color) { line += r to c; r += dr; c += dc }
            r = row - dr; c = col - dc
            while (r in 0..14 && c in 0..14 && board["${r}_${c}"] == color) { line += r to c; r -= dr; c -= dc }
            if (line.size == 5) return line
        }
        return null
    }
}
