package com.example.minesweeper.model

enum class DifficultyType {
    BEGINNER,
    INTERMEDIATE,
    EXPERT,
    CUSTOM
}

data class Difficulty(
    val type: DifficultyType,
    val name: String,
    val rows: Int,
    val cols: Int,
    val mines: Int,
    val description: String
) {
    companion object {
        val BEGINNER_PORTRAIT = Difficulty(
            type = DifficultyType.BEGINNER,
            name = "Beginner",
            rows = 16,
            cols = 9,
            mines = 18,
            description = "9x16 • 18 Mines"
        )

        val BEGINNER_LANDSCAPE = Difficulty(
            type = DifficultyType.BEGINNER,
            name = "Beginner",
            rows = 9,
            cols = 16,
            mines = 18,
            description = "16x9 • 18 Mines"
        )

        val BEGINNER = BEGINNER_PORTRAIT

        val INTERMEDIATE_PORTRAIT = Difficulty(
            type = DifficultyType.INTERMEDIATE,
            name = "Intermediate",
            rows = 21,
            cols = 12,
            mines = 40,
            description = "12x21 • 40 Mines"
        )

        val INTERMEDIATE_LANDSCAPE = Difficulty(
            type = DifficultyType.INTERMEDIATE,
            name = "Intermediate",
            rows = 12,
            cols = 21,
            mines = 40,
            description = "21x12 • 40 Mines"
        )

        val INTERMEDIATE = INTERMEDIATE_PORTRAIT

        val EXPERT_PORTRAIT = Difficulty(
            type = DifficultyType.EXPERT,
            name = "Expert",
            rows = 30,
            cols = 16,
            mines = 99,
            description = "16x30 • 99 Mines"
        )

        val EXPERT_LANDSCAPE = Difficulty(
            type = DifficultyType.EXPERT,
            name = "Expert",
            rows = 16,
            cols = 30,
            mines = 99,
            description = "30x16 • 99 Mines"
        )

        val PRESETS = listOf(BEGINNER, INTERMEDIATE, EXPERT_PORTRAIT)
    }
}
