package com.example.blockpuzzle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.blockpuzzle.ui.BlockPuzzleGameScreen
import com.example.blockpuzzle.ui.theme.BlockPuzzleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlockPuzzleTheme {
                BlockPuzzleGameScreen()
            }
        }
    }
}
