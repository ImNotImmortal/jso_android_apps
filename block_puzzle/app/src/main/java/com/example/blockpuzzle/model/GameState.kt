package com.example.blockpuzzle.model

import androidx.compose.ui.graphics.Color

const val BOARD_SIZE = 8

data class GameState(
    val board: List<List<Color?>> = List(BOARD_SIZE) { List(BOARD_SIZE) { null } },
    val availablePieces: List<BlockPiece?> = listOf(
        BlockPiece.createRandom(),
        BlockPiece.createRandom(),
        BlockPiece.createRandom()
    ),
    val score: Int = 0,
    val highScore: Int = 0,
    val isGameOver: Boolean = false,
    val selectedPieceIndex: Int? = null,
    val hoverRow: Int? = null,
    val hoverCol: Int? = null
)
