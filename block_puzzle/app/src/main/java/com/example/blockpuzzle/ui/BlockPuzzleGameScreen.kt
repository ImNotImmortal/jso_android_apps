package com.example.blockpuzzle.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.blockpuzzle.model.BOARD_SIZE
import com.example.blockpuzzle.model.BlockPiece
import com.example.blockpuzzle.model.GameState
import com.example.blockpuzzle.viewmodel.GameViewModel
import kotlin.math.round

@Composable
fun BlockPuzzleGameScreen(
    gameViewModel: GameViewModel = viewModel()
) {
    val state by gameViewModel.uiState.collectAsState()

    var boardBounds by remember { mutableStateOf<Rect?>(null) }
    var draggingPieceIndex by remember { mutableStateOf<Int?>(null) }
    var dragPositionInWindow by remember { mutableStateOf<Offset?>(null) }

    var currentHoverRow by remember { mutableStateOf<Int?>(null) }
    var currentHoverCol by remember { mutableStateOf<Int?>(null) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        color = Color(0xFF12121E)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Header (Titulok & Skóre)
                HeaderSection(
                    score = state.score,
                    highScore = state.highScore,
                    onRestartClick = { gameViewModel.restartGame() }
                )

                // 2. Hracia plocha 8x8 (s priamym vykresľovaním reálneho tvaru VRCHOLOM nad existujúce bloky)
                BoardSection(
                    gameState = state,
                    hoverRow = currentHoverRow,
                    hoverCol = currentHoverCol,
                    onBoardPositioned = { bounds ->
                        boardBounds = bounds
                    },
                    onCellClick = { row, col ->
                        if (state.selectedPieceIndex != null) {
                            gameViewModel.placeSelectedPieceAt(row, col)
                        }
                    },
                    canPlacePiece = { piece, r, c ->
                        gameViewModel.canPlacePiece(piece, state.board, r, c)
                    }
                )

                // 3. Spodný dok s ponukou blokov
                PieceDockSection(
                    availablePieces = state.availablePieces,
                    selectedIndex = state.selectedPieceIndex,
                    onPieceSelect = { index ->
                        gameViewModel.selectPiece(index)
                    },
                    onDragStart = { index, initialPosition ->
                        gameViewModel.selectPiece(index)
                        draggingPieceIndex = index
                        dragPositionInWindow = initialPosition
                    },
                    onDrag = { mousePosInWindow ->
                        dragPositionInWindow = mousePosInWindow
                        val bounds = boardBounds
                        val activeIndex = draggingPieceIndex
                        val piece = if (activeIndex != null) state.availablePieces.getOrNull(activeIndex) else null

                        if (bounds != null && piece != null && bounds.contains(mousePosInWindow)) {
                            val cellSize = bounds.width / BOARD_SIZE
                            val pieceWidthPx = piece.cols * cellSize
                            val pieceHeightPx = piece.rows * cellSize

                            val pointerOnBoard = mousePosInWindow - bounds.topLeft
                            val pieceTopLeftOnBoard = pointerOnBoard - Offset(pieceWidthPx / 2f, pieceHeightPx / 2f)

                            val col = round(pieceTopLeftOnBoard.x / cellSize).toInt().coerceIn(0, BOARD_SIZE - piece.cols)
                            val row = round(pieceTopLeftOnBoard.y / cellSize).toInt().coerceIn(0, BOARD_SIZE - piece.rows)

                            if (currentHoverRow != row || currentHoverCol != col) {
                                currentHoverRow = row
                                currentHoverCol = col
                                gameViewModel.updateHoverCell(row, col)
                            }
                        } else {
                            if (currentHoverRow != null || currentHoverCol != null) {
                                currentHoverRow = null
                                currentHoverCol = null
                                gameViewModel.updateHoverCell(null, null)
                            }
                        }
                    },
                    onDragEnd = {
                        val row = currentHoverRow
                        val col = currentHoverCol
                        if (row != null && col != null) {
                            gameViewModel.placeSelectedPieceAt(row, col)
                        }
                        draggingPieceIndex = null
                        dragPositionInWindow = null
                        currentHoverRow = null
                        currentHoverCol = null
                        gameViewModel.updateHoverCell(null, null)
                    }
                )
            }

            // Plynulý plávajúci náhľad ťahaného bloku sprevádzajúci kurzor/prst bez prerušenia
            val dragPos = dragPositionInWindow
            val activeIndex = draggingPieceIndex

            if (dragPos != null && activeIndex != null) {
                val piece = state.availablePieces.getOrNull(activeIndex)
                if (piece != null) {
                    val density = LocalDensity.current
                    val dockCellSizeDp = 22.dp
                    val dockCellSizePx = with(density) { dockCellSizeDp.toPx() }

                    val pieceWidthPx = piece.cols * dockCellSizePx
                    val pieceHeightPx = piece.rows * dockCellSizePx

                    // Odsadenie 35dp nahor, aby prst/kurzor neprekrýval tvar
                    val renderX = dragPos.x - pieceWidthPx / 2f
                    val renderY = dragPos.y - pieceHeightPx / 2f - with(density) { 35.dp.toPx() }

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = renderX
                                translationY = renderY
                            }
                    ) {
                        PieceShapeView(
                            piece = piece,
                            cellSize = dockCellSizeDp,
                            cellPadding = 1.5.dp
                        )
                    }
                }
            }

            // 4. Dialóg Game Over
            if (state.isGameOver) {
                GameOverDialog(
                    score = state.score,
                    highScore = state.highScore,
                    onRestart = { gameViewModel.restartGame() }
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(
    score: Int,
    highScore: Int,
    onRestartClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BLOCK PUZZLE",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            IconButton(
                onClick = onRestartClick,
                modifier = Modifier
                    .background(Color(0xFF26263A), shape = RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Nová hra",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ScoreCard(
                title = "SKÓRE",
                value = score.toString(),
                color = Color(0xFF4CAF50)
            )
            ScoreCard(
                title = "BEST",
                value = highScore.toString(),
                color = Color(0xFFFF9800),
                icon = {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun ScoreCard(
    title: String,
    value: String,
    color: Color,
    icon: (@Composable () -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(150.dp)
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon?.invoke()
                if (icon != null) Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun BoardSection(
    gameState: GameState,
    hoverRow: Int?,
    hoverCol: Int?,
    onBoardPositioned: (Rect) -> Unit,
    onCellClick: (row: Int, col: Int) -> Unit,
    canPlacePiece: (piece: BlockPiece, row: Int, col: Int) -> Boolean
) {
    val selectedPiece = gameState.selectedPieceIndex?.let { gameState.availablePieces.getOrNull(it) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1A1A2B))
            .border(2.dp, Color(0xFF2D2D45), RoundedCornerShape(20.dp))
            .onGloballyPositioned { coordinates ->
                onBoardPositioned(coordinates.boundsInWindow())
            }
            .padding(12.dp)
    ) {
        val isValidPlacement = if (selectedPiece != null && hoverRow != null && hoverCol != null) {
            canPlacePiece(selectedPiece, hoverRow, hoverCol)
        } else false

        Column(modifier = Modifier.fillMaxSize()) {
            for (r in 0 until BOARD_SIZE) {
                Row(modifier = Modifier.weight(1f)) {
                    for (c in 0 until BOARD_SIZE) {
                        val cellColor = gameState.board[r][c]

                        val isPreviewCell = if (selectedPiece != null && hoverRow != null && hoverCol != null) {
                            val relR = r - hoverRow
                            val relC = c - hoverCol
                            relR in 0 until selectedPiece.rows &&
                                    relC in 0 until selectedPiece.cols &&
                                    selectedPiece.shape[relR][relC]
                        } else false

                        val previewColor = if (isPreviewCell && selectedPiece != null) selectedPiece.color else null

                        BoardCell(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clickable {
                                    onCellClick(r, c)
                                },
                            color = cellColor,
                            previewColor = previewColor,
                            isValidPreview = isValidPlacement
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardCell(
    modifier: Modifier,
    color: Color?,
    previewColor: Color?,
    isValidPreview: Boolean
) {
    // Náhľad ťahaného tvaru (previewColor) má VŽDY prednosť pred existujúcimi blokmi (color), aby sa tvar neorezával
    val backgroundColor = when {
        previewColor != null && isValidPreview -> previewColor.copy(alpha = 0.9f)
        previewColor != null && !isValidPreview -> previewColor.copy(alpha = 0.45f)
        color != null -> color
        else -> Color(0xFF242438)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .border(
                width = if (previewColor != null) 2.dp else 1.dp,
                color = when {
                    previewColor != null && isValidPreview -> previewColor
                    previewColor != null && !isValidPreview -> Color(0xFFFF5252)
                    color != null -> color.copy(alpha = 0.8f)
                    else -> Color(0xFF2E2E48)
                },
                shape = RoundedCornerShape(6.dp)
            )
    )
}

@Composable
private fun PieceDockSection(
    availablePieces: List<BlockPiece?>,
    selectedIndex: Int?,
    onPieceSelect: (Int) -> Unit,
    onDragStart: (index: Int, initialPosition: Offset) -> Unit,
    onDrag: (position: Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            availablePieces.forEachIndexed { index, piece ->
                val isSelected = selectedIndex == index

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (piece != null) {
                        PiecePreviewCard(
                            index = index,
                            piece = piece,
                            isSelected = isSelected,
                            onClick = { onPieceSelect(index) },
                            onDragStart = onDragStart,
                            onDrag = onDrag,
                            onDragEnd = onDragEnd
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF151522))
                                .border(1.dp, Color(0xFF2A2A3D), RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PiecePreviewCard(
    index: Int,
    piece: BlockPiece,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDragStart: (index: Int, initialPosition: Offset) -> Unit,
    onDrag: (position: Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        label = "pieceScale"
    )

    var cardPositionInWindow by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF2D2D4A) else Color(0xFF161625))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) piece.color else Color(0xFF2E2E48),
                shape = RoundedCornerShape(12.dp)
            )
            .onGloballyPositioned { coordinates ->
                cardPositionInWindow = coordinates.boundsInWindow().topLeft
            }
            .pointerInput(index) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        onDragStart(index, cardPositionInWindow + startOffset)
                    },
                    onDragEnd = {
                        onDragEnd()
                    },
                    onDragCancel = {
                        onDragEnd()
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        onDrag(cardPositionInWindow + change.position)
                    }
                )
            }
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        PieceShapeView(piece = piece)
    }
}

@Composable
private fun PieceShapeView(
    piece: BlockPiece,
    cellSize: Dp = 16.dp,
    cellPadding: Dp = 1.5.dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        for (r in 0 until piece.rows) {
            Row {
                for (c in 0 until piece.cols) {
                    val isFilled = piece.shape[r][c]
                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .padding(cellPadding)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isFilled) piece.color else Color.Transparent
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun GameOverDialog(
    score: Int,
    highScore: Int,
    onRestart: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = Color(0xFF1E1E2E),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "KONIEC HRY!",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF5252)
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Niektorý z blokov v ponuke sa už nedá umiestniť na hraciu plochu.",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF12121E)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Dosiahnuté skóre",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Text(
                            text = score.toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Najlepšie skóre: $highScore",
                                color = Color.LightGray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "HRAŤ ZNOVA",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    )
}
