package com.pandyabhavik.brainvita.core

/** The rules of Brainvita over bitmask positions. Pure functions. */
object Rules {
    fun hasPeg(position: Long, index: Int): Boolean = (position shr index) and 1L == 1L

    fun pegCount(position: Long): Int = java.lang.Long.bitCount(position)

    fun isLegal(position: Long, move: Move): Boolean =
        hasPeg(position, move.from) && hasPeg(position, move.over) && !hasPeg(position, move.to)

    fun apply(position: Long, move: Move): Long =
        position and (1L shl move.from).inv() and (1L shl move.over).inv() or (1L shl move.to)

    fun legalMoves(position: Long): List<Move> = Board.moves.filter { isLegal(position, it) }

    fun legalMovesFrom(position: Long, from: Int): List<Move> = Board.movesFrom[from].filter { isLegal(position, it) }

    fun hasLegalMove(position: Long): Boolean = Board.moves.any { isLegal(position, it) }

    /** One peg left, in the centre. */
    fun isWon(position: Long): Boolean = pegCount(position) == 1 && hasPeg(position, Board.centre)

    /** No peg can jump and the game isn't won. */
    fun isStuck(position: Long): Boolean = !isWon(position) && !hasLegalMove(position)
}
