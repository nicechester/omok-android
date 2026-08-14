package io.github.nicechester.omok.game

data class GameBoard(
    val size: Int = 15,
    private val board: Array<IntArray> = Array(size) { IntArray(size) { EMPTY } }
) {
    companion object {
        const val EMPTY = 0
        const val BLACK = 1
        const val WHITE = 2
    }

    fun placeStone(row: Int, col: Int, player: Int): Boolean {
        if (isValidMove(row, col)) {
            board[row][col] = player
            return true
        }
        return false
    }

    fun isValidMove(row: Int, col: Int): Boolean {
        return row in 0 until size && col in 0 until size && board[row][col] == EMPTY
    }

    fun getStone(row: Int, col: Int): Int = board[row][col]

    fun isBoardFull(): Boolean = board.all { row -> row.all { it != EMPTY } }

    fun checkWin(row: Int, col: Int, player: Int): Boolean {
        return checkDirection(row, col, player, 1, 0) ||   // horizontal
               checkDirection(row, col, player, 0, 1) ||   // vertical
               checkDirection(row, col, player, 1, 1) ||   // diagonal
               checkDirection(row, col, player, 1, -1)     // anti-diagonal
    }

    private fun checkDirection(row: Int, col: Int, player: Int, dRow: Int, dCol: Int): Boolean {
        var count = 1

        // Check forward
        var r = row + dRow
        var c = col + dCol
        while (r in 0 until size && c in 0 until size && board[r][c] == player) {
            count++
            r += dRow
            c += dCol
        }

        // Check backward
        r = row - dRow
        c = col - dCol
        while (r in 0 until size && c in 0 until size && board[r][c] == player) {
            count++
            r -= dRow
            c -= dCol
        }

        return count == 5
    }

    fun reset() {
        for (i in 0 until size) {
            for (j in 0 until size) {
                board[i][j] = EMPTY
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameBoard) return false
        if (size != other.size) return false
        return board.contentDeepEquals(other.board)
    }

    override fun hashCode(): Int = 31 * size + board.contentDeepHashCode()
}
