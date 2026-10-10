package moe.tlaster.zenlessui

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot

/** A bounded, decorative viewport. The single content instance supplies one repeating tile.
 * Content owns its dimensions and spacing; cells have a 48 dp minimum repeat pitch.
 * The background owns tilt, drift and opacity. Content is composed once and is decorative.
 */
@Composable
public fun ZenlessAnimatedBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val tile = rememberGraphicsLayer()
    var tileSize by remember { mutableStateOf(IntSize.Zero) }
    var visible by remember { mutableStateOf(false) }
    val seconds = remember { mutableDoubleStateOf(0.0) }
    val density = LocalDensity.current
    LaunchedEffect(visible && tileSize.width > 0 && tileSize.height > 0) {
        if (!visible || tileSize.width == 0 || tileSize.height == 0) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) withFrameNanos { now -> seconds.doubleValue += (now - last) / 1_000_000_000.0; last = now }
    }
    Layout(
        modifier = modifier.fillMaxSize().clipToBounds().clearAndSetSemantics {}
            .focusProperties { canFocus = false; onEnter = { cancelFocusChange() } }.focusGroup()
            .onGloballyPositioned { visible = !it.boundsInWindow().isEmpty }
            .pointerInput(Unit) {
                awaitPointerEventScope { while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() } }
            }.drawWithCache {
                val dots = dialogDotPath(size)
                onDrawWithContent {
                    drawRect(Color(0xff171717))
                    drawPath(dots, Color.Black.copy(alpha = .3f))
                    // Record one composition, never instantiate a content lambda for each copy.
                    val contentScope = this
                    tile.record(size = tileSize) { contentScope.drawContent() }
                    if (tileSize.width > 0 && tileSize.height > 0) {
                        tile.alpha = .065f
                        tile.clip = true
                        // Tiny caller artwork must not produce millions of draw calls.
                        val w = tileSize.width.toFloat().coerceAtLeast(48.dp.toPx())
                        val h = tileSize.height.toFloat().coerceAtLeast(48.dp.toPx())
                        val extent = hypot(size.width, size.height) / 2
                        val dx = -(seconds.doubleValue * 22 * density.density % w).toFloat()
                        val dy = (seconds.doubleValue * 8 * density.density % (2 * h)).toFloat()
                        withTransform({ translate(center.x, center.y); rotate(-15f, Offset.Zero) }) {
                            for (row in floor((-extent - dy) / h).toInt()..ceil((extent - dy) / h).toInt()) {
                                val stagger = if (row % 2 == 0) 0f else w / 2
                                for (column in floor((-extent - dx - stagger) / w).toInt()..ceil((extent - dx - stagger) / w).toInt()) {
                                    translate(column * w + dx + stagger, row * h + dy) { drawLayer(tile) }
                                }
                            }
                        }
                    }
                }
            },
        content = {
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1f)) {
                Box { content() }
            }
        },
    ) { measurables, constraints ->
        require(constraints.hasBoundedWidth && constraints.hasBoundedHeight) { "ZenlessAnimatedBackground requires a bounded viewport" }
        val child = measurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0))
        tileSize = IntSize(child.width, child.height)
        layout(constraints.maxWidth, constraints.maxHeight) { child.place(0, 0) }
    }
}

internal fun Density.dialogDotPath(size: Size): Path = Path().apply {
    val step = 8.dp.toPx()
    val rowStep = 7.25.dp.toPx()
    for (row in -1..ceil(size.height / rowStep).toInt()) {
        val shift = if (row % 2 == 0) 0f else step / 2
        for (column in -1..ceil(size.width / step).toInt()) {
            val x = column * step + shift + 1.dp.toPx()
            val y = row * rowStep + 1.125.dp.toPx()
            addOval(Rect(x, y, x + 3.5.dp.toPx(), y + 5.dp.toPx()))
        }
    }
}
