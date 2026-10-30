package com.pandyabhavik.brainvita.core

/** A hole on the board: row and column in the 7x7 grid. */
data class Cell(val row: Int, val col: Int)

/** One jump: the peg at [from] hops over the peg at [over] into the empty hole at [to]. Indices are hole indices. */
data class Move(val from: Int, val over: Int, val to: Int)

/**
 * The classic 33-hole Brainvita board. Holes are numbered left to right, top to bottom; a position is a
 * Long bitmask with bit i set when hole i holds a peg. The game starts with the centre empty and is won
 * when one peg is left in the centre.
 */
object Board {
    private val mask = listOf(
        "..ooo..",
        "..ooo..",
        "ooooooo",
        "ooooooo",
        "ooooooo",
        "..ooo..",
        "..ooo..",
    )

    const val GRID = 7
    val cells: List<Cell> = buildList { for ((r, line) in mask.withIndex()) for ((c, ch) in line.withIndex()) if (ch == 'o') add(Cell(r, c)) }
    val size: Int get() = cells.size
    private val index: Map<Cell, Int> = cells.withIndex().associate { (i, c) -> c to i }

    fun indexOf(cell: Cell): Int? = index[cell]

    val centre: Int = index.getValue(Cell(3, 3))
    val startPosition: Long = ((1L shl size) - 1) and (1L shl centre).inv()

    /** Every jump that is geometrically possible, regardless of which holes hold pegs. */
    val moves: List<Move> = buildList {
        for ((i, cell) in cells.withIndex()) for ((dr, dc) in listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)) {
            val over = index[Cell(cell.row + dr, cell.col + dc)] ?: continue
            val to = index[Cell(cell.row + 2 * dr, cell.col + 2 * dc)] ?: continue
            add(Move(i, over, to))
        }
    }

    /** Moves grouped by the hole the peg jumps from, for quick lookup when a peg is picked up. */
    val movesFrom: List<List<Move>> = List(size) { i -> moves.filter { it.from == i } }
}
