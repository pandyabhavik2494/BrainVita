package com.pandyabhavik.brainvita.ui

import android.view.HapticFeedbackConstants
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pandyabhavik.brainvita.ui.theme.Palette

/** The whole app: one board, a few words, two buttons. Laid out for the Fold's cover screen or its inner screen. */
@Composable
fun GameScreen(viewModel: GameViewModel, sizeClass: WindowSizeClass) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val view = LocalView.current

    // One haptic per tap, matching what the tap did.
    LaunchedEffect(state.feedbackCount) {
        if (state.feedbackCount == 0) return@LaunchedEffect
        val constant = when (state.feedback) {
            Feedback.PICK_UP -> HapticFeedbackConstants.KEYBOARD_TAP
            Feedback.PUT_DOWN -> HapticFeedbackConstants.CLOCK_TICK
            Feedback.JUMP -> HapticFeedbackConstants.CONFIRM
            Feedback.REJECT -> HapticFeedbackConstants.REJECT
            Feedback.NONE -> return@LaunchedEffect
        }
        view.performHapticFeedback(constant)
    }

    var confirmRestart by remember { mutableStateOf(false) }
    val restart: () -> Unit = {
        if (state.game.moveCount > 0 && !state.game.isOver) {
            confirmRestart = true
        } else {
            viewModel.restart()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.baize)
            .background(Brush.radialGradient(listOf(Palette.baizeLight, Color.Transparent, Color.Black.copy(alpha = 0.35f)), radius = 1600f))
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        AnimatedVisibility(visible = state.loaded, enter = fadeIn(tween(350))) {
            val configuration = LocalConfiguration.current
            val wide = sizeClass.widthSizeClass != WindowWidthSizeClass.Compact ||
                configuration.screenWidthDp > configuration.screenHeightDp
            if (wide) {
                WideLayout(state, viewModel::tap, viewModel::undo, restart)
            } else {
                TallLayout(state, viewModel::tap, viewModel::undo, restart)
            }
        }
    }

    if (confirmRestart) {
        AlertDialog(
            onDismissRequest = { confirmRestart = false },
            containerColor = Palette.baizeLight,
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

/** Cover screen, portrait: words above, board in the middle, buttons below. */
@Composable
private fun TallLayout(state: UiState, onTap: (Int) -> Unit, onUndo: () -> Unit, onRestart: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(18.dp))
        Wordmark()
        Spacer(Modifier.height(6.dp))
        StatusLine(state)
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            Board(state, onTap)
            GameOverCard(state, onUndo, onRestart)
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            UndoButton(state.game.canUndo, onUndo, Modifier.weight(1f))
            RestartButton(onRestart, Modifier.weight(1f))
        }
    }
}

/** Inner screen and landscape: the board on the left, a panel of words and buttons on the right. */
@Composable
private fun WideLayout(state: UiState, onTap: (Int) -> Unit, onUndo: () -> Unit, onRestart: () -> Unit) {
    Row(Modifier.fillMaxSize().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1.4f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true), contentAlignment = Alignment.Center) {
                Board(state, onTap)
                GameOverCard(state, onUndo, onRestart)
            }
        }
        Spacer(Modifier.width(24.dp))
        Column(
            Modifier.weight(1f).fillMaxHeight().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Wordmark()
            Spacer(Modifier.height(36.dp))
            Text("PEGS LEFT", style = MaterialTheme.typography.labelSmall, color = Palette.ivoryDim)
            AnimatedContent(state.game.pegsLeft, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) }, label = "pegs") { pegs ->
                Text("$pegs", style = MaterialTheme.typography.displaySmall.copy(fontSize = 64.sp), color = Palette.brass)
            }
            Spacer(Modifier.height(18.dp))
            Text("MOVES", style = MaterialTheme.typography.labelSmall, color = Palette.ivoryDim)
            Text("${state.game.moveCount}", style = MaterialTheme.typography.headlineSmall, color = Palette.ivory)
            Spacer(Modifier.height(40.dp))
            UndoButton(state.game.canUndo, onUndo, Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            RestartButton(onRestart, Modifier.fillMaxWidth())
        }
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
    )
}

@Composable
private fun Wordmark() {
    Text("BrainVita", style = MaterialTheme.typography.displaySmall, color = Palette.ivory)
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
                .background(Palette.baize.copy(alpha = 0.94f))
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
