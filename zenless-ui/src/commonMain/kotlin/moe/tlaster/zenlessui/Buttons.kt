package moe.tlaster.zenlessui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

internal fun Modifier.pointerClick(enabled: Boolean, source: MutableInteractionSource, role: Role = Role.Button, onClick: () -> Unit) =
    focusProperties { canFocus = false }.clickable(source, indication = null, enabled = enabled, role = role, onClick = onClick)

internal fun Modifier.plate(
    feedback: Feedback, fill: Color = Color.Black, edge: Color = Color(0xff333333),
    round: Boolean = true, pattern: Boolean = true, expand: Boolean = true, highlightFill: Boolean = true,
): Modifier = onGloballyPositioned { feedback.visible = !it.boundsInWindow().isEmpty }.drawWithCache {
    val radius = if (round) size.height / 2 else 6.dp.toPx()
    val clip = Path().apply { addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(radius))) }
    onDrawBehind {
        val releasing = feedback.release >= 0
        val amount = if (releasing) Motion.release(feedback.release) else if (feedback.active) 1f else 0f
        if (amount > 0) {
            val outset = if (expand && !releasing) min(size.width, size.height) * .15f * Motion.pulse(feedback.seconds) else 0f
            drawRoundRect(lerp(Color.Yellow, Color(0xff80c800), Motion.color(feedback.seconds)).copy(alpha = amount),
                Offset(-outset - 2.dp.toPx(), -outset - 2.dp.toPx()),
                Size(size.width + outset * 2 + 4.dp.toPx(), size.height + outset * 2 + 4.dp.toPx()), CornerRadius(radius + outset))
        }
        drawRoundRect(Color.Black, cornerRadius = CornerRadius(radius))
        inset(1.dp.toPx()) { drawRoundRect(edge, cornerRadius = CornerRadius((radius - 1.dp.toPx()).coerceAtLeast(0f))) }
        inset(4.dp.toPx()) { drawRoundRect(Color.Black, cornerRadius = CornerRadius((radius - 4.dp.toPx()).coerceAtLeast(0f))) }
        inset(5.dp.toPx()) { drawRoundRect(fill, cornerRadius = CornerRadius((radius - 5.dp.toPx()).coerceAtLeast(0f))) }
        if (pattern) clipPath(clip) {
            val step = 3.dp.toPx()
            for (y in 0..(size.height / step).toInt()) for (x in 0..(size.width / step).toInt()) {
                if ((x + y) % 2 == 0) drawRect(Color.White.copy(alpha = .025f), Offset(x * step, y * step), Size(step, step))
            }
            drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = .07f), Color.Transparent)), size = Size(size.width, size.height / 2))
        }
        if (highlightFill && amount > 0) inset(5.dp.toPx()) {
            drawRoundRect(lerp(Color.Yellow, Color(0xff80c800), Motion.color(feedback.seconds)).copy(alpha = amount), cornerRadius = CornerRadius((radius - 5.dp.toPx()).coerceAtLeast(0f)))
        }
    }
}

/** A fixed-palette button. Supply business icons through [content]. */
@Composable
public fun ZenlessButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ZenlessTone = ZenlessTone.Neutral,
    size: ZenlessSize = ZenlessSize.Default,
    variant: ZenlessButtonVariant = ZenlessButtonVariant.Filled,
    enabled: Boolean = true,
    loading: Boolean = false,
    selected: Boolean = false,
    round: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val feedback = rememberFeedback(source, selected, enabled && !loading)
    val semantic = Palette.tone(tone)
    val colored = tone !in listOf(ZenlessTone.Neutral, ZenlessTone.Accent)
    val fill = when {
        !enabled || loading -> if (variant == ZenlessButtonVariant.Plain) Color(0xff999999) else if (variant == ZenlessButtonVariant.Hollow) Color(0xff737373) else Color.Black
        variant == ZenlessButtonVariant.Hollow -> Color.Black
        variant == ZenlessButtonVariant.Plain -> if (colored) lerp(semantic, Color.White, .6f) else Color(0xff999999)
        colored -> semantic
        else -> Color.Black
    }
    val edge = if (colored) semantic else Color(0xff333333)
    val ink = when {
        !enabled -> Color(0xff666666)
        loading -> Color(0xffb0b0b0)
        feedback.highlight > .5f -> Color.Black
        tone == ZenlessTone.Accent -> Palette.signal
        variant == ZenlessButtonVariant.Plain || colored && variant != ZenlessButtonVariant.Hollow -> Color.Black
        else -> Color.White
    }
    val height = when (size) { ZenlessSize.Mini -> 30; ZenlessSize.Small -> 34; ZenlessSize.Default -> 40; ZenlessSize.Large -> 46; ZenlessSize.Extra -> 52 }
    val padding = when (size) { ZenlessSize.Mini -> 17; ZenlessSize.Small -> 23; ZenlessSize.Default -> 29; ZenlessSize.Large -> 47; ZenlessSize.Extra -> 59 }
    val font = when (size) { ZenlessSize.Mini, ZenlessSize.Small -> 12; ZenlessSize.Default -> 14; ZenlessSize.Large -> 16; ZenlessSize.Extra -> 18 }
    Box(modifier.heightIn(min = height.dp).semantics { this.selected = selected; if (loading) progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }
        .plate(feedback, fill, edge, round).pointerClick(enabled && !loading, source, onClick = onClick).padding(horizontal = padding.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center) {
        CompositionLocalProvider(LocalInk provides ink, LocalTextStyle provides LocalTextStyle.current.copy(fontSize = font.sp, letterSpacing = 1.sp)) {
            Row(Modifier.graphicsLayer { alpha = if (loading) 0f else 1f }, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically, content = content)
            if (loading) ZenlessSpinner(Modifier.matchParentSize())
        }
    }
}

@Composable
public fun ZenlessIconButton(onClick: () -> Unit, contentDescription: String, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val feedback = rememberFeedback(source, false, enabled)
    Box(modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp).semantics { this.contentDescription = contentDescription }
        .plate(feedback).pointerClick(enabled, source, onClick = onClick).padding(10.dp), contentAlignment = Alignment.Center) {
        CompositionLocalProvider(LocalInk provides if (feedback.highlight > .5f) Color.Black else if (enabled) Color.White else Palette.muted) { content() }
    }
}

/** Uses the shared release flash before invoking navigation. */
@Composable
public fun ZenlessBackButton(onClick: () -> Unit, contentDescription: String, modifier: Modifier = Modifier, enabled: Boolean = true) {
    NavigationButton(onClick, contentDescription, modifier, enabled, back = true)
}
@Composable
public fun ZenlessCloseButton(onClick: () -> Unit, contentDescription: String, modifier: Modifier = Modifier, enabled: Boolean = true) {
    NavigationButton(onClick, contentDescription, modifier, enabled, back = false)
}

@Composable
private fun NavigationButton(onClick: () -> Unit, description: String, modifier: Modifier, enabled: Boolean, back: Boolean) {
    val scope = rememberCoroutineScope()
    val currentClick by rememberUpdatedState(onClick)
    val currentEnabled by rememberUpdatedState(enabled)
    var pending by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    val feedback = rememberFeedback(source, false, enabled)
    Box(modifier.size(68.dp, 48.dp).semantics { contentDescription = description }.drawWithCache {
        val path = Path().apply {
            moveTo(size.width * .24f, 1f); lineTo(size.width * .92f, 1f)
            cubicTo(size.width, 1f, size.width, size.height * .15f, size.width * .96f, size.height * .3f)
            lineTo(size.width * .78f, size.height * .92f)
            quadraticTo(size.width * .75f, size.height, size.width * .62f, size.height)
            lineTo(size.width * .1f, size.height)
            cubicTo(-size.width * .08f, size.height, 0f, size.height * .3f, size.width * .24f, 1f); close()
        }
        onDrawBehind {
            val alpha = if (feedback.release >= 0) Motion.release(feedback.release) else if (feedback.active) 1f else 0f
            val scale = 1 + if (feedback.active && feedback.release < 0) .15f * Motion.pulse(feedback.seconds) else 0f
            scale(scale) { drawPath(path, if (alpha > 0) lerp(Color(0xff353535), lerp(Color.Yellow, Color(0xff80c800), Motion.color(feedback.seconds)), alpha) else Color(0xff353535)); drawPath(path, Color.Black, style = Stroke(2.dp.toPx())) }
        }
    }.pointerClick(enabled, source) {
        if (!pending) { pending = true; scope.launch { delay(150); pending = false; if (currentEnabled) currentClick() } }
    }, contentAlignment = Alignment.Center) {
        Mark(if (back) Mark.Back else Mark.Close, Modifier.size(24.dp), if (feedback.highlight > .5f) Color.Black else if (enabled) Color.White else Palette.muted)
    }
}

internal enum class Mark { Check, Minus, Close, Back, Down }
@Composable
internal fun Mark(kind: Mark, modifier: Modifier, color: Color = LocalInk.current) {
    Canvas(modifier) {
        val p = Path()
        when (kind) {
            Mark.Check -> { p.moveTo(size.width * .2f, size.height * .5f); p.lineTo(size.width * .43f, size.height * .72f); p.lineTo(size.width * .8f, size.height * .28f) }
            Mark.Minus -> { p.moveTo(size.width * .2f, size.height * .5f); p.lineTo(size.width * .8f, size.height * .5f) }
            Mark.Close -> { p.moveTo(size.width * .25f, size.height * .25f); p.lineTo(size.width * .75f, size.height * .75f); p.moveTo(size.width * .75f, size.height * .25f); p.lineTo(size.width * .25f, size.height * .75f) }
            Mark.Back -> { p.moveTo(size.width * .65f, size.height * .2f); p.lineTo(size.width * .3f, size.height * .5f); p.lineTo(size.width * .65f, size.height * .8f) }
            Mark.Down -> { p.moveTo(size.width * .2f, size.height * .35f); p.lineTo(size.width * .5f, size.height * .65f); p.lineTo(size.width * .8f, size.height * .35f) }
        }
        drawPath(p, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
public fun ZenlessSpinner(modifier: Modifier = Modifier) {
    var angle by remember { mutableFloatStateOf(0f) }
    val ink = LocalInk.current
    LaunchedEffect(Unit) { val start = withFrameNanos { it }; while (true) withFrameNanos { angle = ((it - start) / 1_000_000_000f * 240) % 360 } }
    Canvas(modifier.size(24.dp).semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }) {
        val diameter = min(size.minDimension, 20.dp.toPx())
        drawArc(ink, angle, 270f, false, topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2), size = Size(diameter, diameter), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    }
}
