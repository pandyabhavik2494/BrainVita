package com.pandyabhavik.brainvita.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandyabhavik.brainvita.ui.theme.Palette

/** Sound, haptics and the look of the table. Changes apply at once, behind the sheet, and are remembered. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: Settings,
    onSound: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onMarble: (Marble) -> Unit,
    onBoard: (BoardFinish) -> Unit,
    onBackdrop: (Backdrop) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val surface = settings.backdrop.surface
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = surface,
        contentColor = Palette.ivory,
        scrimColor = settings.backdrop.edge.copy(alpha = 0.6f),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding(),
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall, color = Palette.ivory)
            Spacer(Modifier.height(14.dp))
            ToggleRow("Sound", settings.soundOn, onSound, settings.backdrop)
            ToggleRow("Haptics", settings.hapticsOn, onHaptics, settings.backdrop)

            Section("MARBLES")
            SwatchRow(Marble.entries, settings.marble, { it.label }, onMarble) { marble, r ->
                drawPeg(Offset(r, r), r * 0.72f, marble, lift = 0f)
            }

            Section("BOARD")
            SwatchRow(BoardFinish.entries, settings.board, { it.label }, onBoard) { finish, r ->
                drawBoardSwatch(finish, r)
            }

            Section("BACKGROUND")
            SwatchRow(Backdrop.entries, settings.backdrop, { it.label }, onBackdrop) { backdrop, r ->
                drawCircle(Brush.radialGradient(listOf(backdrop.centre, backdrop.edge), center = Offset(r * 0.8f, r * 0.8f), radius = r * 1.2f), r * 0.78f, Offset(r, r))
                drawCircle(Color.White.copy(alpha = 0.12f), r * 0.78f, Offset(r, r), style = Stroke(r * 0.05f))
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(18.dp))
    Text(title, style = MaterialTheme.typography.labelSmall, color = Palette.ivoryDim)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, backdrop: Backdrop) {
    Row(
        Modifier.fillMaxWidth().height(50.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = Palette.ivory)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Palette.walnutDark,
                checkedTrackColor = Palette.brass,
                uncheckedThumbColor = Palette.ivoryDim,
                uncheckedTrackColor = backdrop.edge,
                uncheckedBorderColor = Palette.brassDark,
            ),
        )
    }
}

/** A row of swatches, each named beneath it; the chosen one wears a glow ring. */
@Composable
private fun <T> SwatchRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onPick: (T) -> Unit,
    draw: DrawScope.(T, Float) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (option in options) {
            val chosen = option == selected
            Column(
                Modifier
                    .width(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPick(option) }
                    .semantics { contentDescription = label(option); this.selected = chosen }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Canvas(Modifier.size(44.dp)) {
                    val r = size.minDimension / 2f
                    if (chosen) drawCircle(Palette.glow.copy(alpha = 0.9f), r * 0.98f, Offset(r, r), style = Stroke(r * 0.09f))
                    draw(option, r)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    label(option),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                    color = if (chosen) Palette.ivory else Palette.ivoryDim,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

/** A little wooden tile in the finish, with a single hole so it reads as a board. */
private fun DrawScope.drawBoardSwatch(finish: BoardFinish, r: Float) {
    val side = r * 1.5f
    val topLeft = Offset(r - side / 2f, r - side / 2f)
    drawRoundRect(
        Brush.radialGradient(listOf(finish.light, finish.base, finish.dark), center = Offset(r * 0.7f, r * 0.7f), radius = side * 1.1f),
        topLeft = topLeft, size = Size(side, side), cornerRadius = CornerRadius(side * 0.2f),
    )
    drawRoundRect(Color.Black.copy(alpha = 0.35f), topLeft = topLeft, size = Size(side, side), cornerRadius = CornerRadius(side * 0.2f), style = Stroke(r * 0.06f))
    drawCircle(finish.holeDeep, r * 0.22f, Offset(r, r))
    drawCircle(finish.lip, r * 0.24f, Offset(r, r + r * 0.03f), style = Stroke(r * 0.03f))
}
