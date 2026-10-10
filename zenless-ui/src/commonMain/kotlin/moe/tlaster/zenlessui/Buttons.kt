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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

internal fun Modifier.pointerClick(enabled: Boolean, source: MutableInteractionSource, role: Role = Role.Button, onClick: () -> Unit) =
    focusProperties { canFocus = false }.clickable(source, indication = null, enabled = enabled, role = role, onClick = onClick)

internal fun Modifier.plate(
    feedback: Feedback, fill: Color = Color.Black, edge: Color = Color(0xff333333),
    round: Boolean = true, pattern: Boolean = true, expand: Boolean = true, highlightFill: Boolean = true,
    enabled: Boolean = true, input: Boolean = false, button: Boolean = false, leading: Boolean = false,
): Modifier = onGloballyPositioned { feedback.visible = !it.boundsInWindow().isEmpty }.drawWithCache {
    val radius = if (round) size.minDimension / 2 else 6.dp.toPx()
    val faceInset = (if (input || !enabled) 4 else 5).dp.toPx()
    val face = roundedPath(Rect(Offset.Zero,size).deflate(faceInset), radius - faceInset)
    val checks = checkerPaint(mix(fill, Color.White, if (!enabled && input) .02f else .06f))
    val buttonDrawing=if(button)buttonShell(fill,edge,round,pattern,leading) else null
    onDrawBehind {
        if(button) {
            drawPath(roundedPath(Rect(Offset.Zero,size).inflate(2.dp.toPx()),radius+2.dp.toPx()),Color.Black)
            buttonDrawing!!()
        } else {
            layeredPlate(fill, edge, radius, faceInset, !input && enabled)
            if(pattern)clipPath(face) { translate(faceInset,faceInset) { checker(checks,size.width-2*faceInset,size.height-2*faceInset) } }
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

/** A fixed-palette button. Supply business icons through [leadingIcon] or [content]. */
@Composable
public fun ZenlessButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ZenlessTone = ZenlessTone.Neutral,
    size: ZenlessSize = LocalControlSize.current,
    variant: ZenlessButtonVariant = ZenlessButtonVariant.Filled,
    enabled: Boolean = true,
    loading: Boolean = false,
    selected: Boolean = false,
    round: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val feedback = rememberFeedback(source, false, enabled && !loading, ambient = selected, buttonFeedback = true)
    val semantic = Palette.tone(tone)
    val badgeButton = leadingIcon!=null && variant==ZenlessButtonVariant.Filled
    val colored = !badgeButton && tone !in listOf(ZenlessTone.Neutral, ZenlessTone.Accent)
    val fill = when {
        !enabled || loading -> if (variant == ZenlessButtonVariant.Plain) Color(0xff999999) else if (variant == ZenlessButtonVariant.Hollow) Color(0xff737373) else Color.Black
        variant == ZenlessButtonVariant.Hollow -> Color.Black
        variant == ZenlessButtonVariant.Plain -> when (tone) { ZenlessTone.Primary -> Color(0xff99d1ff); ZenlessTone.Success -> Color(0xff99eb9e); ZenlessTone.Info -> Color(0xffebebeb); ZenlessTone.Warning -> Color(0xffffe799); ZenlessTone.Danger -> Color(0xffe6a499); else -> Color(0xff999999) }
        colored -> semantic
        else -> Color.Black
    }
    val edge = Color(0xff262626)
    val baseInk = if(enabled)Color.White else Color(0xff565657)
    val ink = mix(baseInk, Color.Black, feedback.highlight)
    val height = size.height
    val padding = size.horizontalPadding * 1.7f
    val font = size.fontSize
    Box(modifier.heightIn(min = height.dp).semantics { this.selected = selected; if (loading) progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }
        .plate(feedback, fill, edge, round, enabled = enabled && !loading,button=true,leading=leadingIcon!=null && !loading).drawWithCache { onDrawBehind {
            if(selected && enabled && !loading && !feedback.active)drawPath(roundedPath(Rect(Offset.Zero,this.size).deflate(2.5.dp.toPx()),(if(round)this.size.minDimension/2 else 6.dp.toPx())-2.5.dp.toPx()),mix(Palette.signal,Color(0xff91bc00),Motion.signalColor(feedback.seconds)),style=Stroke(3.dp.toPx()))
        }}.buttonClick(enabled && !loading, source, onClick = onClick),
        contentAlignment = Alignment.Center, propagateMinConstraints = true) {
        CompositionLocalProvider(LocalInk provides ink, LocalButtonContent provides true, LocalTextStyle provides LocalTextStyle.current.copy(
            fontSize = font.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Normal)) {
            Row(Modifier.graphicsLayer { alpha = if (loading) 0f else 1f }.layout { measurable, constraints ->
                val contentStart = if(leadingIcon!=null)(constraints.minHeight * 58.5f / 58f).roundToInt() else 0
                val contentEnd = if(leadingIcon!=null)constraints.minHeight/2 else 0
                val sidePadding = if(leadingIcon!=null)(height*12f/52f).dp.roundToPx() else padding.dp.roundToPx()
                val body = measurable.measure(constraints.offset(horizontal = -contentStart - contentEnd - 2*sidePadding))
                layout(body.width + contentStart + contentEnd + 2*sidePadding, body.height) { body.placeRelative(contentStart + sidePadding, 0) }
            }, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,content=content)
            if(leadingIcon!=null) Box(Modifier.matchParentSize().graphicsLayer { alpha=if(loading)0f else 1f },contentAlignment=Alignment.CenterStart) {
                val badge=when(tone) { ZenlessTone.Neutral->Color.White;ZenlessTone.Danger->Color(0xffff2b00);ZenlessTone.Warning->Color(0xffffb000);else->semantic }
                Box(Modifier.fillMaxHeight().aspectRatio(58.5f/58f).drawWithCache {
                    onDrawBehind {
                        drawCircle(mix(if(enabled)badge else mix(Color.Black,badge,.353f),Color.Black,feedback.highlight),radius=this.size.minDimension*.28f)
                    }
                },contentAlignment=Alignment.Center) {
                    CompositionLocalProvider(LocalInk provides mix(Color.Black,mix(Color.Yellow,Color(0xff80c800),Motion.color(feedback.seconds)),feedback.highlight)) {
                        Box(Modifier.size(size.iconSize.dp),contentAlignment=Alignment.Center) { leadingIcon() }
                    }
                }
            }
            if (loading) ZenlessSpinner(Modifier.matchParentSize(), size)
        }
    }
}

@Composable
public fun ZenlessIconButton(onClick: () -> Unit, contentDescription: String, modifier: Modifier = Modifier, enabled: Boolean = true, size: ZenlessSize = LocalControlSize.current, content: @Composable () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val feedback = rememberFeedback(source, false, enabled, buttonFeedback = true)
    Box(modifier.sizeIn(minWidth = size.height.dp, minHeight = size.height.dp).semantics { this.contentDescription = contentDescription }
        .plate(feedback,edge=Color(0xff262626),enabled=enabled,button=true).buttonClick(enabled, source, onClick = onClick), contentAlignment = Alignment.Center) {
        CompositionLocalProvider(LocalInk provides mix(if(enabled)Color.White else Color(0xff565657),Color.Black,feedback.highlight),LocalButtonContent provides true,LocalTextStyle provides LocalTextStyle.current.copy(fontSize=size.fontSize.sp)) {
            Box(Modifier.size(size.iconSize.dp),contentAlignment=Alignment.Center) { content() }
        }
    }
}

/** Uses the shared release flash before invoking navigation. */
@Composable
public fun ZenlessBackButton(onClick: () -> Unit, contentDescription: String, modifier: Modifier = Modifier, enabled: Boolean = true, size: ZenlessSize = LocalControlSize.current) {
    NavigationButton(onClick, contentDescription, modifier, enabled, back = true, controlSize = size)
}
@Composable
public fun ZenlessCloseButton(onClick: () -> Unit, contentDescription: String, modifier: Modifier = Modifier, enabled: Boolean = true, size: ZenlessSize = LocalControlSize.current) {
    NavigationButton(onClick, contentDescription, modifier, enabled, back = false, controlSize = size)
}

@Composable
internal fun NavigationButton(onClick: () -> Unit, description: String, modifier: Modifier, enabled: Boolean, back: Boolean, controlSize: ZenlessSize, clickDelayMillis: Long = 150) {
    val scope = rememberCoroutineScope()
    val currentClick by rememberUpdatedState(onClick)
    val currentEnabled by rememberUpdatedState(enabled)
    var pending by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    val feedback = rememberFeedback(source, false, enabled, buttonFeedback = true)
    Box(modifier.size((controlSize.height * 1.5f).dp, controlSize.height.dp).semantics { contentDescription = description }.drawWithCache {
        val unit = size.height / 60f
        val rect = Rect(Offset.Zero, size)
        val path = headerPath(rect, back)
        val inner = if(back)backPlatePath(rect,face=true) else navigationPlatePath(rect,closeFace)
        val ring = if(back)null else navigationPlatePath(rect,closeRing)
        val glint = headerPath(rect,back,-unit)
        onDrawBehind {
            val color = Color(0xffc50600)
            drawPath(path, color)
            if(back) {
                clipPath(inner) {
                    scale(size.width/90*.0801f,size.height/60*.0801f,pivot=Offset.Zero) {
                        translate(4.31f,4.37f) { drawContext.canvas.drawRect(Rect(-5f,-5f,1125f,750f),backCheckerPaint) }
                    }
                }
            } else {
                drawPath(ring!!,Color.Black)
                clipPath(inner) {
                    scale(size.width/90*.08f,size.height/60*.08f,pivot=Offset.Zero) {
                        translate(5.11f,-2.48f) { drawContext.canvas.drawRect(Rect(-6f,-1f,1125f,753f),closeCheckerPaint) }
                    }
                    drawPath(inner,Brush.linearGradient(
                        listOf(Color(0xfffe2002),Color.Transparent),
                        start=Offset(size.width*1.55f/90,size.height*9.73f/60),
                        end=Offset(size.width*3.71f/90,size.height*23.25f/60)),style=Stroke(3*unit))
                }
            }
            val bevel=if(back)Brush.linearGradient(
                0f to Color(0xffff2001),.45f to Color(0xffff1c01),1f to color,
                start=Offset.Zero,end=Offset(size.width*.28f,size.height*.63f)) else Brush.linearGradient(
                listOf(Color(0xfffe1c01),color),
                start=Offset(size.width*8.29f/90,size.height*19.60f/60),
                end=Offset(size.width*14.58f/90,size.height*34.48f/60))
            drawPath(glint,bevel,style=Stroke((if(back)1.3f else 1.6f)*unit))
            if (feedback.highlight > 0f) {
                val outset = if (feedback.release < 0) size.minDimension * .15f * Motion.pulse(feedback.seconds) else 0f
                drawPath(headerPath(rect.inflate(2*unit), back, outset), mix(Color.Yellow,Color(0xff80c800),Motion.color(feedback.seconds)).copy(alpha=feedback.highlight))
            }
        }
    }.buttonClick(enabled, source) {
        if (!pending) { pending = true; scope.launch { delay(clickDelayMillis); pending = false; if (currentEnabled) currentClick() } }
    }) {
        // Soften only the close mark's captured edges, preserving the plate's checker detail.
        Canvas(Modifier.fillMaxSize().then(if(back)Modifier else Modifier.graphicsLayer {
            val radius = size.height / 60f * .35f
            renderEffect = BlurEffect(radius, radius, TileMode.Decal)
        })) {
            if (back) {
                drawPath(backArrowPath(Rect(Offset.Zero,size)),mix(Color(0xffc50600),Color.Black,feedback.highlight))
            } else {
                val start=Offset(size.width*31.117f/90,size.height*20.506f/60)
                val end=Offset(size.width*48.883f/90,size.height*38.272f/60)
                drawLine(Color.Black,start,end,size.width*6.601f/90,StrokeCap.Round)
                drawLine(Color.Black,Offset(end.x,start.y),Offset(start.x,end.y),size.width*6.601f/90,StrokeCap.Round)
            }
        }
    }
}

internal enum class Mark { Check, Minus, Close, Back, Down, Dropdown }
@Composable
internal fun Mark(kind: Mark, modifier: Modifier, color: Color = LocalInk.current) {
    Canvas(modifier) {
        if (kind == Mark.Dropdown) {
            scale(size.width / 16f, size.height / 13f, Offset.Zero) { drawPath(dropdownArrow, color) }
            return@Canvas
        }
        val p = Path()
        when (kind) {
            Mark.Check -> { p.moveTo(size.width * .2f, size.height * .5f); p.lineTo(size.width * .43f, size.height * .72f); p.lineTo(size.width * .8f, size.height * .28f) }
            Mark.Minus -> { p.moveTo(size.width * .2f, size.height * .5f); p.lineTo(size.width * .8f, size.height * .5f) }
            Mark.Close -> { p.moveTo(size.width * .25f, size.height * .25f); p.lineTo(size.width * .75f, size.height * .75f); p.moveTo(size.width * .75f, size.height * .25f); p.lineTo(size.width * .25f, size.height * .75f) }
            Mark.Back -> { p.moveTo(size.width * .65f, size.height * .2f); p.lineTo(size.width * .3f, size.height * .5f); p.lineTo(size.width * .65f, size.height * .8f) }
            Mark.Down -> { p.moveTo(size.width * .2f, size.height * .35f); p.lineTo(size.width * .5f, size.height * .65f); p.lineTo(size.width * .8f, size.height * .35f) }
            Mark.Dropdown -> Unit
        }
        if(kind==Mark.Down) {
            p.reset();p.moveTo(size.width*.15f,size.height*.32f);p.lineTo(size.width*.85f,size.height*.32f);p.lineTo(size.width*.5f,size.height*.7f);p.close()
            drawPath(p,color)
        } else drawPath(p, color, style = Stroke(size.minDimension * 2.5f / 26f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private val dropdownArrow = Path().apply {
    moveTo(2.7f, 0f); lineTo(13.3f, 0f)
    cubicTo(15.3f, 0f, 16.4f, 1.8f, 15.7f, 3.3f)
    lineTo(9.5f, 12.5f); quadraticTo(9.1f, 13f, 8f, 13f)
    quadraticTo(6.9f, 13f, 6.5f, 12.5f); lineTo(.3f, 3.3f)
    cubicTo(-.4f, 1.8f, .7f, 0f, 2.7f, 0f); close()
}

@Composable
public fun ZenlessSpinner(modifier: Modifier = Modifier, size: ZenlessSize = LocalControlSize.current) {
    var angle by remember { mutableFloatStateOf(0f) }
    val ink = LocalInk.current
    LaunchedEffect(Unit) { val start = withFrameNanos { it }; while (true) withFrameNanos { angle = ((it - start) / 1_000_000_000f * 240) % 360 } }
    Canvas(modifier.size((24*size.scale).dp).semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }) {
        val diameter = min(this.size.minDimension, (20*size.scale).dp.toPx())
        drawArc(ink, angle, 270f, false, topLeft = Offset((this.size.width - diameter) / 2, (this.size.height - diameter) / 2), size = Size(diameter, diameter), style = Stroke((2*size.scale).dp.toPx(), cap = StrokeCap.Round))
    }
}
