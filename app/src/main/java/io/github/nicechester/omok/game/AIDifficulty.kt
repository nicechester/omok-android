package io.github.nicechester.omok.game

enum class AIDifficulty(val displayName: String, val maxDepth: Int, val timeLimitMs: Long) {
    EASY("Easy", 2, 500),
    NORMAL("Normal", 4, 2000),
    HARD("Hard", 6, 5000)
}
