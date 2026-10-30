package com.pandyabhavik.brainvita.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesTest {
    @Test
    fun boardHas33HolesAnd76PossibleJumps() {
        assertEquals(33, Board.size)
        assertEquals(76, Board.moves.size)
        assertEquals(32, Rules.pegCount(Board.startPosition))
        assertFalse(Rules.hasPeg(Board.startPosition, Board.centre))
    }

    @Test
    fun startHasFourOpeningMovesAllIntoTheCentre() {
        val moves = Rules.legalMoves(Board.startPosition)
        assertEquals(4, moves.size)
        moves.forEach { assertEquals(Board.centre, it.to) }
    }

    @Test
    fun applyRemovesTwoPegsAndAddsOne() {
        val move = Rules.legalMoves(Board.startPosition).first()
        val after = Rules.apply(Board.startPosition, move)
        assertEquals(31, Rules.pegCount(after))
        assertFalse(Rules.hasPeg(after, move.from))
        assertFalse(Rules.hasPeg(after, move.over))
        assertTrue(Rules.hasPeg(after, move.to))
    }

    @Test
    fun winNeedsOnePegInTheCentre() {
        val corner = Board.indexOf(Cell(0, 2))!!
        assertTrue(Rules.isWon(1L shl Board.centre))
        assertFalse(Rules.isWon(1L shl corner))
        assertTrue(Rules.isStuck(1L shl corner))
        assertFalse(Rules.isStuck(Board.startPosition))
    }

    @Test
    fun gameUndoAndRestart() {
        var game = Game()
        val first = Rules.legalMoves(game.position).first()
        game = game.play(first)
        assertEquals(1, game.moveCount)
        assertEquals(31, game.pegsLeft)
        game = game.undo()
        assertEquals(0, game.moveCount)
        assertFalse(game.canUndo)
        game = game.play(first)
        game = game.play(Rules.legalMoves(game.position).first()).restart()
        assertEquals(Board.startPosition, game.position)
    }

    @Test
    fun gameSurvivesSerialization() {
        var game = Game()
        repeat(3) { game = game.play(Rules.legalMoves(game.position).first()) }
        assertEquals(game, Game.deserialize(game.serialize()))
        assertEquals(Game(), Game.deserialize(""))
        assertNull(Game.deserialize("garbage"))
        assertNull(Game.deserialize("1.2.99"))
    }

    @Test
    fun aFullGameCanBePlayedToAWin() {
        // A known winning line from the centre-empty start, in hole indices (0..32, row by row).
        val line = "4.9.16,7.8.9,0.3.8,2.1.0,9.8.7,6.7.8,10.5.2,12.11.10,15.8.3,0.3.8,13.14.15,15.8.3,17.16.15,19.18.17,17.10.5,2.5.10,22.15.8,3.8.15,20.21.22,23.22.21,29.24.17,26.25.24,30.27.22,15.22.27,32.31.30,30.27.22,21.22.23,23.24.25,10.17.24,25.24.23,28.23.16"
        val game = Game.deserialize(line)!!
        assertEquals(31, game.moveCount)
        assertTrue(game.isWon)
        assertTrue(game.isOver)
    }
}
