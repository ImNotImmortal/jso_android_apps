package com.example.blockpuzzle.viewmodel

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import com.example.blockpuzzle.model.BOARD_SIZE
import com.example.blockpuzzle.model.BlockPiece
import com.example.blockpuzzle.model.GameState
import com.example.blockpuzzle.storage.GameStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(GameStorage.loadGameState(getApplication()))
    val uiState: StateFlow<GameState> = _uiState.asStateFlow()

    private fun saveCurrentState(state: GameState) {
        GameStorage.saveGameState(getApplication(), state)
    }

    fun selectPiece(index: Int?) {
        _uiState.update { state ->
            if (state.selectedPieceIndex == index) {
                state.copy(selectedPieceIndex = null, hoverRow = null, hoverCol = null)
            } else {
                state.copy(selectedPieceIndex = index, hoverRow = null, hoverCol = null)
            }
        }
    }

    fun forceSelectPiece(index: Int?) {
        _uiState.update { state ->
            state.copy(selectedPieceIndex = index, hoverRow = null, hoverCol = null)
        }
    }

    fun updateHoverCell(row: Int?, col: Int?) {
        _uiState.update { state ->
            if (state.hoverRow != row || state.hoverCol != col) {
                state.copy(hoverRow = row, hoverCol = col)
            } else {
                state
            }
        }
    }

    fun canPlacePiece(
        piece: BlockPiece,
        board: List<List<Color?>>,
        startRow: Int,
        startCol: Int
    ): Boolean {
        if (startRow < 0 || startCol < 0) return false
        if (startRow + piece.rows > BOARD_SIZE || startCol + piece.cols > BOARD_SIZE) return false

        for (r in 0 until piece.rows) {
            for (c in 0 until piece.cols) {
                if (piece.shape[r][c]) {
                    if (board[startRow + r][startCol + c] != null) {
                        return false
                    }
                }
            }
        }
        return true
    }

    private fun canPieceFitSomewhere(piece: BlockPiece, board: List<List<Color?>>): Boolean {
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                if (canPlacePiece(piece, board, r, c)) {
                    return true
                }
            }
        }
        return false
    }

    private fun generateValidPiece(board: List<List<Color?>>): BlockPiece? {
        val validShapes = BlockPiece.Shapes.filter { shape ->
            val dummyPiece = BlockPiece(shape = shape, color = Color.Unspecified)
            canPieceFitSomewhere(dummyPiece, board)
        }

        if (validShapes.isEmpty()) return null

        val chosenShape = validShapes.random()
        val randomColor = listOf(
            Color(0xFFFF5252), Color(0xFFFF9800), Color(0xFFFFEB3B),
            Color(0xFF4CAF50), Color(0xFF00BCD4), Color(0xFF2196F3),
            Color(0xFF9C27B0), Color(0xFFE91E63)
        ).random()

        return BlockPiece(shape = chosenShape, color = randomColor)
    }

    private fun generate3ValidPieces(board: List<List<Color?>>): List<BlockPiece>? {
        val pieces = mutableListOf<BlockPiece>()
        repeat(3) {
            val piece = generateValidPiece(board) ?: return null
            pieces.add(piece)
        }
        return pieces
    }

    fun placeSelectedPieceAt(startRow: Int, startCol: Int) {
        val currentState = _uiState.value
        val pieceIndex = currentState.selectedPieceIndex ?: return
        val piece = currentState.availablePieces.getOrNull(pieceIndex) ?: return

        if (currentState.isGameOver) return

        if (!canPlacePiece(piece, currentState.board, startRow, startCol)) return

        // 1. Umiestniť bloky na plochu
        val newBoard = currentState.board.map { it.toMutableList() }.toMutableList()
        var placedBlocksCount = 0

        for (r in 0 until piece.rows) {
            for (c in 0 until piece.cols) {
                if (piece.shape[r][c]) {
                    newBoard[startRow + r][startCol + c] = piece.color
                    placedBlocksCount++
                }
            }
        }

        // 2. Skontrolovať zaplnené riadky a stĺpce
        val fullRows = mutableListOf<Int>()
        val fullCols = mutableListOf<Int>()

        for (r in 0 until BOARD_SIZE) {
            if (newBoard[r].all { it != null }) {
                fullRows.add(r)
            }
        }

        for (c in 0 until BOARD_SIZE) {
            var isFull = true
            for (r in 0 until BOARD_SIZE) {
                if (newBoard[r][c] == null) {
                    isFull = false
                    break
                }
            }
            if (isFull) {
                fullCols.add(c)
            }
        }

        // 3. Vymazať zaplnené riadky a stĺpce
        for (r in fullRows) {
            for (c in 0 until BOARD_SIZE) {
                newBoard[r][c] = null
            }
        }
        for (c in fullCols) {
            for (r in 0 until BOARD_SIZE) {
                newBoard[r][c] = null
            }
        }

        // 4. Výpočet skóre
        val totalLinesCleared = fullRows.size + fullCols.size
        val addedScore = when (totalLinesCleared) {
            0 -> 0
            1 -> 100
            2 -> 300
            3 -> 600
            else -> totalLinesCleared * 250
        }
        val newScore = currentState.score + addedScore
        val newHighScore = maxOf(newScore, currentState.highScore)

        // 5. Aktualizácia zoznamu dostupných kusov
        val newAvailablePieces = currentState.availablePieces.toMutableList()
        newAvailablePieces[pieceIndex] = null

        var isGameOver = false

        // Ak sú všetky 3 kusy použité, vygenerovať nové 3 KUSY, KTORÉ SA ZARUČENE ZMESTIA
        if (newAvailablePieces.all { it == null }) {
            val freshPieces = generate3ValidPieces(newBoard)
            if (freshPieces == null) {
                isGameOver = true
            } else {
                newAvailablePieces[0] = freshPieces[0]
                newAvailablePieces[1] = freshPieces[1]
                newAvailablePieces[2] = freshPieces[2]
            }
        } else {
            // Skontrolovať, či hráči svojim ťahom nezablokovali zostávajúce kusy
            isGameOver = checkGameOver(newBoard, newAvailablePieces)
        }

        val newState = currentState.copy(
            board = newBoard,
            availablePieces = newAvailablePieces,
            score = newScore,
            highScore = newHighScore,
            selectedPieceIndex = null,
            hoverRow = null,
            hoverCol = null,
            isGameOver = isGameOver
        )

        _uiState.value = newState
        saveCurrentState(newState)
    }

    private fun checkGameOver(
        board: List<List<Color?>>,
        availablePieces: List<BlockPiece?>
    ): Boolean {
        val nonNullPieces = availablePieces.filterNotNull()
        if (nonNullPieces.isEmpty()) return false

        for (piece in nonNullPieces) {
            if (!canPieceFitSomewhere(piece, board)) {
                return true
            }
        }
        return false
    }

    fun restartGame() {
        val currentHighScore = _uiState.value.highScore
        val newBoard = List(BOARD_SIZE) { List(BOARD_SIZE) { null } }
        val freshPieces = generate3ValidPieces(newBoard) ?: listOf(
            BlockPiece.createRandom(),
            BlockPiece.createRandom(),
            BlockPiece.createRandom()
        )

        val newState = GameState(
            board = newBoard,
            availablePieces = freshPieces,
            highScore = currentHighScore
        )
        _uiState.value = newState
        saveCurrentState(newState)
    }
}
