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
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

internal fun Modifier.pointerClick(enabled: Boolean, source: MutableInteractionSource, role: Role = Role.Button, onClick: () -> Unit) =
    focusProperties { canFocus = false }.clickable(source, indication = null, enabled = enabled, role = role, onClick = onClick)

internal fun Modifier.plate(
    feedback: Feedback, fill: Color = Color.Black, edge: Color = Color(0xff333333),
    round: Boolean = true, pattern: Boolean = true, expand: Boolean = true, highlightFill: Boolean = true,
    enabled: Boolean = true, input: Boolean = false, button: Boolean = false,
): Modifier = onGloballyPositioned { feedback.visible = !it.boundsInWindow().isEmpty }.drawWithCache {
    val radius = if (round) size.minDimension / 2 else 6.dp.toPx()
    val faceInset = (if (input || !enabled) 4 else 5).dp.toPx()
    val face = roundedPath(Rect(Offset.Zero,size).deflate(faceInset), radius - faceInset)
    val checks = checkerPaint(mix(fill, Color.White, if (!enabled && input) .02f else .06f))
    onDrawBehind {
        if(button) {
            drawPath(roundedPath(Rect(Offset.Zero,size),radius),Color.Black)
            buttonShell(fill,edge,round,pattern)
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
    size: ZenlessSize = ZenlessSize.Default,
    variant: ZenlessButtonVariant = ZenlessButtonVariant.Filled,
    enabled: Boolean = true,
    loading: Boolean = false,
    selected: Boolean = false,
    round: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val feedback = rememberFeedback(source, false, enabled && !loading, ambient = selected)
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
    val height = when (size) { ZenlessSize.Mini -> 30; ZenlessSize.Small -> 34; ZenlessSize.Default -> 40; ZenlessSize.Large -> 46; ZenlessSize.Extra -> 52 }
    val padding = when (size) { ZenlessSize.Mini -> 17; ZenlessSize.Small -> 23; ZenlessSize.Default -> 29; ZenlessSize.Large -> 47; ZenlessSize.Extra -> 59 }
    val font = when (size) { ZenlessSize.Mini, ZenlessSize.Small -> 12; ZenlessSize.Default -> 14; ZenlessSize.Large -> 16; ZenlessSize.Extra -> 18 }
    Box(modifier.heightIn(min = height.dp).semantics { this.selected = selected; if (loading) progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }
        .plate(feedback, fill, edge, round, enabled = enabled && !loading,button=true).drawWithCache { onDrawBehind {
            if(selected && enabled && !loading && !feedback.active)drawPath(roundedPath(Rect(Offset.Zero,this.size).deflate(2.5.dp.toPx()),(if(round)this.size.minDimension/2 else 6.dp.toPx())-2.5.dp.toPx()),mix(Palette.signal,Color(0xff91bc00),Motion.signalColor(feedback.seconds)),style=Stroke(3.dp.toPx()))
        }}.pointerClick(enabled && !loading, source, onClick = onClick),
        contentAlignment = Alignment.Center) {
        CompositionLocalProvider(LocalInk provides ink, LocalButtonContent provides true, LocalTextStyle provides LocalTextStyle.current.copy(
            fontSize = if(leadingIcon!=null)(height*.46f).sp else font.sp, letterSpacing = if(leadingIcon!=null)0.sp else 1.sp,
            fontWeight=if(leadingIcon!=null)FontWeight.Bold else FontWeight.Normal)) {
            Row(Modifier.graphicsLayer { alpha = if (loading) 0f else 1f }.padding(start=if(leadingIcon!=null)height.dp else padding.dp,end=if(leadingIcon!=null)(height/3f).dp else padding.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,content=content)
            if(leadingIcon!=null) Box(Modifier.matchParentSize().graphicsLayer { alpha=if(loading)0f else 1f },contentAlignment=Alignment.CenterStart) {
                val badge=when(tone) { ZenlessTone.Neutral->Color.White;ZenlessTone.Danger->Color(0xffff2b00);ZenlessTone.Warning->Color(0xffffb000);else->semantic }
                Box(Modifier.fillMaxHeight().aspectRatio(58.5f/58f).drawWithCache {
                    val bounds=Rect(.5.dp.toPx(),.5.dp.toPx(),this.size.width-.5.dp.toPx(),this.size.height)
                    val inset=this.size.minDimension*.0835f
                    val face=Path().apply { addOval(bounds.deflate(inset)) }
                    val rings=listOf(.6f to .12f,.3f to .32f,0f to .7f,-.3f to 1f).map { (spread,alpha) ->
                        val offset=spread.dp.toPx()
                        Path().apply { fillType=PathFillType.EvenOdd;addOval(bounds.inflate(offset));addOval(bounds.deflate(inset+offset)) } to alpha
                    }
                    onDrawBehind {
                        val rest=1-feedback.highlight
                        // The interior half-ring is flat; the button body owns the exterior bevel.
                        if(rest>0f) {
                            drawContext.canvas.saveLayer(Rect(Offset.Zero,this.size),Paint().apply { alpha=rest })
                            if(variant!=ZenlessButtonVariant.Filled)clipPath(face) { drawRect(Color.Black);buttonTexture() }
                            clipRect(left=this.size.width/2,top=bounds.top+inset,bottom=bounds.bottom-inset) {
                                rings.forEach { (path,alpha) -> drawPath(path,edge.copy(alpha=alpha)) }
                            }
                            drawContext.canvas.restore()
                        }
                        drawCircle(mix(if(enabled)badge else mix(Color.Black,badge,.353f),Color.Black,feedback.highlight),radius=this.size.minDimension*.28f)
                    }
                },contentAlignment=Alignment.Center) {
                    CompositionLocalProvider(LocalInk provides mix(Color.Black,mix(Color.Yellow,Color(0xff80c800),Motion.color(feedback.seconds)),feedback.highlight)) { leadingIcon() }
                }
            }
            if (loading) ZenlessSpinner(Modifier.matchParentSize())
        }
    }
}

@Composable
public fun ZenlessIconButton(onClick: () -> Unit, contentDescription: String, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val feedback = rememberFeedback(source, false, enabled)
    Box(modifier.sizeIn(minWidth = 40.dp, minHeight = 40.dp).semantics { this.contentDescription = contentDescription }
        .plate(feedback,edge=Color(0xff262626),enabled=enabled,button=true).pointerClick(enabled, source, onClick = onClick).padding(9.dp), contentAlignment = Alignment.Center) {
        CompositionLocalProvider(LocalInk provides mix(if(enabled)Color.White else Color(0xff565657),Color.Black,feedback.highlight),LocalButtonContent provides true) { content() }
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
        val inner = if(back)backPlatePath(rect,face=true) else headerPath(rect.deflate(5.dp.toPx()),false)
        val glint = if(back)headerPath(rect,true,-1.dp.toPx()) else null
        onDrawBehind {
            val color = if (back) Color(0xffc50600) else Color(0xffc90000)
            drawPath(path, color)
            if(back) {
                clipPath(inner) {
                    scale(size.width/90*.0801f,size.height/60*.0801f,pivot=Offset.Zero) {
                        translate(4.31f,4.37f) { drawContext.canvas.drawRect(Rect(-5f,-5f,1125f,750f),backCheckerPaint) }
                    }
                }
                drawPath(glint!!,Brush.linearGradient(
                    0f to Color(0xffff2001),.45f to Color(0xffff1c01),1f to color,
                    start=Offset.Zero,end=Offset(size.width*.28f,size.height*.63f)),style=Stroke(1.3.dp.toPx()))
            } else {
                drawPath(inner,mix(color,Color.Black,.25f))
                drawPath(inner,Color.Black,style=Stroke(1.6.dp.toPx()))
            }
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
                drawPath(backArrowPath(Rect(Offset.Zero,size)),mix(Color(0xffc50600),Color.Black,feedback.highlight))
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
