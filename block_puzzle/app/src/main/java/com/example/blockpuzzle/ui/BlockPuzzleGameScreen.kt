package com.example.blockpuzzle.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.blockpuzzle.model.BOARD_SIZE
import com.example.blockpuzzle.model.BlockPiece
import com.example.blockpuzzle.model.GameState
import com.example.blockpuzzle.viewmodel.GameViewModel
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.hypot

@Composable
fun BlockPuzzleGameScreen(
    gameViewModel: GameViewModel = viewModel()
) {
    val state by gameViewModel.uiState.collectAsState()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    var boardBounds by remember { mutableStateOf<Rect?>(null) }
    var dockBounds by remember { mutableStateOf<Rect?>(null) }
    var draggingPieceIndex by remember { mutableStateOf<Int?>(null) }
    var dragPositionInWindow by remember { mutableStateOf<Offset?>(null) }
    var initialDragPosition by remember { mutableStateOf<Offset?>(null) }
    var touchRatio by remember { mutableStateOf(0.5f) }

    var currentHoverRow by remember { mutableStateOf<Int?>(null) }
    var currentHoverCol by remember { mutableStateOf<Int?>(null) }

    val boardPaddingPx = with(density) { 12.dp.toPx() }

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

                // 2. Hracia plocha 8x8
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
                    draggingIndex = draggingPieceIndex,
                    onDockPositioned = { bounds ->
                        dockBounds = bounds
                    },
                    onDragStart = { index, initialPosition, touchOffset ->
                        gameViewModel.forceSelectPiece(index)
                        draggingPieceIndex = index
                        dragPositionInWindow = initialPosition
                        initialDragPosition = initialPosition
                        val cardHeightPx = with(density) { 70.dp.toPx() }
                        touchRatio = (touchOffset.y / cardHeightPx).coerceIn(0f, 1f)
                    },
                    onDrag = { mousePosInWindow ->
                        dragPositionInWindow = mousePosInWindow
                        val bounds = boardBounds
                        val activeIndex = draggingPieceIndex
                        val piece = if (activeIndex != null) state.availablePieces.getOrNull(activeIndex) else null
                        val dockB = dockBounds

                        if (bounds != null && piece != null && activeIndex != null) {
                            val boardLeft = bounds.left + boardPaddingPx
                            val boardTop = bounds.top + boardPaddingPx
                            val gridWidth = bounds.width - (2 * boardPaddingPx)
                            val cellSizePx = gridWidth / BOARD_SIZE

                            val pieceWidthPx = piece.cols * cellSizePx
                            val pieceHeightPx = piece.rows * cellSizePx

                            val startPos = initialDragPosition ?: mousePosInWindow
                            val fingerX = mousePosInWindow.x
                            val fingerY = mousePosInWindow.y

                            val deltaX = fingerX - startPos.x
                            val deltaY = fingerY - startPos.y
                            val dragDistance = hypot(deltaX, deltaY)

                            // Neviditeľná línia presne medzi spodkom hracej plochy a horným okrajom doku (posunutá vyššie)
                            val baselineY = if (dockB != null) {
                                bounds.bottom + (dockB.top - bounds.bottom) * 0.35f
                            } else {
                                bounds.bottom + with(density) { 20.dp.toPx() }
                            }

                            val slotWidth = if (dockB != null) dockB.width / 3f else bounds.width / 3f
                            val dockLeft = dockB?.left ?: bounds.left
                            val slotCenterX = dockLeft + slotWidth * activeIndex + slotWidth / 2f

                            val accelFactor = 1f + (dragDistance / 350f).coerceIn(0f, 0.35f)
                            val renderX = (slotCenterX + deltaX * accelFactor) - pieceWidthPx / 2f
                            val renderY = (baselineY + deltaY * accelFactor) - pieceHeightPx

                            val pieceCenterX = renderX + pieceWidthPx / 2f

                            val maxCol = BOARD_SIZE - piece.cols
                            val maxRow = BOARD_SIZE - piece.rows

                            val rawCol = round((pieceCenterX - boardLeft) / cellSizePx - piece.cols / 2f).toInt()
                            val rawRow = if (deltaY < 0) {
                                // Smer nahor: posunuté presne o 1.5 políčka nadol
                                floor((renderY - boardTop + cellSizePx * 1.5f) / cellSizePx).toInt()
                            } else {
                                // Smer nadol: posunuté presne o 1.5 políčka nadol
                                floor((renderY + pieceHeightPx - boardTop + cellSizePx * 1.5f) / cellSizePx).toInt() - piece.rows
                            }

                            val isInsideGrid = rawCol in 0..maxCol && rawRow in 0..maxRow

                            if (isInsideGrid && gameViewModel.canPlacePiece(piece, state.board, rawRow, rawCol)) {
                                if (currentHoverRow != rawRow || currentHoverCol != rawCol) {
                                    currentHoverRow = rawRow
                                    currentHoverCol = rawCol
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            } else {
                                if (currentHoverRow != null || currentHoverCol != null) {
                                    currentHoverRow = null
                                    currentHoverCol = null
                                }
                            }
                        } else {
                            if (currentHoverRow != null || currentHoverCol != null) {
                                currentHoverRow = null
                                currentHoverCol = null
                            }
                        }
                    },
                    onDragEnd = {
                        val row = currentHoverRow
                        val col = currentHoverCol
                        if (row != null && col != null) {
                            gameViewModel.placeSelectedPieceAt(row, col)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        } else {
                            // Pokiaľ nebol uložený do hracej plochy, pri pustení prsta sa objekt vráti späť do ponuky
                            gameViewModel.selectPiece(null)
                        }
                        draggingPieceIndex = null
                        dragPositionInWindow = null
                        initialDragPosition = null
                        currentHoverRow = null
                        currentHoverCol = null
                        gameViewModel.updateHoverCell(null, null)
                    }
                )
            }

            // Plynulý plávajúci náhľad ťahaného bloku (zobrazí sa LEN pri stlačení a držaní prstom)
            val activeIndex = draggingPieceIndex

            if (activeIndex != null) {
                val piece = state.availablePieces.getOrNull(activeIndex)
                val bounds = boardBounds
                val dockB = dockBounds

                if (piece != null && bounds != null) {
                    val gridWidth = bounds.width - (2 * boardPaddingPx)
                    val cellSizePx = gridWidth / BOARD_SIZE
                    val cellSizeDp = with(density) { cellSizePx.toDp() }

                    val pieceWidthPx = piece.cols * cellSizePx
                    val pieceHeightPx = piece.rows * cellSizePx

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                val pos = dragPositionInWindow

                                if (pos != null) {
                                    val fingerX = pos.x
                                    val fingerY = pos.y
                                    val startPos = initialDragPosition ?: pos

                                    val deltaX = fingerX - startPos.x
                                    val deltaY = fingerY - startPos.y
                                    val dragDistance = hypot(deltaX, deltaY)

                                    val baselineY = if (dockB != null) {
                                        bounds.bottom + (dockB.top - bounds.bottom) * 0.35f
                                    } else {
                                        bounds.bottom + with(density) { 20.dp.toPx() }
                                    }

                                    val slotWidth = if (dockB != null) dockB.width / 3f else bounds.width / 3f
                                    val dockLeft = dockB?.left ?: bounds.left
                                    val slotCenterX = dockLeft + slotWidth * activeIndex + slotWidth / 2f

                                    val accelFactor = 1f + (dragDistance / 350f).coerceIn(0f, 0.35f)
                                    val renderX = (slotCenterX + deltaX * accelFactor) - pieceWidthPx / 2f
                                    val renderY = (baselineY + deltaY * accelFactor) - pieceHeightPx

                                    translationX = renderX
                                    translationY = renderY
                                } else {
                                    alpha = 0f
                                }

                                shadowElevation = 0f
                                shape = RoundedCornerShape(8.dp)
                                clip = false
                            }
                    ) {
                        PieceShapeView(
                            piece = piece,
                            cellSize = cellSizeDp,
                            cellPadding = 2.dp,
                            cornerRadius = 6.dp
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

private fun getClearingLines(
    board: List<List<Color?>>,
    piece: BlockPiece,
    startRow: Int,
    startCol: Int
): Pair<Set<Int>, Set<Int>> {
    val simBoard = board.map { it.toMutableList() }
    for (r in 0 until piece.rows) {
        for (c in 0 until piece.cols) {
            if (piece.shape[r][c]) {
                val br = startRow + r
                val bc = startCol + c
                if (br in 0 until BOARD_SIZE && bc in 0 until BOARD_SIZE) {
                    simBoard[br][bc] = piece.color
                }
            }
        }
    }

    val fullRows = mutableSetOf<Int>()
    for (r in 0 until BOARD_SIZE) {
        if (simBoard[r].all { it != null }) {
            fullRows.add(r)
        }
    }

    val fullCols = mutableSetOf<Int>()
    for (c in 0 until BOARD_SIZE) {
        if ((0 until BOARD_SIZE).all { r -> simBoard[r][c] != null }) {
            fullCols.add(c)
        }
    }

    return Pair(fullRows, fullCols)
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

        val (clearingRows, clearingCols) = if (selectedPiece != null && hoverRow != null && hoverCol != null && isValidPlacement) {
            getClearingLines(gameState.board, selectedPiece, hoverRow, hoverCol)
        } else Pair(emptySet(), emptySet())

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
                        val isClearingLineCell = r in clearingRows || c in clearingCols

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
                            isValidPreview = isValidPlacement,
                            isClearingLine = isClearingLineCell
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
    isValidPreview: Boolean,
    isClearingLine: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "clearingGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val backgroundColor = when {
        isClearingLine -> Color(0xFFFFD54F).copy(alpha = glowAlpha)
        previewColor != null && isValidPreview -> previewColor.copy(alpha = 0.55f)
        previewColor != null && !isValidPreview -> Color(0xFFEF5350).copy(alpha = 0.35f)
        color != null -> color
        else -> Color(0xFF242438)
    }

    val borderColor = when {
        isClearingLine -> Color(0xFFFFD700)
        previewColor != null && isValidPreview -> previewColor.copy(alpha = 0.9f)
        previewColor != null && !isValidPreview -> Color(0xFFFF5252).copy(alpha = 0.8f)
        color != null -> color.copy(alpha = 0.8f)
        else -> Color(0xFF2E2E48)
    }

    val borderWidth = when {
        isClearingLine -> 2.5.dp
        previewColor != null -> 2.dp
        else -> 1.dp
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(6.dp)
            )
    )
}

@Composable
private fun PieceDockSection(
    availablePieces: List<BlockPiece?>,
    selectedIndex: Int?,
    draggingIndex: Int?,
    onDockPositioned: (Rect) -> Unit,
    onDragStart: (index: Int, initialPosition: Offset, touchOffset: Offset) -> Unit,
    onDrag: (position: Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .onGloballyPositioned { coordinates ->
                onDockPositioned(coordinates.boundsInWindow())
            },
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
                val isBeingDragged = draggingIndex == index

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
                            isBeingDragged = isBeingDragged,
                            onDragStart = onDragStart,
                            onDrag = onDrag,
                            onDragEnd = onDragEnd
                        )
                    } else {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(70.dp)
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
    isBeingDragged: Boolean,
    onDragStart: (index: Int, initialPosition: Offset, touchOffset: Offset) -> Unit,
    onDrag: (position: Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = when {
            isBeingDragged || isSelected -> 0.95f
            else -> 1.0f
        },
        label = "pieceScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isBeingDragged || isSelected) 0f else 1.0f,
        label = "pieceAlpha"
    )

    var cardPositionInWindow by remember { mutableStateOf(Offset.Zero) }
    var currentDragPos by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .scale(scale)
            .graphicsLayer { this.alpha = alpha }
            .onGloballyPositioned { coordinates ->
                cardPositionInWindow = coordinates.boundsInWindow().topLeft
            }
            .pointerInput(index) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val startOffset = down.position
                        val startPos = cardPositionInWindow + startOffset
                        currentDragPos = startPos
                        onDragStart(index, startPos, startOffset)

                        var upOrCancel = false
                        while (!upOrCancel) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) {
                                upOrCancel = true
                                onDragEnd()
                            } else {
                                val dragAmount = change.position - change.previousPosition
                                if (dragAmount != Offset.Zero) {
                                    change.consume()
                                    currentDragPos += dragAmount
                                    onDrag(currentDragPos)
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        PieceShapeView(piece = piece)
    }
}

@Composable
private fun PieceShapeView(
    piece: BlockPiece,
    cellSize: Dp = 16.dp,
    cellPadding: Dp = 1.5.dp,
    cornerRadius: Dp = 3.dp
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
                            .clip(RoundedCornerShape(cornerRadius))
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
