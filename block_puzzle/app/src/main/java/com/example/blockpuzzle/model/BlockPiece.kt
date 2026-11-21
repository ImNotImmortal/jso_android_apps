package com.example.blockpuzzle.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

data class BlockPiece(
    val id: String = UUID.randomUUID().toString(),
    val shape: List<List<Boolean>>,
    val color: Color
) {
    val rows: Int get() = shape.size
    val cols: Int get() = if (shape.isNotEmpty()) shape[0].size else 0

    companion object {
        private val PieceColors = listOf(
            Color(0xFFFF5252), // Červená
            Color(0xFFFF9800), // Oranžová
            Color(0xFFFFEB3B), // Žltá
            Color(0xFF4CAF50), // Zelená
            Color(0xFF00BCD4), // Tyrkysová
            Color(0xFF2196F3), // Modrá
            Color(0xFF9C27B0), // Fialová
            Color(0xFFE91E63)  // Ružová
        )

        val Shapes = listOf(
            // 1x1
            listOf(listOf(true)),

            // 1x2, 2x1
            listOf(listOf(true, true)),
            listOf(listOf(true), listOf(true)),

            // 1x3, 3x1
            listOf(listOf(true, true, true)),
            listOf(listOf(true), listOf(true), listOf(true)),

            // 1x4, 4x1
            listOf(listOf(true, true, true, true)),
            listOf(listOf(true), listOf(true), listOf(true), listOf(true)),

            // 1x5, 5x1
            listOf(listOf(true, true, true, true, true)),
            listOf(listOf(true), listOf(true), listOf(true), listOf(true), listOf(true)),

            // 2x2 štvorec
            listOf(
                listOf(true, true),
                listOf(true, true)
            ),

            // 3x3 štvorec
            listOf(
                listOf(true, true, true),
                listOf(true, true, true),
                listOf(true, true, true)
            ),

            // Rohy 2x2
            listOf(
                listOf(true, true),
                listOf(true, false)
            ),
            listOf(
                listOf(true, true),
                listOf(false, true)
            ),
            listOf(
                listOf(true, false),
                listOf(true, true)
            ),
            listOf(
                listOf(false, true),
                listOf(true, true)
            ),

            // Velké rohy 3x3 (5 kociek)
            listOf(
                listOf(true, true, true),
                listOf(true, false, false),
                listOf(true, false, false)
            ),
            listOf(
                listOf(true, true, true),
                listOf(false, false, true),
                listOf(false, false, true)
            ),
            listOf(
                listOf(true, false, false),
                listOf(true, false, false),
                listOf(true, true, true)
            ),
            listOf(
                listOf(false, false, true),
                listOf(false, false, true),
                listOf(true, true, true)
            ),

            // L-tvary (3x2)
            listOf(
                listOf(true, false),
                listOf(true, false),
                listOf(true, true)
            ),
            listOf(
                listOf(false, true),
                listOf(false, true),
                listOf(true, true)
            ),
            listOf(
                listOf(true, true, true),
                listOf(true, false, false)
            ),
            listOf(
                listOf(true, true, true),
                listOf(false, false, true)
            ),

            // T-tvary
            listOf(
                listOf(true, true, true),
                listOf(false, true, false)
            ),
            listOf(
                listOf(false, true, false),
                listOf(true, true, true)
            ),
            listOf(
                listOf(true, false),
                listOf(true, true),
                listOf(true, false)
            ),

            // Z / S-tvary
            listOf(
                listOf(true, true, false),
                listOf(false, true, true)
            ),
            listOf(
                listOf(false, true, true),
                listOf(true, true, false)
            )
        )

        fun createRandom(): BlockPiece {
            val randomShape = Shapes.random()
            val randomColor = PieceColors.random()
            return BlockPiece(shape = randomShape, color = randomColor)
        }
    }
}
