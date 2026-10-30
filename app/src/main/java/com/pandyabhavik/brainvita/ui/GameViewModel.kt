package com.pandyabhavik.brainvita.ui

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pandyabhavik.brainvita.core.Game
import com.pandyabhavik.brainvita.core.Move
import com.pandyabhavik.brainvita.core.Rules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val Context.store: DataStore<Preferences> by preferencesDataStore("brainvita")
private val GAME_KEY = stringPreferencesKey("game")

/** What the screen shows: the game and which peg, if any, is picked up. */
data class UiState(
    val game: Game = Game(),
    val selected: Int? = null,
    /** The last jump, so the board can animate it. */
    val lastMove: Move? = null,
    val loaded: Boolean = false,
    /** What the last tap did, with a counter so the screen can play one haptic per tap. */
    val feedback: Feedback = Feedback.NONE,
    val feedbackCount: Int = 0,
) {
    val targets: Set<Int> get() = selected?.let { s -> Rules.legalMovesFrom(game.position, s).map { it.to }.toSet() } ?: emptySet()
}

enum class Feedback { NONE, PICK_UP, PUT_DOWN, JUMP, REJECT }

/** One game at a time. Every move is written to disk, so closing the app and coming back resumes the same game. */
class GameViewModel(private val app: Application) : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch {
            val saved = app.store.data.first()[GAME_KEY]?.let(Game::deserialize)
            _state.value = UiState(game = saved ?: Game(), loaded = true)
        }
    }

    fun tap(index: Int) {
        val s = _state.value
        val game = s.game
        when {
            // Tap a peg: pick it up (or put it down if it was already selected).
            Rules.hasPeg(game.position, index) -> _state.update {
                val pickUp = it.selected != index
                it.copy(selected = if (pickUp) index else null, feedback = if (pickUp) Feedback.PICK_UP else Feedback.PUT_DOWN, feedbackCount = it.feedbackCount + 1)
            }
            // Tap an empty hole with a peg in hand: jump if that hole is a legal landing.
            s.selected != null -> {
                val move = Rules.legalMovesFrom(game.position, s.selected).firstOrNull { it.to == index }
                if (move != null) update { it.copy(game = it.game.play(move), selected = null, lastMove = move, feedback = Feedback.JUMP, feedbackCount = it.feedbackCount + 1) }
                else _state.update { it.copy(selected = null, feedback = Feedback.REJECT, feedbackCount = it.feedbackCount + 1) }
            }
        }
    }

    fun undo() = update { it.copy(game = it.game.undo(), selected = null, lastMove = null) }
    fun restart() = update { it.copy(game = it.game.restart(), selected = null, lastMove = null) }

    private fun update(change: (UiState) -> UiState) {
        _state.update(change)
        val game = _state.value.game
        viewModelScope.launch { app.store.edit { it[GAME_KEY] = game.serialize() } }
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory = viewModelFactory { initializer { GameViewModel(app) } }
    }
}
