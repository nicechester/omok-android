package io.github.nicechester.omok.data.model

data class PlayerSeat(
    val uid: String = "",
    val color: String = "", // "black" or "white"
    val name: String? = null,
    val active: Boolean = true,
    val joinedAt: Long = 0
)

data class LastMove(
    val r: Int = 0,
    val c: Int = 0,
    val color: String = ""
)

data class GameRoom(
    val id: String = "",
    val status: String = "waiting", // waiting, playing, finished
    val turn: String = "black",     // "black" or "white"
    val round: Int = 0,
    val moveCount: Int = 0,
    val board: Map<String, String> = emptyMap(), // "r_c" -> "black"/"white"
    val lastMove: LastMove? = null,
    val result: String? = null,     // "black", "white", or "draw"
    val winningLine: List<LastMove> = emptyList(),
    val players: Map<String, PlayerSeat> = emptyMap(), // uid -> PlayerSeat
    val scores: Map<String, Int> = emptyMap(),
    val createdBy: String = "",
    val timerDuration: Int? = null,
    val turnStartedAt: Long? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
) {
    val blackSeat: PlayerSeat? get() = players.values.find { it.color == "black" }
    val whiteSeat: PlayerSeat? get() = players.values.find { it.color == "white" }

    fun seatOf(uid: String): String? = players[uid]?.color

    fun isFinished() = status == "finished"
    fun isPlaying() = status == "playing"
    fun isWaiting() = status == "waiting"
}
