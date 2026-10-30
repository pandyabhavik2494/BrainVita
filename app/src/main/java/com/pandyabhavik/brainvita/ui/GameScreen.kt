package com.pandyabhavik.brainvita.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pandyabhavik.brainvita.ui.theme.Palette
import kotlinx.coroutines.delay

/** The whole app: one board, a few words, two buttons. Laid out for the Fold's cover screen or its inner screen. */
@Composable
fun GameScreen(viewModel: GameViewModel, sizeClass: WindowSizeClass) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val sounds = remember { Sounds(context.applicationContext) }
    val haptics = remember { Haptics(context.applicationContext) }
    DisposableEffect(sounds) { onDispose { sounds.release() } }

    // One sound and one haptic per action, matching what the action did. Nothing plays on first load,
    // even if the saved game was already over.
    LaunchedEffect(state.feedbackCount) {
        if (state.feedbackCount == 0) return@LaunchedEffect
        val settings = state.settings
        fun haptic(pattern: Haptics.Pattern) { if (settings.hapticsOn) haptics.play(pattern) }
        fun sound(sound: Sound) { if (settings.soundOn) sounds.play(sound) }
        when (state.feedback) {
            Feedback.PICK_UP -> { haptic(Haptics.Pattern.PICK_UP); sound(Sound.PICKUP) }
            Feedback.PUT_DOWN -> { haptic(Haptics.Pattern.PUT_DOWN); sound(Sound.DROP) }
            Feedback.REJECT -> { haptic(Haptics.Pattern.REJECT); sound(Sound.REJECT) }
            Feedback.UNDO -> { haptic(Haptics.Pattern.UNDO); sound(Sound.UNDO) }
            Feedback.JUMP, Feedback.WIN, Feedback.STUCK -> {
                haptic(Haptics.Pattern.JUMP)
                sound(Sound.JUMP)
                delay(80)
                sound(Sound.CAPTURE)
                if (state.feedback == Feedback.WIN) { delay(350); sound(Sound.WIN); haptic(Haptics.Pattern.WIN) }
                if (state.feedback == Feedback.STUCK) { delay(350); sound(Sound.STUCK); haptic(Haptics.Pattern.STUCK) }
            }
            Feedback.NONE -> Unit
        }
    }

    var confirmRestart by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val restart: () -> Unit = {
        if (state.game.moveCount > 0 && !state.game.isOver) {
            confirmRestart = true
        } else {
            viewModel.restart()
        }
    }
    val actions = Actions(onTap = viewModel::tap, onUndo = viewModel::undo, onRestart = restart, onSettings = { showSettings = true })

    val backdrop = state.settings.backdrop
    Box(
        Modifier
            .fillMaxSize()
            .background(backdrop.edge)
            .background(Brush.radialGradient(listOf(backdrop.centre, Color.Transparent, Color.Black.copy(alpha = 0.35f)), radius = 1600f))
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        AnimatedVisibility(visible = state.loaded, enter = fadeIn(tween(350))) {
            // Side by side only when the screen is wider than it is tall (inner screen, or the cover
            // screen on its side); every other shape stacks, with the board centred.
            BoxWithConstraints(Modifier.fillMaxSize()) {
                if (maxWidth > maxHeight) {
                    WideLayout(state, actions)
                } else {
                    TallLayout(state, actions, roomy = sizeClass.widthSizeClass != WindowWidthSizeClass.Compact)
                }
            }
        }
    }

    if (showSettings) {
        SettingsSheet(
            settings = state.settings,
            onSound = viewModel::setSound,
            onHaptics = viewModel::setHaptics,
            onMarble = viewModel::setMarble,
            onBoard = viewModel::setBoard,
            onBackdrop = viewModel::setBackdrop,
            onDismiss = { showSettings = false },
        )
    }

    if (confirmRestart) {
        AlertDialog(
            onDismissRequest = { confirmRestart = false },
            containerColor = backdrop.surface,
            titleContentColor = Palette.ivory,
            textContentColor = Palette.ivoryDim,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Start over?", style = MaterialTheme.typography.headlineSmall) },
            text = { Text("The pegs go back to the start. Your ${state.game.moveCount} moves so far will be lost.") },
            confirmButton = {
                TextButton(onClick = { confirmRestart = false; viewModel.restart() }) {
                    Text("Start over", color = Palette.brass, style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestart = false }) {
                    Text("Keep playing", color = Palette.ivoryDim, style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }
}

private class Actions(val onTap: (Int) -> Unit, val onUndo: () -> Unit, val onRestart: () -> Unit, val onSettings: () -> Unit)

/**
 * Any screen taller than it is wide: words above, the board centred beneath them, buttons at the bottom.
 * [roomy] is the inner screen held upright, where the board is capped so it never crowds the words.
 */
@Composable
private fun TallLayout(state: UiState, actions: Actions, roomy: Boolean) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sidePadding = if (roomy) 40.dp else 20.dp
        val boardSide = minOf(maxWidth - sidePadding * 2, maxHeight - 230.dp).coerceAtLeast(120.dp)
        Column(Modifier.fillMaxSize().padding(horizontal = sidePadding), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth()) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Wordmark()
                    Spacer(Modifier.height(6.dp))
                    StatusLine(state)
                }
                SettingsButton(actions.onSettings, Modifier.align(Alignment.TopEnd))
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(boardSide).align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
                Board(state, actions.onTap)
                GameOverCard(state, actions.onUndo, actions.onRestart)
            }
            Spacer(Modifier.weight(1.2f))
            Row(
                (if (roomy) Modifier.width(minOf(boardSide, 520.dp)) else Modifier.fillMaxWidth()).padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                UndoButton(state.game.canUndo, actions.onUndo, Modifier.weight(1f))
                RestartButton(actions.onRestart, Modifier.weight(1f))
            }
        }
    }
}

/**
 * Inner screen and landscape: a square board and a fixed-width panel, sized together so the board never
 * runs under the panel or off the screen, and centred as one group.
 */
@Composable
private fun WideLayout(state: UiState, actions: Actions) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        val panelWidth = if (maxWidth > 760.dp) 340.dp else 300.dp
        val gap = 32.dp
        val boardSide = minOf(maxHeight, maxWidth - panelWidth - gap)
        // The board sits on the right, under the right hand; the panel takes the left.
        Row(Modifier.width(boardSide + gap + panelWidth).height(boardSide), verticalAlignment = Alignment.CenterVertically) {
            SidePanel(state, actions, Modifier.width(panelWidth).fillMaxHeight())
            Spacer(Modifier.width(gap))
            Box(Modifier.size(boardSide), contentAlignment = Alignment.Center) {
                Board(state, actions.onTap)
                GameOverCard(state, actions.onUndo, actions.onRestart)
            }
        }
    }
}

@Composable
private fun SidePanel(state: UiState, actions: Actions, modifier: Modifier) {
    Column(modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.Start) {
        Box(Modifier.fillMaxWidth()) {
            Box(Modifier.align(Alignment.CenterStart)) { Wordmark() }
            SettingsButton(actions.onSettings, Modifier.align(Alignment.CenterEnd))
        }
        Spacer(Modifier.weight(1f))
        Text("PEGS LEFT", style = MaterialTheme.typography.labelSmall, color = Palette.ivoryDim)
        AnimatedContent(state.game.pegsLeft, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) }, label = "pegs") { pegs ->
            Text("$pegs", style = MaterialTheme.typography.displaySmall.copy(fontSize = 72.sp), color = Palette.brass)
        }
        Spacer(Modifier.height(20.dp))
        Text("MOVES", style = MaterialTheme.typography.labelSmall, color = Palette.ivoryDim)
        Text("${state.game.moveCount}", style = MaterialTheme.typography.headlineSmall, color = Palette.ivory)
        Spacer(Modifier.weight(1.3f))
        UndoButton(state.game.canUndo, actions.onUndo, Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        RestartButton(actions.onRestart, Modifier.fillMaxWidth())
    }
}

@Composable
private fun Board(state: UiState, onTap: (Int) -> Unit) {
    BoardView(
        position = state.game.position,
        selected = state.selected,
        targets = state.targets,
        lastMove = state.lastMove,
        onTap = onTap,
        modifier = Modifier.fillMaxSize(),
        marble = state.settings.marble,
        finish = state.settings.board,
    )
}

@Composable
private fun Wordmark() {
    Text("BrainVita", style = MaterialTheme.typography.displaySmall, color = Palette.ivory)
}

@Composable
private fun SettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Palette.ivoryDim)
    }
}

@Composable
private fun StatusLine(state: UiState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("PEGS LEFT  ", style = MaterialTheme.typography.labelSmall, color = Palette.ivoryDim)
        AnimatedContent(state.game.pegsLeft, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) }, label = "pegs") { pegs ->
            Text("$pegs", style = MaterialTheme.typography.titleMedium, color = Palette.brass, fontWeight = FontWeight.SemiBold)
        }
        Text("   ·   MOVES  ", style = MaterialTheme.typography.labelSmall, color = Palette.ivoryDim)
        Text("${state.game.moveCount}", style = MaterialTheme.typography.titleMedium, color = Palette.ivory, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun UndoButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PillButton("Undo", Icons.AutoMirrored.Filled.Undo, onClick, modifier, enabled = enabled, filled = false)
}

@Composable
private fun RestartButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    PillButton("Restart", Icons.Filled.Refresh, onClick, modifier, enabled = true, filled = false)
}

@Composable
private fun PillButton(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier, enabled: Boolean, filled: Boolean) {
    val shape = RoundedCornerShape(50)
    if (filled) {
        Button(
            onClick = onClick, enabled = enabled, modifier = modifier.height(52.dp), shape = shape,
            colors = ButtonDefaults.buttonColors(containerColor = Palette.brass, contentColor = Palette.walnutDark),
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        OutlinedButton(
            onClick = onClick, enabled = enabled, modifier = modifier.height(52.dp), shape = shape,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Palette.ivory, disabledContentColor = Palette.ivoryDim.copy(alpha = 0.4f)),
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Slides up over the board when the game is won or stuck. */
@Composable
private fun GameOverCard(state: UiState, onUndo: () -> Unit, onRestart: () -> Unit) {
    val game = state.game
    AnimatedVisibility(
        visible = game.isOver,
        enter = fadeIn(tween(500, delayMillis = 350)) + slideInVertically(tween(500, delayMillis = 350)) { it / 6 } + scaleIn(tween(500, delayMillis = 350), initialScale = 0.92f),
        exit = fadeOut(tween(150)),
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.78f)
                .clip(RoundedCornerShape(24.dp))
                .background(state.settings.backdrop.edge.copy(alpha = 0.94f))
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val won = game.isWon
            Text(
                if (won) "Brilliant" else "No moves left",
                style = MaterialTheme.typography.displaySmall,
                color = if (won) Palette.brass else Palette.ivory,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (won) "Solved in ${game.moveCount} moves" else "${game.pegsLeft} ${if (game.pegsLeft == 1) "peg" else "pegs"} remaining",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.ivoryDim,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRestart, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Palette.brass, contentColor = Palette.walnutDark),
            ) { Text("New game", style = MaterialTheme.typography.labelLarge) }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onUndo, modifier = Modifier.fillMaxWidth()) {
                Text("Undo last move", color = Palette.ivoryDim, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
