package com.example.minesweeper.logic

import com.example.minesweeper.model.Cell
import com.example.minesweeper.model.Difficulty
import kotlin.random.Random

/**
 * Validates whether a Minesweeper board can be 100% solved purely through logic
 * (no guessing required) starting from the initial opened cell.
 */
class GuessFreeSolver(
    private val rows: Int,
    private val cols: Int,
    private val totalMines: Int,
    private val mineGrid: Array<BooleanArray>
) {
    // 0 = UNREVEALED, 1 = REVEALED, 2 = FLAGGED_MINE
    private val state = Array(rows) { IntArray(cols) }
    private val adjacentMines = Array(rows) { IntArray(cols) }

    init {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (!mineGrid[r][c]) {
                    var count = 0
                    for (dr in -1..1) {
                        for (dc in -1..1) {
                            if (dr == 0 && dc == 0) continue
                            val nr = r + dr
                            val nc = c + dc
                            if (nr in 0 until rows && nc in 0 until cols && mineGrid[nr][nc]) {
                                count++
                            }
                        }
                    }
                    adjacentMines[r][c] = count
                }
            }
        }
    }

    /**
     * Checks if all safe cells can be logically deduced and revealed.
     */
    fun isSolvable(startRow: Int, startCol: Int): Boolean {
        if (mineGrid[startRow][startCol]) return false

        // First click opening
        revealCell(startRow, startCol)

        val totalSafeCells = (rows * cols) - totalMines
        var revealedSafeCount = countRevealed()

        var progress = true
        while (progress && revealedSafeCount < totalSafeCells) {
            progress = false

            // Step 1: Single-cell direct deductions
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    if (state[r][c] != 1) continue
                    val targetMines = adjacentMines[r][c]
                    if (targetMines == 0) continue

                    val unrevealed = mutableListOf<Pair<Int, Int>>()
                    var flagged = 0

                    for (dr in -1..1) {
                        for (dc in -1..1) {
                            if (dr == 0 && dc == 0) continue
                            val nr = r + dr
                            val nc = c + dc
                            if (nr in 0 until rows && nc in 0 until cols) {
                                when (state[nr][nc]) {
                                    0 -> unrevealed.add(nr to nc)
                                    2 -> flagged++
                                }
                            }
                        }
                    }

                    if (unrevealed.isEmpty()) continue

                    val remainingMinesNeeded = targetMines - flagged

                    // Rule 1: Remaining unrevealed neighbors must all be mines
                    if (unrevealed.size == remainingMinesNeeded) {
                        for ((ur, uc) in unrevealed) {
                            state[ur][uc] = 2
                            progress = true
                        }
                    }
                    // Rule 2: All required mines are flagged; all other neighbors are safe
                    else if (remainingMinesNeeded == 0) {
                        for ((ur, uc) in unrevealed) {
                            revealCell(ur, uc)
                            progress = true
                        }
                    }
                }
            }

            if (progress) {
                revealedSafeCount = countRevealed()
                continue
            }

            // Step 2: Set/subset reduction between frontier cells
            val frontier = mutableListOf<Triple<Int, Int, Int>>()
            val frontierNeighbors = mutableMapOf<Pair<Int, Int>, Set<Pair<Int, Int>>>()

            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    if (state[r][c] == 1 && adjacentMines[r][c] > 0) {
                        val unrevealed = mutableSetOf<Pair<Int, Int>>()
                        var flagged = 0
                        for (dr in -1..1) {
                            for (dc in -1..1) {
                                if (dr == 0 && dc == 0) continue
                                val nr = r + dr
                                val nc = c + dc
                                if (nr in 0 until rows && nc in 0 until cols) {
                                    when (state[nr][nc]) {
                                        0 -> unrevealed.add(nr to nc)
                                        2 -> flagged++
                                    }
                                }
                            }
                        }
                        if (unrevealed.isNotEmpty()) {
                            val rem = adjacentMines[r][c] - flagged
                            frontier.add(Triple(r, c, rem))
                            frontierNeighbors[r to c] = unrevealed
                        }
                    }
                }
            }

            for (i in 0 until frontier.size) {
                val (r1, c1, rem1) = frontier[i]
                val set1 = frontierNeighbors[r1 to c1] ?: continue

                for (j in 0 until frontier.size) {
                    if (i == j) continue
                    val (r2, c2, rem2) = frontier[j]
                    val set2 = frontierNeighbors[r2 to c2] ?: continue

                    // If set1 is a subset of set2
                    if (set2.containsAll(set1) && set2.size > set1.size) {
                        val diff = set2 - set1
                        val remDiff = rem2 - rem1

                        if (remDiff == 0) {
                            // All cells in diff are guaranteed safe
                            for ((dr, dc) in diff) {
                                if (state[dr][dc] == 0) {
                                    revealCell(dr, dc)
                                    progress = true
                                }
                            }
                        } else if (remDiff == diff.size) {
                            // All cells in diff are guaranteed mines
                            for ((dr, dc) in diff) {
                                if (state[dr][dc] == 0) {
                                    state[dr][dc] = 2
                                    progress = true
                                }
                            }
                        }
                    }
                }
                if (progress) break
            }

            if (progress) {
                revealedSafeCount = countRevealed()
                continue
            }

            // Step 3: Global remaining mine count deduction
            var totalFlagged = 0
            val allUnrevealed = mutableListOf<Pair<Int, Int>>()
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    when (state[r][c]) {
                        2 -> totalFlagged++
                        0 -> allUnrevealed.add(r to c)
                    }
                }
            }

            val remainingMinesTotal = totalMines - totalFlagged
            if (remainingMinesTotal == 0 && allUnrevealed.isNotEmpty()) {
                for ((ur, uc) in allUnrevealed) {
                    revealCell(ur, uc)
                    progress = true
                }
            } else if (remainingMinesTotal == allUnrevealed.size && allUnrevealed.isNotEmpty()) {
                for ((ur, uc) in allUnrevealed) {
                    state[ur][uc] = 2
                    progress = true
                }
            }

            if (progress) {
                revealedSafeCount = countRevealed()
            }
        }

        return revealedSafeCount == totalSafeCells
    }

    private fun revealCell(r: Int, c: Int) {
        if (state[r][c] != 0) return
        state[r][c] = 1

        if (adjacentMines[r][c] == 0) {
            val queue = ArrayDeque<Pair<Int, Int>>()
            queue.add(r to c)

            while (queue.isNotEmpty()) {
                val (currR, currC) = queue.removeFirst()
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        if (dr == 0 && dc == 0) continue
                        val nr = currR + dr
                        val nc = currC + dc
                        if (nr in 0 until rows && nc in 0 until cols && state[nr][nc] == 0) {
                            state[nr][nc] = 1
                            if (adjacentMines[nr][nc] == 0) {
                                queue.add(nr to nc)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun countRevealed(): Int {
        var count = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (state[r][c] == 1) count++
            }
        }
        return count
    }
}
