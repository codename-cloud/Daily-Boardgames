package com.example.sudoku

import kotlin.random.Random

object SudokuGenerator {

    fun generatePuzzle(
        difficulty: SudokuDifficulty,
        seed: Long? = null
    ): List<List<SudokuCell>> {
        val rng = if (seed != null) Random(seed) else Random.Default
        val solutionGrid = Array(9) { IntArray(9) }

        // Fill diagonal 3x3 boxes first (they are mutually independent)
        for (box in 0 until 9 step 3) {
            val digits = (1..9).shuffled(rng)
            var idx = 0
            for (r in box until box + 3) {
                for (c in box until box + 3) {
                    solutionGrid[r][c] = digits[idx++]
                }
            }
        }

        solveRandom(solutionGrid, rng)

        // Carve clues while preserving strict uniqueness (exactly 1 solution)
        val puzzleGrid = Array(9) { r -> solutionGrid[r].clone() }
        val positions = (0 until 81).shuffled(rng)
        var cluesRemaining = 81

        for (pos in positions) {
            if (cluesRemaining <= difficulty.cluesToKeep) break
            val r = pos / 9
            val c = pos % 9
            val backup = puzzleGrid[r][c]
            puzzleGrid[r][c] = 0

            if (countSolutions(puzzleGrid, limit = 2) == 1) {
                cluesRemaining--
            } else {
                puzzleGrid[r][c] = backup
            }
        }

        return List(9) { r ->
            List(9) { c ->
                val valAtCell = puzzleGrid[r][c]
                SudokuCell(
                    row = r,
                    col = c,
                    value = valAtCell,
                    solution = solutionGrid[r][c],
                    isGiven = valAtCell != 0
                )
            }
        }
    }

    private fun solveRandom(grid: Array<IntArray>, rng: Random): Boolean {
        val (row, col, mask) = findBestEmptyCell(grid) ?: return true
        if (mask == 0) return false

        val candidates = mutableListOf<Int>()
        for (d in 1..9) {
            if ((mask and (1 shl d)) != 0) candidates.add(d)
        }
        candidates.shuffle(rng)

        for (d in candidates) {
            grid[row][col] = d
            if (solveRandom(grid, rng)) return true
            grid[row][col] = 0
        }
        return false
    }

    fun countSolutions(grid: Array<IntArray>, limit: Int = 2): Int {
        val working = Array(9) { r -> grid[r].clone() }
        return countSolutionsInternal(working, limit)
    }

    private fun countSolutionsInternal(grid: Array<IntArray>, limit: Int): Int {
        val best = findBestEmptyCell(grid) ?: return 1
        val (row, col, mask) = best
        if (mask == 0) return 0

        var count = 0
        for (d in 1..9) {
            if ((mask and (1 shl d)) != 0) {
                grid[row][col] = d
                count += countSolutionsInternal(grid, limit - count)
                grid[row][col] = 0
                if (count >= limit) return count
            }
        }
        return count
    }

    private fun findBestEmptyCell(grid: Array<IntArray>): Triple<Int, Int, Int>? {
        var bestR = -1
        var bestC = -1
        var bestMask = 0
        var minCount = 10

        val rowMasks = IntArray(9)
        val colMasks = IntArray(9)
        val boxMasks = IntArray(9)

        for (r in 0 until 9) {
            for (c in 0 until 9) {
                val v = grid[r][c]
                if (v != 0) {
                    val bit = 1 shl v
                    rowMasks[r] = rowMasks[r] or bit
                    colMasks[c] = colMasks[c] or bit
                    boxMasks[(r / 3) * 3 + (c / 3)] = boxMasks[(r / 3) * 3 + (c / 3)] or bit
                }
            }
        }

        val allDigitsMask = 0b1111111110 // bits 1..9
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                if (grid[r][c] == 0) {
                    val used = rowMasks[r] or colMasks[c] or boxMasks[(r / 3) * 3 + (c / 3)]
                    val avail = allDigitsMask and used.inv()
                    val bitCount = Integer.bitCount(avail)
                    if (bitCount < minCount) {
                        minCount = bitCount
                        bestR = r
                        bestC = c
                        bestMask = avail
                        if (minCount == 0 || minCount == 1) {
                            return Triple(bestR, bestC, bestMask)
                        }
                    }
                }
            }
        }

        return if (bestR == -1) null else Triple(bestR, bestC, bestMask)
    }

    fun hasConflict(board: List<List<SudokuCell>>, row: Int, col: Int, value: Int): Boolean {
        if (value == 0) return false
        for (i in 0 until 9) {
            if (i != col && board[row][i].value == value) return true
            if (i != row && board[i][col].value == value) return true
        }
        val boxR = (row / 3) * 3
        val boxC = (col / 3) * 3
        for (r in boxR until boxR + 3) {
            for (c in boxC until boxC + 3) {
                if ((r != row || c != col) && board[r][c].value == value) return true
            }
        }
        return false
    }
}
