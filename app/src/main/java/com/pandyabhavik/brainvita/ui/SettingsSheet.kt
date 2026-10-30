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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pandyabhavik.brainvita.ui.theme.Palette

/** Sound, haptics and the finish of the marbles. Changes apply at once, behind the sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: Settings,
    onSound: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onMarble: (Marble) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Palette.baizeLight,
        contentColor = Palette.ivory,
        scrimColor = Palette.baize.copy(alpha = 0.6f),
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
            Spacer(Modifier.height(18.dp))
            ToggleRow("Sound", settings.soundOn, onSound)
            ToggleRow("Haptics", settings.hapticsOn, onHaptics)
            Spacer(Modifier.height(18.dp))
            Text("MARBLES", style = MaterialTheme.typography.labelSmall, color = Palette.ivoryDim)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                for (marble in Marble.entries) {
                    MarbleSwatch(marble, selected = marble == settings.marble) { onMarble(marble) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(settings.marble.label, style = MaterialTheme.typography.bodyMedium, color = Palette.ivoryDim)
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(52.dp),
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
                uncheckedTrackColor = Palette.baize,
                uncheckedBorderColor = Palette.brassDark,
            ),
        )
    }
}

/** A little glossy marble of the given finish; the chosen one wears a glow ring. */
@Composable
private fun MarbleSwatch(marble: Marble, selected: Boolean, onClick: () -> Unit) {
    Canvas(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = marble.label; this.selected = selected },
    ) {
        val r = size.minDimension / 2f
        val centre = Offset(r, r)
        if (selected) drawCircle(Palette.glow.copy(alpha = 0.9f), r * 0.98f, centre, style = Stroke(r * 0.1f))
        drawPeg(centre, r * 0.72f, marble, lift = 0f)
    }
}
