package com.example.minesweeper.model

data class Cell(
    val row: Int,
    val col: Int,
    val isMine: Boolean = false,
    val isRevealed: Boolean = false,
    val isFlagged: Boolean = false,
    val adjacentMines: Int = 0,
    val isExploded: Boolean = false,
    val isFalseFlag: Boolean = false
) {
    val canInteract: Boolean
        get() = !isRevealed
}
