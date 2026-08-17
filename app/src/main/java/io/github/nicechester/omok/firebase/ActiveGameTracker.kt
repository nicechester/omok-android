package io.github.nicechester.omok.firebase

object ActiveGameTracker {
    @Volatile var activeGameId: String? = null
}
