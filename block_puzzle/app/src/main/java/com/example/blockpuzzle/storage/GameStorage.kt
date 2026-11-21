package com.example.blockpuzzle.storage

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.blockpuzzle.model.BOARD_SIZE
import com.example.blockpuzzle.model.BlockPiece
import com.example.blockpuzzle.model.GameState

object GameStorage {
    private const val PREFS_NAME = "block_puzzle_preferences"
    private const val KEY_HIGH_SCORE = "high_score"
    private const val KEY_SCORE = "current_score"
    private const val KEY_IS_GAME_OVER = "is_game_over"
    private const val KEY_BOARD_MATRIX = "board_matrix"
    private const val KEY_AVAILABLE_PIECES = "available_pieces"

    fun saveGameState(context: Context, state: GameState) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt(KEY_HIGH_SCORE, state.highScore)
            putInt(KEY_SCORE, state.score)
            putBoolean(KEY_IS_GAME_OVER, state.isGameOver)
            putString(KEY_BOARD_MATRIX, encodeBoard(state.board))
            putString(KEY_AVAILABLE_PIECES, encodePieces(state.availablePieces))
            apply()
        }
    }

    fun loadGameState(context: Context): GameState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val highScore = prefs.getInt(KEY_HIGH_SCORE, 0)

        if (!prefs.contains(KEY_BOARD_MATRIX)) {
            return GameState(highScore = highScore)
        }

        val score = prefs.getInt(KEY_SCORE, 0)
        val isGameOver = prefs.getBoolean(KEY_IS_GAME_OVER, false)
        val boardStr = prefs.getString(KEY_BOARD_MATRIX, "") ?: ""
        val piecesStr = prefs.getString(KEY_AVAILABLE_PIECES, "") ?: ""

        val board = decodeBoard(boardStr)
        val availablePieces = decodePieces(piecesStr)

        return GameState(
            board = board,
            availablePieces = availablePieces,
            score = score,
            highScore = highScore,
            isGameOver = isGameOver
        )
    }

    private fun encodeBoard(board: List<List<Color?>>): String {
        return board.flatten().joinToString(",") { color ->
            color?.toArgb()?.toString() ?: "-1"
        }
    }

    private fun decodeBoard(encoded: String): List<List<Color?>> {
        if (encoded.isBlank()) return List(BOARD_SIZE) { List(BOARD_SIZE) { null } }
        val tokens = encoded.split(",")
        if (tokens.size != BOARD_SIZE * BOARD_SIZE) return List(BOARD_SIZE) { List(BOARD_SIZE) { null } }

        val result = MutableList(BOARD_SIZE) { MutableList<Color?>(BOARD_SIZE) { null } }
        var idx = 0
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                val valInt = tokens[idx++].toIntOrNull() ?: -1
                result[r][c] = if (valInt != -1) Color(valInt) else null
            }
        }
        return result
    }

    private fun encodePieces(pieces: List<BlockPiece?>): String {
        return pieces.joinToString(";") { piece ->
            if (piece == null) "NULL"
            else {
                val colorInt = piece.color.toArgb()
                val rows = piece.rows
                val cols = piece.cols
                val flatShape = piece.shape.flatten().joinToString(",") { if (it) "1" else "0" }
                "$colorInt|$rows|$cols|$flatShape"
            }
        }
    }

    private fun decodePieces(encoded: String): List<BlockPiece?> {
        if (encoded.isBlank()) return listOf(BlockPiece.createRandom(), BlockPiece.createRandom(), BlockPiece.createRandom())
        val pieceTokens = encoded.split(";")
        if (pieceTokens.size != 3) return listOf(BlockPiece.createRandom(), BlockPiece.createRandom(), BlockPiece.createRandom())

        return pieceTokens.map { token ->
            if (token == "NULL" || token.isBlank()) null
            else {
                try {
                    val parts = token.split("|")
                    val colorInt = parts[0].toInt()
                    val rows = parts[1].toInt()
                    val cols = parts[2].toInt()
                    val shapeBools = parts[3].split(",").map { it == "1" }

                    val shape = List(rows) { r ->
                        List(cols) { c ->
                            shapeBools[r * cols + c]
                        }
                    }
                    BlockPiece(shape = shape, color = Color(colorInt))
                } catch (e: Exception) {
                    BlockPiece.createRandom()
                }
            }
        }
    }
}
