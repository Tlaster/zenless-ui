package moe.tlaster.zenlessui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A caller-sized feed item. [coverAspectRatio] is width / height of the reserved cover area.
 * All slots are display-only; [onClick] is the card's single action. The caller supplies image
 * loading, crop/alignment, avatar artwork/background, text limits and accessibility descriptions.
 * [label] and [status] supply content inside the built-in blue and dark capsules; null hides them.
 * Selection, press and input focus breathe in color only. Hover does not highlight the card.
 */
@Composable
public fun ZenlessFeedCard(
    onClick: () -> Unit,
    coverAspectRatio: Float,
    cover: @Composable BoxScope.() -> Unit,
    avatar: @Composable BoxScope.() -> Unit,
    author: @Composable () -> Unit,
    title: @Composable () -> Unit,
    summary: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    size: ZenlessSize = LocalControlSize.current,
    label: (@Composable RowScope.() -> Unit)? = null,
    status: (@Composable RowScope.() -> Unit)? = null,
) {
    require(coverAspectRatio.isFinite() && coverAspectRatio > 0f) { "coverAspectRatio must be finite and positive" }
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    val feedback = rememberFeedback(source, selected || pressed || focused, enabled)
    val scale = size.scale
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val outer = RoundedCornerShape(topStart = (36*scale).dp, topEnd = (36*scale).dp, bottomStart = (36*scale).dp)
    val inner = RoundedCornerShape(topStart = (31.25f*scale).dp, topEnd = (31.25f*scale).dp, bottomStart = (31.25f*scale).dp)
    val panel = Color(0xff181818)
    CompositionLocalProvider(LocalControlSize provides size) {
        Box(modifier
            .graphicsLayer { alpha = if (enabled) 1f else .5f }
            .onGloballyPositioned { feedback.visible = !it.boundsInWindow().isEmpty }
            .clip(outer)
            .drawWithCache { onDrawBehind {
                drawRect(if (feedback.active) mix(Palette.signal, Color(0xff91bc00), Motion.signalColor(feedback.seconds)) else Color.Black)
            } }
            .semantics(mergeDescendants = true) { this.selected = selected }
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(start = (4.5f*scale).dp, end = (5*scale).dp, top = (5*scale).dp, bottom = (5*scale).dp)) {
            // Calibrated subpixel placement preserves the captured face's edge coverage.
            Column(Modifier.fillMaxWidth().graphicsLayer { translationX = (if (rtl) -.2f else .2f)*scale*density; translationY = -.12f*scale*density }.clip(inner).background(panel)) {
                Box(Modifier.fillMaxWidth().aspectRatio(coverAspectRatio).clip(androidx.compose.ui.graphics.RectangleShape)) {
                    Box(Modifier.matchParentSize(), content = cover)
                    if (label != null) FeedCapsule(true, scale,
                        Modifier.align(Alignment.TopStart).padding(start = (13.5f*scale).dp, end = (13.5f*scale).dp, top = (9.5f*scale).dp), label)
                    if (status != null) FeedCapsule(false, scale,
                        Modifier.align(Alignment.BottomEnd).padding(start = (13.5f*scale).dp, end = (9*scale).dp, bottom = (9*scale).dp), status)
                }
                Box(Modifier.fillMaxWidth().drawWithCache {
                    val unit = scale.dp.toPx()
                    val crest = Path().apply {
                        moveTo(0f, 0f)
                        cubicTo(10*unit, 0f, 11.4f*unit, -1.6f*unit, 14.414f*unit, -7.811f*unit)
                        arcTo(Rect(13*unit, -34.25f*unit, 86*unit, 38.75f*unit), 196f, 148f, false)
                        cubicTo(87.6f*unit, -1.6f*unit, 89*unit, 0f, 99*unit, 0f)
                        close()
                    }
                    if (layoutDirection == LayoutDirection.Rtl) crest.transform(Matrix().apply { translate(this@drawWithCache.size.width, 0f); scale(-1f, 1f) })
                    onDrawBehind { drawPath(crest, panel) }
                }) {
                    Box(Modifier.matchParentSize().wrapContentSize(Alignment.TopStart, unbounded = true)) {
                        Box(Modifier.offset(x = (16.5f*scale).dp, y = (-30.75f*scale).dp).size((66*scale).dp).clip(CircleShape), content = avatar)
                    }
                    Column(Modifier.fillMaxWidth().padding(start = (89.25f*scale).dp, end = (16.25f*scale).dp, top = (4*scale).dp)) {
                        FeedText(Color(0xff545454), 20f*scale, 24f*scale) {
                            Box(Modifier.fillMaxWidth().heightIn(min = (30.5f*scale).dp), contentAlignment = Alignment.CenterStart) { author() }
                        }
                        Box(Modifier.fillMaxWidth().height((3*scale).dp).background(Color(0xff2a2a2a), CircleShape))
                    }
                }
                Column(Modifier.fillMaxWidth().padding(start = (22*scale).dp, end = (16.25f*scale).dp, top = (11*scale).dp, bottom = (11.5f*scale).dp),
                    verticalArrangement = Arrangement.spacedBy((4*scale).dp)) {
                    FeedText(Color.White, 20f*scale, 28f*scale) { title() }
                    FeedText(Color(0xffabaca9), 17f*scale, 24f*scale) { summary() }
                }
            }
        }
    }
}

@Composable
private fun FeedText(color: Color, fontSize: Float, lineHeight: Float, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalInk provides color, LocalTextStyle provides LocalTextStyle.current.copy(
        fontSize = fontSize.sp, lineHeight = lineHeight.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp), content = content)
}

@Composable
private fun FeedCapsule(blue: Boolean, scale: Float, modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.heightIn(min = (32.5f*scale).dp).drawWithCache {
        val unit = scale.dp.toPx()
        val bounds = Rect(0f, .25f*unit, size.width, size.height-.5f*unit)
        val shape = roundedPath(bounds, bounds.height/2)
        val fill = if (blue) Brush.verticalGradient(
            0f to Color(0xff00d6ff), .035f to Color(0xff00dbff), .1f to Color(0xff00c4ff),
            .24f to Color(0xff00aeff), .4f to Color(0xff0092ff), .56f to Color(0xff007bff),
            .72f to Color(0xff006aff), .9f to Color(0xff0058ff), .96f to Color(0xff0045ff), 1f to Color(0xff0038c5),
            startY = bounds.top, endY = bounds.bottom)
        else Brush.verticalGradient(listOf(Color(0xff272e2c), Color(0xff272d2c)))
        onDrawBehind {
            for (i in 3 downTo 1) drawPath(roundedPath(bounds.translate(0f, i*.5f*unit).inflate(i*.25f*unit), bounds.height/2), Color.Black.copy(alpha = .10f))
            drawPath(shape, fill)
            if (!blue) drawPath(roundedPath(bounds.deflate(.6f*unit), bounds.height/2),
                Brush.verticalGradient(listOf(Color(0xff444644), Color(0xff171a19))), style = Stroke(1.2f*unit))
        }
    }.padding(horizontal = (12*scale).dp, vertical = (3*scale).dp),
        horizontalArrangement = Arrangement.spacedBy((6*scale).dp), verticalAlignment = Alignment.CenterVertically) {
        FeedText(Color.White, 18f*scale, 24f*scale) { content() }
    }
}
