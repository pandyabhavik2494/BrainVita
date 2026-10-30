package com.pandyabhavik.brainvita.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.pandyabhavik.brainvita.core.Board
import com.pandyabhavik.brainvita.core.Move
import com.pandyabhavik.brainvita.core.Rules
import com.pandyabhavik.brainvita.ui.theme.Palette
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * The walnut board, drawn with Canvas alone. Fills whatever size it is given, keeping the board square,
 * and reports taps as hole indices. The last jump slides the peg along an arc into its new hole while the
 * peg it jumped over sinks away.
 */
@Composable
fun BoardView(
    position: Long,
    selected: Int?,
    targets: Set<Int>,
    lastMove: Move?,
    onTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
    marble: Marble = Marble.BRASS,
) {
    // 0 = the moment before the jump, 1 = settled in the new hole.
    val flight = remember(lastMove) { Animatable(if (lastMove == null) 1f else 0f) }
    LaunchedEffect(lastMove) { if (lastMove != null) flight.animateTo(1f, tween(260)) }

    // The selected peg lifts off the board with a little spring.
    val lift by animateFloatAsState(
        targetValue = if (selected != null) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "lift",
    )

    // Legal landing holes breathe softly.
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow",
    )

    val pegs = Rules.pegCount(position)
    // Geometry (with its grain paths) per canvas size, kept across recompositions.
    val geometries = remember { HashMap<Pair<Float, Float>, Geometry>() }
    Box(
        modifier
            .semantics { contentDescription = "BrainVita board, $pegs pegs" }
            .pointerInput(Unit) {
                detectTapGestures { tap ->
                    val g = Geometry(size.width.toFloat(), size.height.toFloat())
                    val hit = (0 until Board.size).minByOrNull { (g.centre(it) - tap).getDistanceSquared() } ?: return@detectTapGestures
                    if ((g.centre(hit) - tap).getDistance() <= g.unit * 0.5f) onTap(hit)
                }
            }
            // The geometry and grain paths are built once per size; only the draw lambda re-runs per frame.
            .drawWithCache {
                val g = geometries.getOrPut(size.width to size.height) { Geometry(size.width, size.height) }
                onDrawBehind {
                    drawBoard(g)

                    val holeRadius = g.unit * 0.30f
                    val pegRadius = g.unit * 0.36f
                    val t = flight.value
                    val inFlight = if (t < 1f) lastMove else null

                    for (i in 0 until Board.size) {
                        val c = g.centre(i)
                        drawHole(c, holeRadius)
                        if (i == Board.centre && !Rules.hasPeg(position, i) && i != inFlight?.to) {
                            // The goal: a faint brass ring at the bottom of the centre hole.
                            drawCircle(Palette.brass.copy(alpha = 0.22f), holeRadius * 0.5f, c, style = Stroke(g.unit * 0.035f))
                        }
                        if (i in targets) {
                            val alpha = 0.45f + 0.45f * pulse
                            val r = holeRadius * (1.05f + 0.12f * pulse)
                            drawCircle(Palette.glow.copy(alpha = alpha * 0.35f), r * 1.25f, c)
                            drawCircle(Palette.glow.copy(alpha = alpha), r, c, style = Stroke(g.unit * 0.06f))
                        }
                    }

                    // Resting pegs; the selected one is drawn last so its shadow falls over its neighbours.
                    for (i in 0 until Board.size) {
                        if (i == selected) continue
                        if (inFlight != null && i == inFlight.to) continue
                        if (inFlight != null && i == inFlight.over) {
                            val sink = 1f - t
                            drawPeg(g.centre(i), pegRadius * sink, marble, lift = 0f, alpha = sink)
                            continue
                        }
                        if (!Rules.hasPeg(position, i)) continue
                        drawPeg(g.centre(i), pegRadius, marble, lift = 0f)
                    }
                    if (selected != null && Rules.hasPeg(position, selected)) {
                        drawPeg(g.centre(selected), pegRadius, marble, lift = lift, ring = true)
                    }
                    if (inFlight != null) {
                        val from = g.centre(inFlight.from)
                        val to = g.centre(inFlight.to)
                        val eased = 1f - (1f - t) * (1f - t) // ease-out: quick off the mark, gentle landing
                        val arc = sin(eased * PI.toFloat()) // rises mid-jump
                        drawPeg(Offset(from.x + (to.x - from.x) * eased, from.y + (to.y - from.y) * eased), pegRadius, marble, lift = arc * 1.4f)
                    }
                }
            },
    )
}

/** The wooden slab with grain, edge shading and a soft shadow on the baize. */
private fun DrawScope.drawBoard(g: Geometry) {
    val rect = g.boardRect
    val corner = CornerRadius(g.unit * 0.9f)
    // Shadow on the cloth.
    drawRoundRect(
        Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent), center = rect.center + Offset(0f, g.unit * 0.25f), radius = rect.width * 0.78f),
        topLeft = rect.topLeft - Offset(g.unit * 0.3f, g.unit * 0.15f), size = Size(rect.width + g.unit * 0.6f, rect.height + g.unit * 0.75f), cornerRadius = corner,
    )
    // Wood body: lit from the top-left.
    drawRoundRect(
        Brush.radialGradient(listOf(Palette.walnutLight, Palette.walnut, Palette.walnutDark), center = rect.center - Offset(rect.width * 0.25f, rect.height * 0.3f), radius = rect.width * 1.1f),
        topLeft = rect.topLeft, size = rect.size, cornerRadius = corner,
    )
    // Grain.
    val stroke = Stroke(g.unit * 0.025f)
    for (path in g.grain) drawPath(path, Palette.grain, style = stroke)
    // Bevelled edge: a light inner line on top/left, dark on bottom/right.
    val inset = g.unit * 0.12f
    drawRoundRect(Color.White.copy(alpha = 0.10f), topLeft = rect.topLeft + Offset(inset, inset), size = Size(rect.width - 2 * inset, rect.height - 2 * inset), cornerRadius = CornerRadius(corner.x - inset), style = Stroke(g.unit * 0.05f))
    drawRoundRect(Color.Black.copy(alpha = 0.35f), topLeft = rect.topLeft, size = rect.size, cornerRadius = corner, style = Stroke(g.unit * 0.06f))
}

/** A bevelled hole: dark at the bottom, with a sliver of light catching its lower lip. */
private fun DrawScope.drawHole(centre: Offset, radius: Float) {
    drawCircle(Palette.holeLip, radius * 1.08f, centre + Offset(0f, radius * 0.10f))
    drawCircle(
        Brush.radialGradient(listOf(Palette.holeDeep, Palette.holeDeep, Palette.holeShadow), center = centre + Offset(radius * 0.15f, radius * 0.2f), radius = radius * 1.15f),
        radius, centre,
    )
    drawCircle(Color.Black.copy(alpha = 0.5f), radius, centre - Offset(0f, radius * 0.08f), style = Stroke(radius * 0.12f))
}

/**
 * A turned marble peg seen from above, in the chosen finish. [lift] of 1 raises it one peg-radius off the board (bigger, with a
 * longer, softer shadow); [ring] marks the peg the player is holding.
 */
fun DrawScope.drawPeg(centre: Offset, radius: Float, marble: Marble, lift: Float, alpha: Float = 1f, ring: Boolean = false) {
    if (radius <= 0f) return
    val r = radius * (1f + 0.15f * lift)
    val shadowOffset = Offset(radius * (0.12f + 0.45f * lift), radius * (0.18f + 0.6f * lift))
    drawCircle(Color.Black.copy(alpha = 0.38f * alpha * (1f - 0.3f * lift)), r * (1f + 0.25f * lift), centre + shadowOffset)
    // Dark rim, then the domed body with a highlight up and to the left.
    drawCircle(marble.dark.copy(alpha = alpha), r, centre)
    drawCircle(
        Brush.radialGradient(
            listOf(marble.light.copy(alpha = alpha), marble.base.copy(alpha = alpha), marble.dark.copy(alpha = alpha)),
            center = centre - Offset(r * 0.3f, r * 0.35f), radius = r * 1.2f,
        ),
        r * 0.88f, centre,
    )
    drawCircle(Color.White.copy(alpha = 0.65f * alpha), r * 0.18f, centre - Offset(r * 0.32f, r * 0.38f))
    if (ring) drawCircle(marble.light.copy(alpha = 0.95f), r * 1.08f, centre, style = Stroke(r * 0.12f))
}

/** Maps hole grid units to pixels so the board fits the canvas, square and centred. */
private class Geometry(width: Float, height: Float) {
    /** Distance between neighbouring holes. The board is GRID holes plus a hole of margin on each side. */
    val unit: Float = min(width, height) / (Board.GRID + 1.2f)
    private val side = unit * (Board.GRID + 1.2f)
    private val originX = (width - side) / 2f + unit * 1.1f
    private val originY = (height - side) / 2f + unit * 1.1f
    val boardRect = Rect(Offset((width - side) / 2f, (height - side) / 2f), Size(side, side))

    fun centre(i: Int): Offset {
        val cell = Board.cells[i]
        return Offset(originX + cell.col * unit, originY + cell.row * unit)
    }

    /** Wavy grain lines across the slab; the same seed gives the same wood every time. */
    val grain: List<Path> = run {
        val random = Random(7)
        val lines = ArrayList<Path>()
        var y = boardRect.top + unit * 0.2f
        while (y < boardRect.bottom) {
            val path = Path()
            val amp = unit * (0.05f + random.nextFloat() * 0.12f)
            val freq = 1.5f + random.nextFloat() * 2.5f
            val phase = random.nextFloat() * 6.28f
            var x = boardRect.left + unit * 0.15f
            var first = true
            while (x < boardRect.right - unit * 0.15f) {
                val yy = y + amp * sin(phase + freq * (x - boardRect.left) / side * 6.28f)
                if (first) { path.moveTo(x, yy); first = false } else path.lineTo(x, yy)
                x += unit * 0.1f
            }
            lines.add(path)
            y += unit * (0.12f + random.nextFloat() * 0.25f)
        }
        lines
    }
}
