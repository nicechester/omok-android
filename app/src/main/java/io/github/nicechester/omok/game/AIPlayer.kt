package io.github.nicechester.omok.game

object AIPlayer {
    private const val SIZE = 15
    private const val INF = 10_000_000

    private val DIRECTIONS = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)

    // board: "r_c" -> "black"/"white"
    suspend fun findBestMove(
        board: Map<String, String>,
        moveCount: Int,
        aiColor: String,
        difficulty: AIDifficulty
    ): Pair<Int, Int>? {
        val playerColor = if (aiColor == "black") "white" else "black"
        val deadline = System.currentTimeMillis() + difficulty.timeLimitMs
        val candidates = generateCandidates(board, moveCount, aiColor)
        if (candidates.isEmpty()) return null

        var bestMove: Pair<Int, Int>? = null

        for (depth in 1..difficulty.maxDepth) {
            if (System.currentTimeMillis() >= deadline) break
            var depthBest: Pair<Int, Int>? = null
            var depthBestVal = -INF

            for (candidate in candidates) {
                if (System.currentTimeMillis() >= deadline) break
                val newBoard = board.toMutableMap().also { it["${candidate.first}_${candidate.second}"] = aiColor }
                val value = minimax(newBoard, candidate, depth - 1, false, -INF, INF, aiColor, playerColor, deadline)
                if (value > depthBestVal) {
                    depthBestVal = value
                    depthBest = candidate
                }
            }

            if (System.currentTimeMillis() < deadline && depthBest != null) {
                bestMove = depthBest
            }
        }

        return bestMove
    }

    private fun minimax(
        board: Map<String, String>,
        lastMove: Pair<Int, Int>,
        depth: Int,
        isMaximizing: Boolean,
        alpha: Int,
        beta: Int,
        aiColor: String,
        playerColor: String,
        deadline: Long
    ): Int {
        val lastMoveColor = if (isMaximizing) playerColor else aiColor
        if (checkWin(board, lastMove.first, lastMove.second, lastMoveColor) != null) {
            val score = INF + depth
            return if (isMaximizing) -score else score
        }
        if (depth == 0 || board.size >= SIZE * SIZE) {
            return evaluate(board, aiColor)
        }

        val candidates = generateCandidates(board, board.size, aiColor)
        var a = alpha
        var b = beta

        return if (isMaximizing) {
            var maxEval = -INF
            for (c in candidates) {
                if (System.currentTimeMillis() >= deadline) break
                val newBoard = board.toMutableMap().also { it["${c.first}_${c.second}"] = aiColor }
                val eval = minimax(newBoard, c, depth - 1, false, a, b, aiColor, playerColor, deadline)
                maxEval = maxOf(maxEval, eval)
                a = maxOf(a, eval)
                if (b <= a) break
            }
            maxEval
        } else {
            var minEval = INF
            for (c in candidates) {
                if (System.currentTimeMillis() >= deadline) break
                val newBoard = board.toMutableMap().also { it["${c.first}_${c.second}"] = playerColor }
                val eval = minimax(newBoard, c, depth - 1, true, a, b, aiColor, playerColor, deadline)
                minEval = minOf(minEval, eval)
                b = minOf(b, eval)
                if (b <= a) break
            }
            minEval
        }
    }

    private fun generateCandidates(board: Map<String, String>, moveCount: Int, aiColor: String): List<Pair<Int, Int>> {
        if (board.isEmpty()) return listOf(7 to 7)

        val candidates = mutableSetOf<Pair<Int, Int>>()
        for (key in board.keys) {
            val (r, c) = key.split("_").map { it.toInt() }
            for (dr in -2..2) for (dc in -2..2) {
                val nr = r + dr; val nc = c + dc
                if (nr in 0 until SIZE && nc in 0 until SIZE && !board.containsKey("${nr}_${nc}")) {
                    candidates.add(nr to nc)
                }
            }
        }

        val playerColor = if (aiColor == "black") "white" else "black"
        return candidates
            .filter { (r, c) ->
                val testBoard = board.toMutableMap().also { it["${r}_${c}"] = aiColor }
                countOpenThrees(testBoard, r, c, aiColor) < 2
            }
            .sortedByDescending { (r, c) -> heuristicScore(r, c, board, aiColor, playerColor) }
            .take(20)
    }

    private fun heuristicScore(row: Int, col: Int, board: Map<String, String>, aiColor: String, playerColor: String): Int {
        var score = 0
        val aiBoard = board.toMutableMap().also { it["${row}_${col}"] = aiColor }
        val oppBoard = board.toMutableMap().also { it["${row}_${col}"] = playerColor }
        for ((dr, dc) in DIRECTIONS) {
            score += scoreWindow(row, col, dr, dc, aiColor, aiBoard)
            score += scoreWindow(row, col, dr, dc, playerColor, oppBoard) * 2
        }
        return score
    }

    private fun evaluate(board: Map<String, String>, aiColor: String): Int {
        val playerColor = if (aiColor == "black") "white" else "black"
        var aiScore = 0; var oppScore = 0
        for (r in 0 until SIZE) for (c in 0 until SIZE) for ((dr, dc) in DIRECTIONS) {
            aiScore += scoreLine(r, c, dr, dc, aiColor, board)
            oppScore += scoreLine(r, c, dr, dc, playerColor, board)
        }
        return aiScore - oppScore * 2
    }

    private fun scoreWindow(row: Int, col: Int, dr: Int, dc: Int, color: String, board: Map<String, String>): Int {
        var total = 0
        for (offset in 0 until 5) {
            total += scoreLine(row - dr * offset, col - dc * offset, dr, dc, color, board)
        }
        return total
    }

    private fun scoreLine(startR: Int, startC: Int, dr: Int, dc: Int, color: String, board: Map<String, String>): Int {
        var friendly = 0; var enemy = 0
        var r = startR; var c = startC
        repeat(5) {
            if (r in 0 until SIZE && c in 0 until SIZE) {
                when (board["${r}_${c}"]) {
                    color -> friendly++
                    null -> {}
                    else -> enemy++
                }
            }
            r += dr; c += dc
        }
        if (enemy > 0) return 0
        return when (friendly) {
            4 -> 50_000
            3 -> 1_000
            2 -> 100
            1 -> 10
            else -> 0
        }
    }

    private fun countOpenThrees(board: Map<String, String>, row: Int, col: Int, color: String): Int {
        var count = 0
        for ((dr, dc) in DIRECTIONS) {
            var fwd = 0; var r = row + dr; var c = col + dc
            while (r in 0 until SIZE && c in 0 until SIZE && board["${r}_${c}"] == color) { fwd++; r += dr; c += dc }
            val fwdOpen = r in 0 until SIZE && c in 0 until SIZE && board["${r}_${c}"] == null
            val fwdGap = fwdOpen && (r + dr) in 0 until SIZE && (c + dc) in 0 until SIZE && board["${r + dr}_${c + dc}"] == color

            var bwd = 0; r = row - dr; c = col - dc
            while (r in 0 until SIZE && c in 0 until SIZE && board["${r}_${c}"] == color) { bwd++; r -= dr; c -= dc }
            val bwdOpen = r in 0 until SIZE && c in 0 until SIZE && board["${r}_${c}"] == null
            val bwdGap = bwdOpen && (r - dr) in 0 until SIZE && (c - dc) in 0 until SIZE && board["${r - dr}_${c - dc}"] == color

            if (fwd + 1 + bwd == 3 && fwdOpen && bwdOpen && !fwdGap && !bwdGap) count++
        }
        return count
    }

    private fun checkWin(board: Map<String, String>, row: Int, col: Int, color: String): List<Pair<Int, Int>>? {
        for ((dr, dc) in DIRECTIONS) {
            val line = mutableListOf(row to col)
            var r = row + dr; var c = col + dc
            while (r in 0 until SIZE && c in 0 until SIZE && board["${r}_${c}"] == color) { line += r to c; r += dr; c += dc }
            r = row - dr; c = col - dc
            while (r in 0 until SIZE && c in 0 until SIZE && board["${r}_${c}"] == color) { line += r to c; r -= dr; c -= dc }
            if (line.size == 5) return line
        }
        return null
    }
}
