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
    enabled: Boolean = true, input: Boolean = false,
): Modifier = onGloballyPositioned { feedback.visible = !it.boundsInWindow().isEmpty }.drawWithCache {
    val radius = if (round) size.minDimension / 2 else 6.dp.toPx()
    val faceInset = (if (input || !enabled) 4 else 5).dp.toPx()
    val face = roundedPath(Rect(Offset.Zero, size).deflate(faceInset), radius - faceInset)
    val checks = checkerPaint(mix(fill, Color.White, if (!enabled && input) .02f else .06f))
    onDrawBehind {
        layeredPlate(fill, edge, radius, faceInset, !input && enabled)
        if (pattern) clipPath(face) {
            translate(faceInset, faceInset) { checker(checks,size.width-2*faceInset,size.height-2*faceInset) }
        }
        val amount = feedback.highlight
        if (amount > 0f) {
            val outset = if (expand && feedback.release < 0) min(size.width, size.height) * .15f * Motion.pulse(feedback.seconds) else 0f
            val color = mix(Color.Yellow, Color(0xff80c800), Motion.color(feedback.seconds)).copy(alpha = amount)
            val rect = Rect(Offset.Zero, size).inflate(2.dp.toPx() + outset)
            if (highlightFill) drawPath(roundedPath(rect, radius + 2.dp.toPx() + outset), color)
            else drawPath(roundedPath(Rect(Offset.Zero, size).deflate(1.dp.toPx()), radius - 1.dp.toPx()), color, style = Stroke(2.dp.toPx()))
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
    val feedback = rememberFeedback(source, false, enabled && !loading, ambient = tone == ZenlessTone.Accent || selected)
    val semantic = Palette.tone(tone)
    val colored = tone !in listOf(ZenlessTone.Neutral, ZenlessTone.Accent)
    val fill = when {
        !enabled || loading -> if (variant == ZenlessButtonVariant.Plain) Color(0xff999999) else if (variant == ZenlessButtonVariant.Hollow) Color(0xff737373) else Color.Black
        variant == ZenlessButtonVariant.Hollow -> Color.Black
        variant == ZenlessButtonVariant.Plain -> when (tone) { ZenlessTone.Primary -> Color(0xff99d1ff); ZenlessTone.Success -> Color(0xff99eb9e); ZenlessTone.Info -> Color(0xffebebeb); ZenlessTone.Warning -> Color(0xffffe799); ZenlessTone.Danger -> Color(0xffe6a499); else -> Color(0xff999999) }
        colored -> semantic
        else -> Color.Black
    }
    val edge = if(selected && enabled)Palette.signal else if (colored) semantic else Color(0xff333333)
    val baseInk = when {
        !enabled -> if (variant == ZenlessButtonVariant.Filled) Color(0xff666666) else Color(0xff2e2e2e)
        loading -> Color(0xffb0b0b0)
        variant == ZenlessButtonVariant.Plain -> Color.Black
        variant == ZenlessButtonVariant.Hollow -> Color.White
        tone == ZenlessTone.Accent -> mix(Palette.signal, Color(0xff91bc00), Motion.signalColor(feedback.seconds))
        tone == ZenlessTone.Primary -> Color.White
        colored -> Color.Black
        else -> Color.White
    }
    val ink = mix(baseInk, Color.Black, feedback.highlight)
    val height = when (size) { ZenlessSize.Mini -> 30; ZenlessSize.Small -> 34; ZenlessSize.Default -> 40; ZenlessSize.Large -> 46; ZenlessSize.Extra -> 52 }
    val padding = when (size) { ZenlessSize.Mini -> 17; ZenlessSize.Small -> 23; ZenlessSize.Default -> 29; ZenlessSize.Large -> 47; ZenlessSize.Extra -> 59 }
    val font = when (size) { ZenlessSize.Mini, ZenlessSize.Small -> 12; ZenlessSize.Default -> 14; ZenlessSize.Large -> 16; ZenlessSize.Extra -> 18 }
    Box(modifier.heightIn(min = height.dp).semantics { this.selected = selected; if (loading) progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }
        .plate(feedback, fill, edge, round, enabled = enabled && !loading).drawWithCache { onDrawBehind {
            if(selected && enabled && !loading && !feedback.active)drawPath(roundedPath(Rect(Offset.Zero,this.size).deflate(2.5.dp.toPx()),(if(round)this.size.minDimension/2 else 6.dp.toPx())-2.5.dp.toPx()),mix(Palette.signal,Color(0xff91bc00),Motion.signalColor(feedback.seconds)),style=Stroke(3.dp.toPx()))
        }}.pointerClick(enabled && !loading, source, onClick = onClick).padding(horizontal = padding.dp),
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
    Box(modifier.sizeIn(minWidth = 40.dp, minHeight = 40.dp).semantics { this.contentDescription = contentDescription }
        .plate(feedback, enabled = enabled).pointerClick(enabled, source, onClick = onClick).padding(9.dp), contentAlignment = Alignment.Center) {
        CompositionLocalProvider(LocalInk provides mix(if(enabled)Color.White else Palette.muted,Color.Black,feedback.highlight)) { content() }
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
    Box(modifier.size(90.dp, 60.dp).semantics { contentDescription = description }.drawWithCache {
        val rect = Rect(Offset.Zero, size)
        val path = headerPath(rect, back)
        val inner = headerPath(rect.deflate(5.dp.toPx()), back)
        onDrawBehind {
            val color = if (back) Color(0xffe51c00) else Color(0xffc90000)
            drawPath(path, color)
            drawPath(inner, if (back) Color(0xff171717) else mix(color,Color.Black,.25f))
            drawPath(inner, Color.Black, style = Stroke(1.6.dp.toPx()))
            if (feedback.highlight > 0f) {
                val outset = if (feedback.release < 0) size.minDimension * .15f * Motion.pulse(feedback.seconds) else 0f
                drawPath(headerPath(rect.inflate(2.dp.toPx()), back, outset), mix(Color.Yellow,Color(0xff80c800),Motion.color(feedback.seconds)).copy(alpha=feedback.highlight))
            }
        }
    }.pointerClick(enabled, source) {
        if (!pending) { pending = true; scope.launch { delay(150); pending = false; if (currentEnabled) currentClick() } }
    }) {
        Canvas(Modifier.fillMaxSize()) {
            if (back) {
                val p=Path().apply {
                    fun x(v:Float)=size.width*(27+v*35)/90
                    fun y(v:Float)=size.height*(15+v*30)/60
                    moveTo(x(.28f),y(.3f));lineTo(x(.65f),y(.3f))
                    cubicTo(x(1f),y(.3f),x(1f),y(.83f),x(.65f),y(.83f));lineTo(x(.42f),y(.83f))
                }
                drawPath(p,Color(1f,.08f,0f),style=Stroke(size.height*.095f,cap=StrokeCap.Butt))
                val head=Path().apply { moveTo(size.width*29.1f/90,size.height*24/60);lineTo(size.width*41.7f/90,size.height*16.2f/60);lineTo(size.width*41.7f/90,size.height*31.8f/60);close() }
                drawPath(head,Color(1f,.08f,0f))
            } else {
                val start=Offset(size.width*30.9f/90,size.height*20.9f/60)
                val end=Offset(size.width*49.1f/90,size.height*39.1f/60)
                drawLine(Color.Black,start,end,size.width*26*.23f/90,StrokeCap.Round)
                drawLine(Color.Black,Offset(end.x,start.y),Offset(start.x,end.y),size.width*26*.23f/90,StrokeCap.Round)
            }
        }
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
        if(kind==Mark.Down) {
            p.reset();p.moveTo(size.width*.15f,size.height*.32f);p.lineTo(size.width*.85f,size.height*.32f);p.lineTo(size.width*.5f,size.height*.7f);p.close()
            drawPath(p,color)
        } else drawPath(p, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
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
