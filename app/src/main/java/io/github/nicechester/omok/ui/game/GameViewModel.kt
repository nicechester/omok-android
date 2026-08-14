package io.github.nicechester.omok.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.nicechester.omok.game.GameBoard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    private val _gameBoard = MutableStateFlow(GameBoard())
    val gameBoard: StateFlow<GameBoard> = _gameBoard

    private val _currentPlayer = MutableStateFlow(GameBoard.BLACK)
    val currentPlayer: StateFlow<Int> = _currentPlayer

    private val _gameOver = MutableStateFlow(false)
    val gameOver: StateFlow<Boolean> = _gameOver

    private val _winner = MutableStateFlow<Int?>(null)
    val winner: StateFlow<Int?> = _winner

    fun placeStone(row: Int, col: Int) {
        viewModelScope.launch {
            val board = _gameBoard.value
            if (board.placeStone(row, col, _currentPlayer.value)) {
                if (board.checkWin(row, col, _currentPlayer.value)) {
                    _gameOver.value = true
                    _winner.value = _currentPlayer.value
                } else if (board.isBoardFull()) {
                    _gameOver.value = true
                } else {
                    _currentPlayer.value = if (_currentPlayer.value == GameBoard.BLACK) {
                        GameBoard.WHITE
                    } else {
                        GameBoard.BLACK
                    }
                }
            }
        }
    }

    fun resetGame() {
        _gameBoard.value.reset()
        _currentPlayer.value = GameBoard.BLACK
        _gameOver.value = false
        _winner.value = null
    }
}
