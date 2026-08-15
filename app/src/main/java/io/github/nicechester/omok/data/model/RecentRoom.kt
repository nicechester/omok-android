package io.github.nicechester.omok.data.model

data class RecentRoom(
    val code: String,
    val lastPlayedAt: Long,
    val aiDifficulty: String? = null
) {
    val id: String get() = code
}
