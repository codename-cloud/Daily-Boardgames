package com.example.sudoku

enum class SudokuDifficulty(val label: String, val shortLabel: String, val cluesToKeep: Int) {
    EASY("Easy", "Easy", 40),
    MEDIUM("Medium", "Med", 32),
    HARD("Hard", "Hard", 26),
    EXPERT("Expert", "Exp", 22)
}

data class SudokuCell(
    val row: Int,
    val col: Int,
    val value: Int = 0,
    val solution: Int = 0,
    val isGiven: Boolean = false,
    val notes: Set<Int> = emptySet(),
    val isError: Boolean = false
)

data class SudokuStats(
    val played: Int = 0,
    val won: Int = 0,
    val bestTimeSeconds: Int? = null,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0
) {
    val winPercentage: Int
        get() = if (played > 0) (won * 100) / played else 0
}
