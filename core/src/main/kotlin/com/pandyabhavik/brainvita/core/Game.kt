package com.pandyabhavik.brainvita.core

/**
 * One game in progress: the moves made so far. Immutable; every action returns a new Game, which makes
 * undo and saving trivial.
 */
data class Game(val played: List<Move> = emptyList()) {
    val position: Long = played.fold(Board.startPosition) { p, m -> Rules.apply(p, m) }
    val pegsLeft: Int get() = Rules.pegCount(position)
    val moveCount: Int get() = played.size
    val isWon: Boolean get() = Rules.isWon(position)
    val isStuck: Boolean get() = Rules.isStuck(position)
    val isOver: Boolean get() = isWon || isStuck
    val canUndo: Boolean get() = played.isNotEmpty()

    fun play(move: Move): Game {
        require(Rules.isLegal(position, move)) { "Illegal move $move" }
        return Game(played + move)
    }

    fun undo(): Game = if (!canUndo) this else Game(played.dropLast(1))

    fun restart(): Game = Game()

    /** The moves as text, so the game survives the app being closed: "from.over.to,from.over.to,...". */
    fun serialize(): String = played.joinToString(",") { "${it.from}.${it.over}.${it.to}" }

    companion object {
        fun deserialize(text: String): Game? = runCatching {
            val moves = if (text.isBlank()) emptyList() else text.split(',').map { m ->
                val (f, o, t) = m.split('.').map(String::toInt); Move(f, o, t)
            }
            // Replay to make sure every move was legal before trusting the saved text.
            var p = Board.startPosition
            for (m in moves) { check(m in Board.moves && Rules.isLegal(p, m)); p = Rules.apply(p, m) }
            Game(moves)
        }.getOrNull()
    }
}
