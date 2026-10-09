package moe.tlaster.zenlessui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlin.math.roundToInt

internal class OverlayContext {
    val blurs = mutableStateMapOf<Any, Float>()
    val activeModals = mutableStateMapOf<Any, Boolean>()
    val modals = mutableStateMapOf<Any, @Composable () -> Unit>()
    val popups = mutableStateMapOf<Any, @Composable () -> Unit>()
    var bounds by mutableStateOf(IntRect.Zero)
    var tooltip by mutableStateOf<Any?>(null)
}
internal val LocalOverlays = compositionLocalOf<OverlayContext?> { null }
@Composable internal expect fun ModalBackHandler(enabled: Boolean, onBack: () -> Unit)

/** Wrap the app once to supply live background blur and exclusive tooltips. */
@Composable
public fun ZenlessOverlayHost(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val context = remember { OverlayContext() }
    val density = LocalDensity.current
    CompositionLocalProvider(LocalOverlays provides context) {
        Box(modifier.fillMaxSize().onGloballyPositioned { context.bounds = it.boundsInWindow().roundToIntRect() }.pointerInput(context) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                context.tooltip = null
            }
        }) {
            Box(Modifier.matchParentSize().graphicsLayer {
                val strength = context.blurs.values.maxOrNull() ?: 0f
                renderEffect = if (strength > 0f) BlurEffect(with(density) { 4.5.dp.toPx() } * strength, with(density) { 4.5.dp.toPx() } * strength, TileMode.Clamp) else null
            }.then(if (context.activeModals.values.any { it }) Modifier.clearAndSetSemantics {} else Modifier)) { content() }
            context.modals.forEach { (id, overlay) -> key(id) { overlay() } }
            context.popups.forEach { (id, overlay) -> key(id) { overlay() } }
        }
    }
}

internal fun popupOffset(anchor: IntRect, popup: IntSize, window: IntSize, margin: Int): IntOffset {
    val x = anchor.left.coerceIn(margin, (window.width - popup.width - margin).coerceAtLeast(margin))
    val below = anchor.bottom + margin
    val y = if (below + popup.height <= window.height - margin) below else (anchor.top - popup.height - margin).coerceAtLeast(margin)
    return IntOffset(x, y)
}
@Composable
private fun AnchoredOverlay(anchor: IntRect, margin: Int, blockOutside: Boolean, onDismiss: () -> Unit, decoration: @Composable BoxScope.() -> Unit = {}, content: @Composable (Boolean) -> Unit) {
    val context = checkNotNull(LocalOverlays.current) { "Select and tooltip require ZenlessOverlayHost" }
    val id = remember { Any() }
    val density = LocalDensity.current
    val overlay by rememberUpdatedState<@Composable () -> Unit> {
        var measured by remember { mutableStateOf(IntSize.Zero) }
        ModalBackHandler(blockOutside, onDismiss)
        val above=anchor.bottom-context.bounds.top+margin+measured.height > context.bounds.height-margin
        val availableHeight=maxOf(anchor.top-context.bounds.top,context.bounds.bottom-anchor.bottom)-margin*2
        Box(Modifier.fillMaxSize().then(if (blockOutside) Modifier.pointerInput(onDismiss) { detectTapGestures { onDismiss() } } else Modifier)) {
            decoration()
            Box(Modifier.offset {
                val origin = context.bounds.topLeft
                popupOffset(anchor.translate(-origin), measured, context.bounds.size, margin)
            }.widthIn(max = with(density) { (context.bounds.width - margin * 2).coerceAtLeast(1).toDp() })
                .heightIn(max = with(density) { availableHeight.coerceIn(1,(context.bounds.height-margin*2).coerceAtLeast(1)).toDp() })
                .onSizeChanged { measured = it }
                .then(if (blockOutside) Modifier.pointerInput(Unit) { detectTapGestures {} } else Modifier)) { content(above) }
        }
    }
    DisposableEffect(context) {
        context.popups[id] = { overlay() }
        onDispose { context.popups.remove(id) }
    }
}

@Composable
public fun ZenlessSelect(
    options: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, label: String = "",
) {
    require(options.isNotEmpty() && selectedIndex in options.indices)
    var expanded by remember { mutableStateOf(false) }
    var width by remember { mutableIntStateOf(0) }
    var anchor by remember { mutableStateOf(IntRect.Zero) }
    val progress = remember { Animatable(0f) }
    val density = LocalDensity.current
    val source = remember { MutableInteractionSource() }
    val feedback = rememberFeedback(source, false, enabled, ambient = expanded)
    val overlayContext=LocalOverlays.current
    LaunchedEffect(expanded, enabled) {
        if (!enabled) expanded = false
        progress.animateTo(if (expanded && enabled) 1f else 0f, tween(if (expanded) 200 else 100, easing = if (expanded) EaseOutCubic else EaseInCubic))
    }
    val opacity=remember { Animatable(0f) }
    LaunchedEffect(expanded) { opacity.animateTo(if(expanded)1f else 0f,tween(if(expanded)60 else 100,easing=if(expanded)EaseOutCubic else EaseInCubic)) }
    FieldLayout(label,modifier) {
        Row(Modifier.fillMaxWidth().heightIn(min=62.dp).onGloballyPositioned { width=it.size.width; anchor=it.boundsInWindow().roundToIntRect() }
            .semantics { if(label.isNotEmpty())contentDescription=label;stateDescription=options[selectedIndex] }
            .plate(feedback,fill=if(enabled)Color.Black else Color(0xff080808),edge=if(enabled)Color(0xff323232) else Color(0xff191919),enabled=enabled,input=true)
            .pointerClick(enabled,source) { expanded=!expanded }.padding(start=17.dp,end=12.dp),verticalAlignment=Alignment.CenterVertically) {
            CompositionLocalProvider(LocalInk provides mix(if(enabled)Color.White else Color(0xff737373),Color.Black,feedback.highlight),LocalTextStyle provides LocalTextStyle.current.copy(fontSize=24.sp)) {
                ZenlessText(options[selectedIndex],Modifier.weight(1f),maxLines=1)
                Mark(Mark.Down,Modifier.padding(start=7.dp).size(24.dp))
            }
        }
    }
    if(expanded || progress.value>0f) AnchoredOverlay(anchor,with(density){4.dp.roundToPx()},true,{expanded=false},decoration={
        Canvas(Modifier.offset { val origin=overlayContext?.bounds?.topLeft ?: IntOffset.Zero; IntOffset(anchor.left-origin.x,anchor.top-origin.y) }.size(with(density){anchor.width.toDp()},with(density){anchor.height.toDp()})) {
            val rim=2+6*Motion.pulse(feedback.seconds)
            for(i in 0..6) {
                val width=if(i==0)2f else 1f
                val outset=(if(i==0)1f else i+1.5f).dp.toPx()
                val alpha=if(i==0)1f else (rim-i-1).coerceIn(0f,1f)
                drawPath(roundedPath(Rect(Offset.Zero,size).inflate(outset),size.minDimension/2+outset),mix(Color.Yellow,Color(0xff80c800),Motion.color(feedback.seconds)).copy(alpha=alpha),style=Stroke(width.dp.toPx()))
            }
        }
    }) { above ->
        val scroll=rememberScrollState()
        LaunchedEffect(Unit) { scroll.scrollTo(with(density){(selectedIndex*68).dp.roundToPx()}) }
        Column(Modifier.width(with(density){width.toDp()}).heightIn(max=368.dp).graphicsLayer { alpha=opacity.value;translationY=(if(above)32 else -32).dp.toPx()*(1-progress.value) },horizontalAlignment=Alignment.CenterHorizontally) {
            Column(Modifier.weight(1f,fill=false).fillMaxWidth().heightIn(max=344.dp).background(Color(0xff262626),RoundedCornerShape(36.dp)).border(2.dp,Color(0xff141414),RoundedCornerShape(36.dp)).padding(4.dp).clip(RoundedCornerShape(32.dp)).verticalScroll(scroll),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                options.forEachIndexed { index,text ->
                    val optionSource=remember { MutableInteractionSource() }
                    val optionFeedback=rememberFeedback(optionSource,index==selectedIndex,true)
                    Box(Modifier.fillMaxWidth().heightIn(min=64.dp).drawWithCache { onDrawBehind {
                        if(optionFeedback.highlight>0f)drawRoundRect(mix(Palette.signal,Color(0xff91bc00),Motion.signalColor(optionFeedback.seconds)).copy(alpha=optionFeedback.highlight),cornerRadius=CornerRadius(size.height/2))
                    }}.semantics { selected=index==selectedIndex }.pointerClick(expanded,optionSource) { expanded=false;if(index!=selectedIndex)onSelected(index) }.padding(horizontal=16.dp),contentAlignment=Alignment.Center) {
                        CompositionLocalProvider(LocalInk provides mix(if(index==selectedIndex)Color.Black else Color.White,Color.Black,optionFeedback.highlight),LocalTextStyle provides LocalTextStyle.current.copy(fontSize=24.sp)) { ZenlessText(text,maxLines=1) }
                    }
                }
            }
            if(scroll.canScrollForward)Mark(Mark.Down,Modifier.padding(top=4.dp).size(40.dp,20.dp))
        }
    }
}

@Composable
public fun ZenlessTooltip(text: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val context = checkNotNull(LocalOverlays.current) { "ZenlessTooltip requires ZenlessOverlayHost" }
    val id = remember { Any() }
    var hovered by remember { mutableStateOf(false) }
    var anchor by remember { mutableStateOf(IntRect.Zero) }
    val visible = context.tooltip === id
    val density = LocalDensity.current
    LaunchedEffect(hovered) { if (hovered) { delay(600); context.tooltip = id } else if (context.tooltip === id) context.tooltip = null }
    DisposableEffect(Unit) { onDispose { if (context.tooltip === id) context.tooltip = null } }
    Box(modifier.onGloballyPositioned {
        val next = it.boundsInWindow().roundToIntRect()
        if (anchor != next && context.tooltip === id) context.tooltip = null
        anchor = next
    }.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                when (event.type) {
                    PointerEventType.Enter -> if (event.changes.any { it.type == PointerType.Mouse }) hovered = true
                    PointerEventType.Exit -> hovered = false
                    PointerEventType.Scroll -> { hovered = false; if (context.tooltip === id) context.tooltip = null }
                }
            }
        }
    }.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (down.type != PointerType.Mouse) {
                val longPress = awaitLongPressOrCancellation(down.id)
                if (longPress != null) {
                    context.tooltip = id
                    do { val event = awaitPointerEvent(PointerEventPass.Initial); event.changes.forEach { it.consume() } } while (event.changes.any { it.pressed })
                }
            }
        }
    }) {
        content()
        if (visible) AnchoredOverlay(anchor, with(density) { 8.dp.roundToPx() }, false, { context.tooltip = null }) {
            Box(Modifier.widthIn(max = 280.dp).background(Color.Black, RoundedCornerShape(10.dp)).border(1.dp, Palette.line, RoundedCornerShape(10.dp)).padding(12.dp).semantics { liveRegion = LiveRegionMode.Polite }) { ZenlessText(text) }
        }
    }
}

@Composable
private fun Modal(visible: Boolean, onDismiss: (() -> Unit)?, drawer: Boolean, content: @Composable (ModalFrame, Boolean, ((() -> Unit) -> Unit)) -> Unit) {
    val context = checkNotNull(LocalOverlays.current) { "Dialogs and drawers require ZenlessOverlayHost" }
    val id = remember { Any() }
    val progress = remember { Animatable(0f) }
    val scrim = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val latestDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(visible, closing) {
        val opening = visible && !closing
        val target = if (opening) 1f else 0f
        coroutineScope {
            if (drawer) launch { scrim.animateTo(target, tween(if (opening) 180 else 220,easing=Motion.smoothEasing)) }
            progress.animateTo(target, tween(if (!opening) 220 else if (drawer) 280 else 250, easing = if (!drawer) LinearEasing else if (opening) EaseOutCubic else EaseInCubic))
        }
        if (!visible) { pending = null; closing = false }
        else if (closing) { val action = pending; pending = null; action?.invoke() }
    }
    val ready = progress.value == 1f && !closing
    val close: (() -> Unit) -> Unit = { action -> if (ready || drawer && visible && !closing) { pending = action; closing = true } }
    val frame = if (drawer) ModalFrame(scrim.value, progress.value, 1f, 1f, 0f) else Motion.modal(progress.value, closing || !visible)
    SideEffect {
        context.blurs[id] = if (drawer) 0f else frame.effect
        context.activeModals[id] = visible || progress.value > 0f
        if (visible) context.tooltip = null
    }
    val overlay by rememberUpdatedState<@Composable () -> Unit> {
        val shown = visible && !closing || progress.value > 0f
        ModalBackHandler(shown) { latestDismiss?.let(close) }
        if (shown) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = (if (drawer) .4f else .58f) * frame.effect)).drawBehind {
            if (!drawer) {
                val step = 7.dp.toPx()
                for (i in 0..((size.width + size.height) / step).toInt()) {
                    val x = i * step - size.height
                    drawLine(Color(0xff171717).copy(alpha = frame.effect * .55f), Offset(x, 0f), Offset(x + size.height, size.height), 2.dp.toPx())
                }
            }
        }.pointerInput(ready, onDismiss, drawer) {
                detectTapGestures { if (drawer) latestDismiss?.let(close) }
            }) { content(frame, ready, close) }
    }
    // Keep modal content in the same scene so Web semantics are restored after dismissal.
    DisposableEffect(context) {
        context.modals[id] = { overlay() }
        onDispose { context.blurs.remove(id); context.activeModals.remove(id); context.modals.remove(id) }
    }
}

/** A full-width confirmation band. Outside clicks never confirm or dismiss it. */
@Composable
public fun ZenlessAlert(
    visible: Boolean, message: String, confirmText: String, onConfirm: () -> Unit,
    onDismissRequest: (() -> Unit)? = null, cancelText: String? = null,
) {
    Modal(visible, onDismissRequest, false) { frame, ready, close ->
        val currentConfirm by rememberUpdatedState(onConfirm)
        val density = LocalDensity.current
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment=Alignment.Center) {
            val narrow=maxWidth<600.dp
            val actionWidth=if(narrow)(maxWidth-50.dp)/2 else 272.dp
            val messageWidth=(maxWidth*.8f).coerceAtMost(1100.dp)
            Box(Modifier.fillMaxWidth().heightIn(min=186.dp,max=(maxHeight-100.dp).coerceAtLeast(186.dp)).pointerInput(Unit){detectTapGestures{}}) {
                Canvas(Modifier.matchParentSize().graphicsLayer { scaleY=frame.band.coerceAtLeast(.001f);alpha=frame.band;clip=true }) {
                    drawRect(Color.Black)
                    val w=size.width;val h=size.height
                    val art=Path().apply { moveTo(-30.dp.toPx(),-40.dp.toPx());lineTo(w*.22f,-90.dp.toPx());lineTo(w*.25f,h*.65f);lineTo(0f,h*1.3f);close() }
                    drawPath(art,Color(.035f,.035f,.035f,frame.artwork))
                    val cut=Path().apply { moveTo(0f,h*.3f);cubicTo(w*.1f,h*.8f,w*.14f,-h,w*.24f,-20.dp.toPx()) }
                    drawPath(cut,Color.Black,style=Stroke(12.dp.toPx()))
                    val right=Path().apply { moveTo(w*.76f,h);lineTo(w,h*.32f);lineTo(w,h*1.2f);close() }
                    drawPath(right,Color(.035f,.035f,.035f,frame.artwork))
                    val step=3.dp.toPx()
                    for(i in 0..((w+h)/step).toInt()) { val x=i*step-h;drawLine(Color(.12f,.12f,.12f,.12f*frame.artwork),Offset(x,0f),Offset(x+h,h),1.dp.toPx()) }
                    drawRect(Color(0xff343434),size=Size(w,2.dp.toPx()))
                    drawRect(Color(0xff343434),topLeft=Offset(0f,h-2.dp.toPx()),size=Size(w,2.dp.toPx()))
                    drawRect(Color(0xff999999).copy(alpha=frame.flash))
                }
                Box(Modifier.align(Alignment.TopCenter).width(messageWidth).padding(top=58.dp,bottom=92.dp).verticalScroll(rememberScrollState()).graphicsLayer { alpha=frame.ink;translationY=(-9).dp.toPx()*(1-frame.ink) }) {
                    CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontSize=26.sp,lineHeight=36.sp,textAlign=TextAlign.Center)) { ZenlessText(message,Modifier.fillMaxWidth()) }
                }
                Row(Modifier.align(Alignment.BottomCenter).offset(y=28.dp).graphicsLayer { alpha=frame.ink;translationY=(-9).dp.toPx()*(1-frame.ink) },horizontalArrangement=Arrangement.spacedBy(26.dp)) {
                    if(cancelText!=null && onDismissRequest!=null)AlertAction(cancelText,false,ready,actionWidth,narrow) {close(onDismissRequest)}
                    AlertAction(confirmText,true,ready,actionWidth,narrow) {close {currentConfirm()}}
                }
            }
        }
    }
}

@Composable
private fun AlertAction(text:String,confirm:Boolean,enabled:Boolean,width:Dp,narrow:Boolean,onClick:()->Unit) {
    val source=remember { MutableInteractionSource() }
    val feedback=rememberFeedback(source,false,enabled)
    Box(Modifier.width(width).heightIn(min=56.dp).plate(feedback).pointerClick(enabled,source,onClick=onClick),contentAlignment=Alignment.Center) {
        CompositionLocalProvider(LocalInk provides mix(Color.White,Color.Black,feedback.highlight),LocalTextStyle provides LocalTextStyle.current.copy(fontSize=if(narrow)16.sp else 23.sp,letterSpacing=0.sp,textAlign=TextAlign.Center)) {
            ZenlessText(text,Modifier.padding(start=58.dp,end=32.dp),maxLines=1)
        }
        Box(Modifier.align(Alignment.CenterStart).padding(start=2.dp).size(50.dp).drawWithCache { onDrawBehind {
            drawCircle(Color(0xff060606).copy(alpha=1-feedback.highlight))
            drawCircle(Color(0xff303030).copy(alpha=1-feedback.highlight),radius=size.width/2-2.dp.toPx(),style=Stroke(4.dp.toPx()))
            drawCircle(mix(if(confirm)Color(0xff00d127) else Color(0xffec1000),Color.Black,feedback.highlight),16.dp.toPx())
        }},contentAlignment=Alignment.Center) { Mark(if(confirm)Mark.Check else Mark.Close,Modifier.size(26.dp),mix(Color.Black,mix(Color.Yellow,Color(0xff80c800),Motion.color(feedback.seconds)),feedback.highlight)) }
    }
}

@Composable
public fun ZenlessDrawer(
    visible: Boolean, title: String, closeDescription: String, onDismissRequest: () -> Unit,
    footer: @Composable RowScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit,
) {
    Modal(visible, onDismissRequest, true) { frame, ready, close ->
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.CenterEnd) {
            val narrow=maxWidth<420.dp
            Column(Modifier.width(if(maxWidth<600.dp)maxWidth else (maxWidth*.46f).coerceAtMost(680.dp)).fillMaxHeight().graphicsLayer {translationX=size.width*(1-frame.band)}
                .background(Color.Black).padding(start=3.dp).pointerInput(Unit){detectTapGestures{}}) {
                Box(Modifier.fillMaxWidth().height(96.dp)) {
                    Box(Modifier.fillMaxSize().padding(start=40.dp,end=if(narrow)130.dp else 180.dp,top=24.dp),contentAlignment=Alignment.CenterStart) {
                        CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontSize=26.sp)) { ZenlessText(title,maxLines=1) }
                    }
                    ZenlessCloseButton({close(onDismissRequest)},closeDescription,Modifier.align(Alignment.TopEnd).padding(top=20.dp,end=if(narrow)24.dp else 66.dp),enabled=ready)
                }
                Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xff1a1a1a)).drawWithCache { val checks=checkerPaint(Color(0xff202020));onDrawBehind {checker(checks)} }.padding(horizontal=34.dp,vertical=24.dp)) {
                    val scroll=rememberScrollState()
                    Row(Modifier.fillMaxSize().background(Color(0xff050505),RoundedCornerShape(18.dp)).padding(12.dp)) {
                        Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(6.dp)).verticalScroll(scroll).padding(18.dp),verticalArrangement=Arrangement.spacedBy(18.dp),content=content)
                        if(scroll.maxValue>0)DrawerScrollbar(scroll)
                    }
                }
                Row(Modifier.fillMaxWidth().background(Color.Black).padding(start=34.dp,end=34.dp,top=24.dp,bottom=24.dp),horizontalArrangement=Arrangement.spacedBy(16.dp),content=footer)
            }
        }
    }
}

@Composable
private fun DrawerScrollbar(scroll: ScrollState) {
    val scope=rememberCoroutineScope()
    BoxWithConstraints(Modifier.width(10.dp).fillMaxHeight()) {
        val height=with(LocalDensity.current){maxHeight.toPx()}
        val thumb=(height*scroll.viewportSize/(scroll.viewportSize+scroll.maxValue).coerceAtLeast(1))
            .coerceIn(with(LocalDensity.current){30.dp.toPx()}.coerceAtMost(height),height)
        val travel=(height-thumb).coerceAtLeast(1f)
        Canvas(Modifier.fillMaxSize().semantics { contentDescription="Scroll" }
            .pointerInput(travel,thumb) { detectTapGestures { point -> scope.launch { scroll.scrollTo(((point.y-thumb/2)/travel*scroll.maxValue).roundToInt().coerceIn(0,scroll.maxValue)) } } }
            .pointerInput(travel) { detectVerticalDragGestures { change,delta -> change.consume();scroll.dispatchRawDelta(delta/travel*scroll.maxValue) } }) {
            drawRect(Color(0xff303030),Offset(4.dp.toPx(),0f),Size(2.dp.toPx(),height))
            drawRoundRect(Color(0xff777777),Offset(1.dp.toPx(),travel*scroll.value/scroll.maxValue.coerceAtLeast(1)),Size(8.dp.toPx(),thumb),CornerRadius(4.dp.toPx()))
        }
    }
}
