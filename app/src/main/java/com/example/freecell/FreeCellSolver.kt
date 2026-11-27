package com.example.freecell

import com.example.cards.PlayingCard
import java.util.PriorityQueue

internal object FreeCellSolver {

    private fun encode(card: PlayingCard): Int = (card.suit.ordinal shl 4) or card.rank.value
    private fun suitOf(code: Int): Int = code ushr 4
    private fun rankOf(code: Int): Int = code and 0xF
    private fun isRed(code: Int): Boolean = ((code ushr 4) and 1) != 0

    private fun canStackTableau(card: Int, target: Int): Boolean =
        isRed(card) != isRed(target) && rankOf(card) == rankOf(target) - 1

    private fun isOrderedRun(col: IntArray, startIdx: Int): Boolean {
        for (i in startIdx until col.lastIndex) {
            val u = col[i]
            val l = col[i + 1]
            if (isRed(u) == isRed(l) || rankOf(l) != rankOf(u) - 1) return false
        }
        return true
    }

    private fun isSafeToAutoFoundation(found: IntArray, code: Int): Boolean {
        val suit = suitOf(code)
        val rank = rankOf(code)
        if (rank != found[suit] + 1) return false
        if (rank <= 2) return true
        val oppSuitA = if (isRed(code)) 0 else 1
        val oppSuitB = oppSuitA + 2
        val minOpp = minOf(found[oppSuitA], found[oppSuitB])
        return rank <= minOpp + 1
    }

    private fun autoReduce(found: IntArray, free: IntArray, cols: Array<IntArray>) {
        val topIndices = IntArray(8) { cols[it].size }
        var changed = true
        while (changed) {
            changed = false
            for (i in 0 until 4) {
                val c = free[i]
                if (c != 0 && isSafeToAutoFoundation(found, c)) {
                    found[suitOf(c)]++
                    free[i] = 0
                    changed = true
                }
            }
            for (col in 0 until 8) {
                val len = topIndices[col]
                if (len > 0) {
                    val c = cols[col][len - 1]
                    if (isSafeToAutoFoundation(found, c)) {
                        found[suitOf(c)]++
                        topIndices[col] = len - 1
                        changed = true
                    }
                }
            }
        }
        for (col in 0 until 8) {
            val len = topIndices[col]
            if (len != cols[col].size) {
                cols[col] = cols[col].copyOf(len)
            }
        }
    }

    private fun stateKey(found: IntArray, free: IntArray, cols: Array<IntArray>): Long {
        val sortedFree = free.copyOf()
        sortedFree.sort()

        val colHashes = LongArray(8)
        for (c in 0 until 8) {
            var h = -0x340d631b7bdddcdbL
            val col = cols[c]
            for (i in col.indices) {
                h = (h xor col[i].toLong()) * 0x100000001b3L
            }
            colHashes[c] = h xor (col.size.toLong() shl 32)
        }
        colHashes.sort()

        var h = 0x517cc1b727220a95L
        for (i in 0 until 4) {
            h = (h * 31L) + found[i]
        }
        for (i in 0 until 4) {
            h = (h * 131L) + sortedFree[i]
        }
        for (i in 0 until 8) {
            h = h xor (colHashes[i] + -0x61c8864680b583ebL + (h shl 6) + (h ushr 2))
        }
        return h
    }

    private fun scoreState(found: IntArray, free: IntArray, cols: Array<IntArray>, depth: Int): Int {
        var foundSum = 0
        for (i in 0 until 4) foundSum += found[i]
        var emptyFree = 0
        for (i in 0 until 4) if (free[i] == 0) emptyFree++

        var emptyCols = 0
        var orderedPairs = 0
        var buriedPenalty = 0

        for (c in 0 until 8) {
            val col = cols[c]
            if (col.isEmpty()) {
                emptyCols++
                continue
            }
            for (i in 0 until col.lastIndex) {
                val u = col[i]
                val l = col[i + 1]
                if (isRed(u) != isRed(l) && rankOf(l) == rankOf(u) - 1) {
                    orderedPairs++
                }
                val r = rankOf(u)
                if (r <= 6) {
                    val depthAbove = col.lastIndex - i
                    buriedPenalty += (7 - r) * depthAbove * 3
                }
            }
        }

        return foundSum * 130 +
            emptyCols * 48 +
            emptyFree * 28 +
            orderedPairs * 14 -
            buriedPenalty -
            depth * 4
    }

    private class Node(
        val found: IntArray,
        val free: IntArray,
        val cols: Array<IntArray>,
        val depth: Int,
        val score: Int
    ) : Comparable<Node> {
        override fun compareTo(other: Node): Int = other.score.compareTo(score)
    }

    fun isWinnable(state: FreeCellGameState, maxExpandedStates: Int = 3500): Boolean {
        val initFound = IntArray(4)
        for (pile in state.foundations) {
            val top = pile.lastOrNull() ?: continue
            initFound[top.suit.ordinal] = top.rank.value
        }
        val initFree = IntArray(4) { i -> state.freeCells[i]?.let(::encode) ?: 0 }
        val initCols = Array(8) { c ->
            val pile = state.tableau[c]
            IntArray(pile.size) { r -> encode(pile[r]) }
        }

        autoReduce(initFound, initFree, initCols)
        if (initFound[0] + initFound[1] + initFound[2] + initFound[3] == 52) return true

        val visited = HashSet<Long>(maxExpandedStates * 2)
        val queue = PriorityQueue<Node>(256)
        val rootKey = stateKey(initFound, initFree, initCols)
        visited.add(rootKey)
        queue.add(Node(initFound, initFree, initCols, 0, scoreState(initFound, initFree, initCols, 0)))

        var expanded = 0
        while (queue.isNotEmpty() && expanded < maxExpandedStates) {
            val cur = queue.poll() ?: break
            expanded++

            val emptyFreeIdx = cur.free.indexOfFirst { it == 0 }
            val emptyFreeCount = cur.free.count { it == 0 }
            val emptyColIdx = cur.cols.indexOfFirst { it.isEmpty() }
            val emptyColCount = cur.cols.count { it.isEmpty() }

            fun enqueueChild(nFound: IntArray, nFree: IntArray, nCols: Array<IntArray>): Boolean {
                autoReduce(nFound, nFree, nCols)
                if (nFound[0] + nFound[1] + nFound[2] + nFound[3] == 52) return true
                val key = stateKey(nFound, nFree, nCols)
                if (visited.add(key)) {
                    val nd = cur.depth + 1
                    queue.add(Node(nFound, nFree, nCols, nd, scoreState(nFound, nFree, nCols, nd)))
                }
                return false
            }

            // 1. Non-safe foundation moves (from freeCells or tableau tops)
            for (i in 0 until 4) {
                val c = cur.free[i]
                if (c != 0 && rankOf(c) == cur.found[suitOf(c)] + 1) {
                    val nf = cur.found.copyOf().apply { this[suitOf(c)]++ }
                    val nfr = cur.free.copyOf().apply { this[i] = 0 }
                    val nc = cur.cols.copyOf()
                    if (enqueueChild(nf, nfr, nc)) return true
                }
            }
            for (col in 0 until 8) {
                val pile = cur.cols[col]
                if (pile.isNotEmpty()) {
                    val c = pile.last()
                    if (rankOf(c) == cur.found[suitOf(c)] + 1) {
                        val nf = cur.found.copyOf().apply { this[suitOf(c)]++ }
                        val nfr = cur.free.copyOf()
                        val nc = cur.cols.copyOf().apply { this[col] = pile.copyOf(pile.size - 1) }
                        if (enqueueChild(nf, nfr, nc)) return true
                    }
                }
            }

            // 2. FreeCell -> Tableau (non-empty first, then one canonical empty col)
            for (i in 0 until 4) {
                val c = cur.free[i]
                if (c == 0) continue
                for (dst in 0 until 8) {
                    val dPile = cur.cols[dst]
                    if (dPile.isNotEmpty() && canStackTableau(c, dPile.last())) {
                        val nf = cur.found.copyOf()
                        val nfr = cur.free.copyOf().apply { this[i] = 0 }
                        val nc = cur.cols.copyOf().apply {
                            this[dst] = dPile.copyOf(dPile.size + 1).also { it[dPile.size] = c }
                        }
                        if (enqueueChild(nf, nfr, nc)) return true
                    }
                }
                if (emptyColIdx >= 0) {
                    val nf = cur.found.copyOf()
                    val nfr = cur.free.copyOf().apply { this[i] = 0 }
                    val nc = cur.cols.copyOf().apply { this[emptyColIdx] = intArrayOf(c) }
                    if (enqueueChild(nf, nfr, nc)) return true
                }
            }

            // 3. Tableau run -> Tableau
            val maxMoveToNonEmpty = (1 + emptyFreeCount) * (1 shl emptyColCount.coerceAtMost(4))
            val maxMoveToEmpty = (1 + emptyFreeCount) * (1 shl (emptyColCount - 1).coerceIn(0, 4))

            for (src in 0 until 8) {
                val sPile = cur.cols[src]
                if (sPile.isEmpty()) continue

                // Find start of maximal ordered run ending at sPile.lastIndex
                var maxRunStart = sPile.lastIndex
                while (maxRunStart > 0 && isRed(sPile[maxRunStart - 1]) != isRed(sPile[maxRunStart]) &&
                    rankOf(sPile[maxRunStart]) == rankOf(sPile[maxRunStart - 1]) - 1
                ) {
                    maxRunStart--
                }

                // Try moving runs starting from maxRunStart down to top
                for (startIdx in maxRunStart until sPile.size) {
                    val runLen = sPile.size - startIdx
                    val lead = sPile[startIdx]

                    // Move to non-empty tableau
                    if (runLen <= maxMoveToNonEmpty) {
                        for (dst in 0 until 8) {
                            if (dst == src) continue
                            val dPile = cur.cols[dst]
                            if (dPile.isNotEmpty() && canStackTableau(lead, dPile.last())) {
                                // Skip moving part of an already-valid run onto another card of equal rank unless it exposes a non-run card or allows progress
                                if (startIdx > maxRunStart && sPile.size - maxRunStart <= maxMoveToNonEmpty) continue
                                val nf = cur.found.copyOf()
                                val nfr = cur.free.copyOf()
                                val nc = cur.cols.copyOf().apply {
                                    this[src] = sPile.copyOf(startIdx)
                                    val newDst = IntArray(dPile.size + runLen)
                                    dPile.copyInto(newDst)
                                    sPile.copyInto(newDst, destinationOffset = dPile.size, startIndex = startIdx)
                                    this[dst] = newDst
                                }
                                if (enqueueChild(nf, nfr, nc)) return true
                            }
                        }
                    }

                    // Move to one canonical empty tableau (only if startIdx > 0 so we don't bounce an entire single-run column)
                    if (emptyColIdx >= 0 && startIdx > 0 && runLen <= maxMoveToEmpty) {
                        val nf = cur.found.copyOf()
                        val nfr = cur.free.copyOf()
                        val nc = cur.cols.copyOf().apply {
                            this[src] = sPile.copyOf(startIdx)
                            this[emptyColIdx] = sPile.copyOfRange(startIdx, sPile.size)
                        }
                        if (enqueueChild(nf, nfr, nc)) return true
                    }
                }
            }

            // 4. Tableau top -> one canonical empty FreeCell
            if (emptyFreeIdx >= 0) {
                for (src in 0 until 8) {
                    val sPile = cur.cols[src]
                    if (sPile.size >= 2) {
                        val c = sPile.last()
                        val nf = cur.found.copyOf()
                        val nfr = cur.free.copyOf().apply { this[emptyFreeIdx] = c }
                        val nc = cur.cols.copyOf().apply { this[src] = sPile.copyOf(sPile.size - 1) }
                        if (enqueueChild(nf, nfr, nc)) return true
                    }
                }
            }
        }

        return false
    }
}
