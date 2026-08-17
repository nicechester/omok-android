package io.github.nicechester.omok.firebase

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object PendingGameNavigation {
    private val _pendingGameId = MutableStateFlow<String?>(null)
    val pendingGameId: StateFlow<String?> = _pendingGameId

    fun request(gameId: String) { _pendingGameId.value = gameId }
    fun consume() { _pendingGameId.value = null }
}
