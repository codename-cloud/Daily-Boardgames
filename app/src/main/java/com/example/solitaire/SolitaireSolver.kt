package com.example.solitaire

import com.example.cards.PlayingCard
import java.util.PriorityQueue

internal object SolitaireSolver {

    private fun encode(card: PlayingCard): Int = (card.suit.ordinal shl 4) or card.rank.value
    private fun suitOf(code: Int): Int = code ushr 4
    private fun rankOf(code: Int): Int = code and 0xF
    private fun isRed(code: Int): Boolean = ((code ushr 4) and 1) != 0

    private fun canPlaceOnTableau(card: Int, destTop: Int?): Boolean {
        return if (destTop == null) {
            rankOf(card) == 13
        } else {
            isRed(card) != isRed(destTop) && rankOf(card) == rankOf(destTop) - 1
        }
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

    private fun autoReduce(
        drawMode: SolitaireDrawMode,
        hiddenCols: Array<IntArray>,
        found: IntArray,
        faceDown: IntArray,
        faceUp: Array<IntArray>,
        stockRef: Array<IntArray>,
        wasteRef: Array<IntArray>
    ) {
        var changed = true
        while (changed) {
            changed = false
            // Safe tableau tops -> foundation (valid in both Draw 1 and Draw 3)
            for (c in 0 until 7) {
                val up = faceUp[c]
                if (up.isNotEmpty()) {
                    val top = up.last()
                    if (isSafeToAutoFoundation(found, top)) {
                        found[suitOf(top)]++
                        if (up.size > 1) {
                            faceUp[c] = up.copyOf(up.size - 1)
                        } else if (faceDown[c] > 0) {
                            val newCount = faceDown[c] - 1
                            faceDown[c] = newCount
                            faceUp[c] = intArrayOf(hiddenCols[c][newCount])
                        } else {
                            faceUp[c] = IntArray(0)
                        }
                        changed = true
                    }
                }
            }

            // In Draw 1, any card in stock/waste can be accessed freely without affecting cycle accessibility
            if (drawMode == SolitaireDrawMode.DRAW_1) {
                val w = wasteRef[0]
                for (i in w.indices) {
                    val card = w[i]
                    if (isSafeToAutoFoundation(found, card)) {
                        found[suitOf(card)]++
                        wasteRef[0] = removeAt(w, i)
                        changed = true
                        break
                    }
                }
                if (changed) continue
                val s = stockRef[0]
                for (i in s.indices) {
                    val card = s[i]
                    if (isSafeToAutoFoundation(found, card)) {
                        found[suitOf(card)]++
                        stockRef[0] = removeAt(s, i)
                        changed = true
                        break
                    }
                }
            }
        }
    }

    private fun removeAt(arr: IntArray, index: Int): IntArray {
        if (arr.size <= 1) return IntArray(0)
        val out = IntArray(arr.size - 1)
        if (index > 0) arr.copyInto(out, 0, 0, index)
        if (index < arr.lastIndex) arr.copyInto(out, index, index + 1, arr.size)
        return out
    }

    private fun appendInt(arr: IntArray, value: Int): IntArray {
        val out = IntArray(arr.size + 1)
        arr.copyInto(out)
        out[arr.size] = value
        return out
    }

    private fun appendRange(dst: IntArray, src: IntArray, startIdx: Int): IntArray {
        val count = src.size - startIdx
        val out = IntArray(dst.size + count)
        dst.copyInto(out)
        src.copyInto(out, dst.size, startIdx, src.size)
        return out
    }

    private fun isGuaranteedWin(
        found: IntArray,
        faceDown: IntArray,
        stock: IntArray,
        waste: IntArray
    ): Boolean {
        if (found[0] + found[1] + found[2] + found[3] == 52) return true
        return stock.isEmpty() && waste.isEmpty() && faceDown.all { it == 0 }
    }

    private fun stateKey(
        drawMode: SolitaireDrawMode,
        found: IntArray,
        faceDown: IntArray,
        faceUp: Array<IntArray>,
        stock: IntArray,
        waste: IntArray
    ): Long {
        var h = -0x340d631b7bdddcdbL
        for (i in 0 until 4) h = (h xor found[i].toLong()) * 0x100000001b3L
        for (c in 0 until 7) {
            h = (h xor (faceDown[c].toLong() shl 8)) * 0x100000001b3L
            val up = faceUp[c]
            h = (h xor (up.size.toLong() + 100L)) * 0x100000001b3L
            for (v in up) {
                h = (h xor v.toLong()) * 0x100000001b3L
            }
        }
        if (drawMode == SolitaireDrawMode.DRAW_1) {
            // In Draw 1, only the multiset of remaining stock+waste cards matters
            val combined = IntArray(stock.size + waste.size)
            stock.copyInto(combined)
            waste.copyInto(combined, stock.size)
            combined.sort()
            h = (h xor (combined.size.toLong() + 300L)) * 0x100000001b3L
            for (v in combined) h = (h xor v.toLong()) * 0x100000001b3L
        } else {
            h = (h xor (stock.size.toLong() + 500L)) * 0x100000001b3L
            for (v in stock) h = (h xor v.toLong()) * 0x100000001b3L
            h = (h xor (waste.size.toLong() + 700L)) * 0x100000001b3L
            for (v in waste) h = (h xor v.toLong()) * 0x100000001b3L
        }
        return h
    }

    private fun scoreState(
        found: IntArray,
        faceDown: IntArray,
        faceUp: Array<IntArray>,
        stock: IntArray,
        waste: IntArray,
        depth: Int
    ): Int {
        var foundSum = 0
        for (i in 0 until 4) foundSum += found[i]
        var hiddenSum = 0
        var emptyCols = 0
        for (c in 0 until 7) {
            hiddenSum += faceDown[c]
            if (faceDown[c] == 0 && faceUp[c].isEmpty()) emptyCols++
        }
        val remainingStockWaste = stock.size + waste.size
        return foundSum * 145 +
            (21 - hiddenSum) * 95 +
            (24 - remainingStockWaste) * 45 +
            emptyCols * 14 -
            depth * 5
    }

    private class Node(
        val found: IntArray,
        val faceDown: IntArray,
        val faceUp: Array<IntArray>,
        val stock: IntArray,
        val waste: IntArray,
        val depth: Int,
        val score: Int
    ) : Comparable<Node> {
        override fun compareTo(other: Node): Int = other.score.compareTo(score)
    }

    fun isWinnable(state: SolitaireGameState, maxExpandedStates: Int = 3000): Boolean {
        val drawMode = state.drawMode
        val hiddenCols = Array(7) { c ->
            val hidden = state.tableau[c].filter { !it.isFaceUp }
            IntArray(hidden.size) { i -> encode(hidden[i]) }
        }
        val initFaceDown = IntArray(7) { c -> hiddenCols[c].size }
        val initFaceUp = Array(7) { c ->
            val up = state.tableau[c].filter { it.isFaceUp }
            IntArray(up.size) { i -> encode(up[i]) }
        }
        val initFound = IntArray(4)
        for (pile in state.foundations) {
            val top = pile.lastOrNull() ?: continue
            initFound[top.suit.ordinal] = top.rank.value
        }
        val stockRef = arrayOf(IntArray(state.stock.size) { i -> encode(state.stock[i]) })
        val wasteRef = arrayOf(IntArray(state.waste.size) { i -> encode(state.waste[i]) })

        autoReduce(drawMode, hiddenCols, initFound, initFaceDown, initFaceUp, stockRef, wasteRef)
        if (isGuaranteedWin(initFound, initFaceDown, stockRef[0], wasteRef[0])) return true

        val visited = HashSet<Long>(maxExpandedStates * 2)
        val queue = PriorityQueue<Node>(256)
        visited.add(stateKey(drawMode, initFound, initFaceDown, initFaceUp, stockRef[0], wasteRef[0]))
        queue.add(
            Node(
                initFound,
                initFaceDown,
                initFaceUp,
                stockRef[0],
                wasteRef[0],
                0,
                scoreState(initFound, initFaceDown, initFaceUp, stockRef[0], wasteRef[0], 0)
            )
        )

        var expanded = 0
        while (queue.isNotEmpty() && expanded < maxExpandedStates) {
            val cur = queue.poll() ?: break
            expanded++

            fun enqueueChild(
                nFound: IntArray,
                nFaceDown: IntArray,
                nFaceUp: Array<IntArray>,
                nStock: IntArray,
                nWaste: IntArray
            ): Boolean {
                val sRef = arrayOf(nStock)
                val wRef = arrayOf(nWaste)
                autoReduce(drawMode, hiddenCols, nFound, nFaceDown, nFaceUp, sRef, wRef)
                if (isGuaranteedWin(nFound, nFaceDown, sRef[0], wRef[0])) return true
                val key = stateKey(drawMode, nFound, nFaceDown, nFaceUp, sRef[0], wRef[0])
                if (visited.add(key)) {
                    val nd = cur.depth + 1
                    queue.add(
                        Node(
                            nFound,
                            nFaceDown,
                            nFaceUp,
                            sRef[0],
                            wRef[0],
                            nd,
                            scoreState(nFound, nFaceDown, nFaceUp, sRef[0], wRef[0], nd)
                        )
                    )
                }
                return false
            }

            fun popTableauTop(col: Int, nFaceDown: IntArray, nFaceUp: Array<IntArray>) {
                val up = nFaceUp[col]
                if (up.size > 1) {
                    nFaceUp[col] = up.copyOf(up.size - 1)
                } else if (nFaceDown[col] > 0) {
                    val newCount = nFaceDown[col] - 1
                    nFaceDown[col] = newCount
                    nFaceUp[col] = intArrayOf(hiddenCols[col][newCount])
                } else {
                    nFaceUp[col] = IntArray(0)
                }
            }

            // 1. Non-safe tableau top -> foundation
            for (col in 0 until 7) {
                val up = cur.faceUp[col]
                if (up.isEmpty()) continue
                val top = up.last()
                val s = suitOf(top)
                if (rankOf(top) == cur.found[s] + 1) {
                    val nf = cur.found.copyOf().apply { this[s]++ }
                    val nfd = cur.faceDown.copyOf()
                    val nfu = cur.faceUp.copyOf()
                    popTableauTop(col, nfd, nfu)
                    if (enqueueChild(nf, nfd, nfu, cur.stock, cur.waste)) return true
                }
            }

            val firstEmptyCol = (0 until 7).firstOrNull { cur.faceDown[it] == 0 && cur.faceUp[it].isEmpty() } ?: -1

            // 2. Tableau -> Tableau moves
            for (src in 0 until 7) {
                val sUp = cur.faceUp[src]
                if (sUp.isEmpty()) continue

                // Full face-up stack move (index 0)
                run {
                    val lead = sUp[0]
                    val targets = if (rankOf(lead) == 13) {
                        if (cur.faceDown[src] > 0 && firstEmptyCol >= 0) intArrayOf(firstEmptyCol) else IntArray(0)
                    } else {
                        (0 until 7).filter { it != src && cur.faceUp[it].isNotEmpty() }.toIntArray()
                    }
                    for (dst in targets) {
                        val destTop = cur.faceUp[dst].lastOrNull()
                        if (canPlaceOnTableau(lead, destTop)) {
                            val nf = cur.found.copyOf()
                            val nfd = cur.faceDown.copyOf()
                            val nfu = cur.faceUp.copyOf()
                            nfu[dst] = appendRange(nfu[dst], sUp, 0)
                            if (nfd[src] > 0) {
                                val nc = nfd[src] - 1
                                nfd[src] = nc
                                nfu[src] = intArrayOf(hiddenCols[src][nc])
                            } else {
                                nfu[src] = IntArray(0)
                            }
                            if (enqueueChild(nf, nfd, nfu, cur.stock, cur.waste)) return true
                        }
                    }
                }

                // Partial face-up split ONLY when splitting exposes a card that immediately goes to foundation
                for (idx in 1 until sUp.size) {
                    val exposed = sUp[idx - 1]
                    if (rankOf(exposed) != cur.found[suitOf(exposed)] + 1) continue
                    val lead = sUp[idx]
                    for (dst in 0 until 7) {
                        if (dst == src) continue
                        val dUp = cur.faceUp[dst]
                        if (dUp.isEmpty() && cur.faceDown[dst] > 0) continue
                        if (dst != firstEmptyCol && dUp.isEmpty()) continue
                        if (canPlaceOnTableau(lead, dUp.lastOrNull())) {
                            val nf = cur.found.copyOf()
                            val nfd = cur.faceDown.copyOf()
                            val nfu = cur.faceUp.copyOf()
                            nfu[dst] = appendRange(dUp, sUp, idx)
                            nfu[src] = sUp.copyOf(idx)
                            if (enqueueChild(nf, nfd, nfu, cur.stock, cur.waste)) return true
                        }
                    }
                }
            }

            // 3. Stock / Waste plays
            if (drawMode == SolitaireDrawMode.DRAW_1) {
                // Any card in waste or stock is freely playable in Draw 1
                fun tryPlayPoolCard(card: Int, fromWaste: Boolean, idx: Int): Boolean {
                    val s = suitOf(card)
                    // Play to foundation
                    if (rankOf(card) == cur.found[s] + 1) {
                        val nf = cur.found.copyOf().apply { this[s]++ }
                        val ns = if (fromWaste) cur.stock else removeAt(cur.stock, idx)
                        val nw = if (fromWaste) removeAt(cur.waste, idx) else cur.waste
                        if (enqueueChild(nf, cur.faceDown.copyOf(), cur.faceUp.copyOf(), ns, nw)) return true
                    }
                    // Play to tableau (canonical empty col for King, or matching non-empty col)
                    val isKing = rankOf(card) == 13
                    if (isKing && firstEmptyCol >= 0) {
                        val nfu = cur.faceUp.copyOf().apply { this[firstEmptyCol] = intArrayOf(card) }
                        val ns = if (fromWaste) cur.stock else removeAt(cur.stock, idx)
                        val nw = if (fromWaste) removeAt(cur.waste, idx) else cur.waste
                        if (enqueueChild(cur.found.copyOf(), cur.faceDown.copyOf(), nfu, ns, nw)) return true
                    } else if (!isKing) {
                        for (dst in 0 until 7) {
                            val dUp = cur.faceUp[dst]
                            if (dUp.isNotEmpty() && canPlaceOnTableau(card, dUp.last())) {
                                val nfu = cur.faceUp.copyOf().apply { this[dst] = appendInt(dUp, card) }
                                val ns = if (fromWaste) cur.stock else removeAt(cur.stock, idx)
                                val nw = if (fromWaste) removeAt(cur.waste, idx) else cur.waste
                                if (enqueueChild(cur.found.copyOf(), cur.faceDown.copyOf(), nfu, ns, nw)) return true
                            }
                        }
                    }
                    return false
                }

                for (i in cur.waste.indices) {
                    if (tryPlayPoolCard(cur.waste[i], fromWaste = true, idx = i)) return true
                }
                for (i in cur.stock.indices) {
                    if (tryPlayPoolCard(cur.stock[i], fromWaste = false, idx = i)) return true
                }
            } else {
                // Draw 3: simulate cycling stock with drawCount = 3 up to one full pass
                var sArr = cur.stock
                var wArr = cur.waste
                val totalCards = sArr.size + wArr.size
                if (totalCards > 0) {
                    val maxSteps = totalCards + 4
                    val seenCycleStates = HashSet<Long>()
                    val playedCardTargets = HashSet<Int>()

                    for (step in 0 until maxSteps) {
                        val cycleKey = (sArr.contentHashCode().toLong() shl 32) xor (wArr.contentHashCode().toLong() and 0xFFFFFFFFL)
                        if (!seenCycleStates.add(cycleKey)) break

                        if (wArr.isNotEmpty()) {
                            val top = wArr.last()
                            val suit = suitOf(top)
                            // Foundation play
                            if (rankOf(top) == cur.found[suit] + 1) {
                                val actionId = (top shl 8) or 200
                                if (playedCardTargets.add(actionId)) {
                                    val nf = cur.found.copyOf().apply { this[suit]++ }
                                    val nw = wArr.copyOf(wArr.size - 1)
                                    if (enqueueChild(nf, cur.faceDown.copyOf(), cur.faceUp.copyOf(), sArr, nw)) return true
                                }
                            }
                            // Tableau play
                            val isKing = rankOf(top) == 13
                            if (isKing && firstEmptyCol >= 0) {
                                val actionId = (top shl 8) or firstEmptyCol
                                if (playedCardTargets.add(actionId)) {
                                    val nfu = cur.faceUp.copyOf().apply { this[firstEmptyCol] = intArrayOf(top) }
                                    val nw = wArr.copyOf(wArr.size - 1)
                                    if (enqueueChild(cur.found.copyOf(), cur.faceDown.copyOf(), nfu, sArr, nw)) return true
                                }
                            } else if (!isKing) {
                                for (dst in 0 until 7) {
                                    val dUp = cur.faceUp[dst]
                                    if (dUp.isNotEmpty() && canPlaceOnTableau(top, dUp.last())) {
                                        val actionId = (top shl 8) or dst
                                        if (playedCardTargets.add(actionId)) {
                                            val nfu = cur.faceUp.copyOf().apply { this[dst] = appendInt(dUp, top) }
                                            val nw = wArr.copyOf(wArr.size - 1)
                                            if (enqueueChild(cur.found.copyOf(), cur.faceDown.copyOf(), nfu, sArr, nw)) return true
                                        }
                                    }
                                }
                            }
                        }

                        // Advance 1 Draw-3 click matching SolitaireEngine.drawFromStock
                        if (sArr.isNotEmpty()) {
                            val count = minOf(3, sArr.size)
                            val remSize = sArr.size - count
                            val newStock = sArr.copyOf(remSize)
                            val newWaste = IntArray(wArr.size + count)
                            wArr.copyInto(newWaste)
                            for (k in 0 until count) {
                                newWaste[wArr.size + k] = sArr[sArr.size - 1 - k]
                            }
                            sArr = newStock
                            wArr = newWaste
                        } else if (wArr.isNotEmpty()) {
                            val newStock = IntArray(wArr.size)
                            for (k in wArr.indices) {
                                newStock[k] = wArr[wArr.lastIndex - k]
                            }
                            sArr = newStock
                            wArr = IntArray(0)
                        } else {
                            break
                        }
                    }
                }
            }
        }

        return false
    }
}
