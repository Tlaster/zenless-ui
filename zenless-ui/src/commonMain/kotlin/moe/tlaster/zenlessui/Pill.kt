package moe.tlaster.zenlessui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * An opaque pill shell with caller-owned, display-only content and selection.
 * A null [onClick] makes it display-only. Press, input focus and [selected] share a
 * color-only two-second rim cycle; disabling preserves the shell and stops input/animation.
 * Height is a preset minimum; width follows content or [modifier]. Leave 3 dp at Default
 * (proportional to the preset) outside the shell for its rim. Parent clipping can cut it off.
 */
@Composable
public fun ZenlessPillContainer(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    size: ZenlessSize = LocalControlSize.current,
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    val feedback = rememberFeedback(source, selected || (onClick != null && (pressed || focused)), enabled)
    val scale = size.scale
    val unit = with(LocalDensity.current) { scale.dp.toPx() }
    val shape = remember(unit) { GenericShape { dimensions, _ -> addPath(pillPath(Rect(Offset.Zero, dimensions), unit)) } }
    CompositionLocalProvider(LocalControlSize provides size,
        LocalTextStyle provides LocalTextStyle.current.copy(fontSize = size.fontSize.sp)) {
        Row(modifier.heightIn(min = size.height.dp).widthIn(min = size.height.dp)
            .onGloballyPositioned { feedback.visible = !it.boundsInWindow().isEmpty }
            .drawWithCache {
                val bounds = Rect(Offset.Zero, this.size)
                val radius = bounds.minDimension / 2
                val outer = pillPath(bounds, unit)
                val inset = (2.3f*unit).coerceAtMost(radius)
                val faceBounds = bounds.deflate(inset)
                val face = pillPath(faceBounds, unit)
                // The reference's straight rim is cropped independently of its soft end caps.
                val rimBounds = Rect(-3*unit, -3.25f*unit, bounds.right+3*unit, bounds.bottom+3.25f*unit)
                val rimRadius = rimBounds.minDimension/2
                val rim = Path().apply { addRoundRect(RoundRect(rimBounds, CornerRadius((rimRadius-1.2f*unit).coerceAtLeast(0f), rimRadius))) }
                val edge = Brush.verticalGradient(
                    0f to Color(0xff2d2c2e), .5f to Color(0xff141314), 1f to Color(0xff0e0e0f),
                    startY = faceBounds.top, endY = faceBounds.bottom)
                val fill = Brush.verticalGradient(
                    0f to Color(0xff171817), .2f to Color(0xff171717), .42f to Color(0xff131213),
                    .56f to Color(0xff0e0f0e), .7f to Color(0xff0e0e0f), 1f to Color(0xff0b0b0b),
                    startY = faceBounds.top, endY = faceBounds.bottom)
                val inner = listOf(1.65f to .2f, 2.1f to .45f, 2.65f to 1f).map { (d, alpha) ->
                    val distance = (d*unit).coerceAtMost(faceBounds.minDimension/2)
                    pillPath(faceBounds.deflate(distance), unit) to alpha
                }
                onDrawBehind {
                    if (feedback.active) clipRect(top = -3*unit, bottom = bounds.bottom+3*unit, left = -3*unit, right = bounds.right+3*unit) {
                        drawPath(rim, mix(Palette.signal, Color(0xff91bc00), Motion.signalColor(feedback.seconds)))
                    }
                    drawPath(outer, Color.Black)
                    drawPath(face, edge)
                    inner.forEach { (path, alpha) -> drawPath(path, fill, alpha) }
                }
            }
            .semantics(mergeDescendants = true) { this.selected = selected }
            .then(if (onClick != null) Modifier.clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick) else Modifier)
            .clip(shape)
            .padding(horizontal = (18*scale).dp, vertical = (6*scale).dp),
            verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

// Fitted cap tangents preserve the reference shell's contour scanlines.
private fun pillPath(bounds: Rect, unit: Float) = Path().apply {
    val ry = bounds.minDimension/2
    val rx = (ry + .7f*unit).coerceAtMost(bounds.width/2)
    val k = .54f
    val l = bounds.left; val t = bounds.top; val r = bounds.right; val b = bounds.bottom
    moveTo(l+rx, t)
    lineTo(r-rx, t)
    cubicTo(r-rx+rx*k, t, r, t+ry-ry*k, r, t+ry)
    lineTo(r, b-ry)
    cubicTo(r, b-ry+ry*k, r-rx+rx*k, b, r-rx, b)
    lineTo(l+rx, b)
    cubicTo(l+rx-rx*k, b, l, b-ry+ry*k, l, b-ry)
    lineTo(l, t+ry)
    cubicTo(l, t+ry-ry*k, l+rx-rx*k, t, l+rx, t)
    close()
}
