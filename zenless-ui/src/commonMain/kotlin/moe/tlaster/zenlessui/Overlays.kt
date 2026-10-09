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
import androidx.compose.ui.graphics.drawscope.translate
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
private fun AnchoredOverlay(anchor: IntRect, margin: Int, blockOutside: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val context = checkNotNull(LocalOverlays.current) { "Select and tooltip require ZenlessOverlayHost" }
    val id = remember { Any() }
    val density = LocalDensity.current
    val overlay by rememberUpdatedState<@Composable () -> Unit> {
        var measured by remember { mutableStateOf(IntSize.Zero) }
        ModalBackHandler(blockOutside, onDismiss)
        Box(Modifier.fillMaxSize().then(if (blockOutside) Modifier.pointerInput(onDismiss) { detectTapGestures { onDismiss() } } else Modifier)) {
            Box(Modifier.offset {
                val origin = context.bounds.topLeft
                popupOffset(anchor.translate(-origin), measured, context.bounds.size, margin)
            }.widthIn(max = with(density) { (context.bounds.width - margin * 2).coerceAtLeast(1).toDp() })
                .heightIn(max = with(density) { (context.bounds.height - margin * 2).coerceAtLeast(1).toDp() })
                .onSizeChanged { measured = it }
                .then(if (blockOutside) Modifier.pointerInput(Unit) { detectTapGestures {} } else Modifier)) { content() }
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
    val feedback = rememberFeedback(source, expanded, enabled)
    LaunchedEffect(expanded, enabled) {
        if (!enabled) expanded = false
        progress.animateTo(if (expanded && enabled) 1f else 0f, tween(if (expanded) 200 else 100, easing = if (expanded) EaseOutCubic else EaseInCubic))
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (label.isNotEmpty()) ZenlessText(label)
        Box {
            Row(Modifier.fillMaxWidth().heightIn(min = 62.dp).onGloballyPositioned { width = it.size.width; anchor = it.boundsInWindow().roundToIntRect() }
                .semantics { if (label.isNotEmpty()) contentDescription = label; stateDescription = options[selectedIndex] }
                .plate(feedback).pointerClick(enabled, source) { expanded = !expanded }.padding(horizontal = 22.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                CompositionLocalProvider(LocalInk provides if (feedback.highlight > .5f) Color.Black else if (enabled) Color.White else Palette.muted, LocalTextStyle provides LocalTextStyle.current.copy(fontSize = 24.sp)) {
                    ZenlessText(options[selectedIndex], Modifier.weight(1f), maxLines = 1)
                    Mark(Mark.Down, Modifier.size(20.dp))
                }
            }
            if (expanded || progress.value > 0f) AnchoredOverlay(anchor, with(density) { 4.dp.roundToPx() }, true, { expanded = false }) {
                val scroll = rememberScrollState()
                LaunchedEffect(Unit) { scroll.scrollTo(with(density) { (selectedIndex * 68).dp.roundToPx() }) }
                Column(Modifier.width(with(density) { width.toDp() }).heightIn(max = 320.dp)
                    .graphicsLayer { alpha = progress.value; translationY = -32.dp.toPx() * (1 - progress.value) }
                    .background(Color(0xff262626), RoundedCornerShape(32.dp)).border(2.dp, Color(0xff141414), RoundedCornerShape(32.dp)).clip(RoundedCornerShape(32.dp))
                    .verticalScroll(scroll).padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEachIndexed { index, text ->
                        ZenlessButton({ if (expanded) { expanded = false; if (index != selectedIndex) onSelected(index) } }, Modifier.fillMaxWidth().heightIn(min = 64.dp), selected = index == selectedIndex, enabled = expanded) {
                            CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontSize = 24.sp)) { ZenlessText(text, maxLines = 1) }
                        }
                    }
                }
            }
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
            if (drawer) launch { scrim.animateTo(target, tween(if (opening) 180 else 220)) }
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
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
            val narrow = maxWidth < 600.dp
            Column(Modifier.fillMaxWidth().heightIn(max = maxHeight - 24.dp)
                .pointerInput(Unit) { detectTapGestures {} }, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(min = 186.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.matchParentSize().graphicsLayer { scaleY = frame.band.coerceAtLeast(.001f); alpha = frame.band }) {
                        drawRect(Color.Black)
                        val path = Path().apply { moveTo(0f, 0f); lineTo(size.width * .18f, 0f); lineTo(size.width * .27f, size.height); lineTo(size.width * .06f, size.height); close() }
                        val art = Color(0xff131313).copy(alpha = frame.artwork)
                        drawPath(path, art); translate(size.width * .67f) { drawPath(path, art) }
                        drawLine(Palette.line, Offset.Zero, Offset(size.width, 0f), 2.dp.toPx())
                        drawLine(Palette.line, Offset(0f, size.height), Offset(size.width, size.height), 2.dp.toPx())
                        drawRect(Color(0xff999999).copy(alpha = frame.flash))
                    }
                    Box(Modifier.padding(horizontal = if (narrow) 20.dp else 64.dp, vertical = 48.dp).verticalScroll(rememberScrollState())
                        .graphicsLayer { alpha = frame.ink; translationY = with(density) { (-9).dp.toPx() } * (1 - frame.ink) }) {
                        ZenlessText(message, style = ZenlessTextStyle.Subtitle)
                    }
                }
                FlowRow(Modifier.offset(y = (-28).dp).padding(horizontal = 12.dp).graphicsLayer { alpha = frame.ink }, horizontalArrangement = Arrangement.spacedBy(26.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (cancelText != null && onDismissRequest != null) AlertAction(cancelText, false, ready, narrow) { close(onDismissRequest) }
                    AlertAction(confirmText, true, ready, narrow) { close { currentConfirm() } }
                }
            }
        }
    }
}

@Composable
private fun AlertAction(text: String, confirm: Boolean, enabled: Boolean, narrow: Boolean, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.CenterStart) {
        ZenlessButton(onClick, Modifier.widthIn(min = if (narrow) 140.dp else 240.dp).heightIn(min = 56.dp), enabled = enabled, size = ZenlessSize.Large) {
            Spacer(Modifier.width(24.dp)); ZenlessText(text)
        }
        Box(Modifier.padding(start = 4.dp).size(46.dp).background(Color(0xff060606), RoundedCornerShape(23.dp)).border(4.dp, Color(0xff303030), RoundedCornerShape(23.dp)).padding(7.dp)
            .background(if (confirm) Color(0xff00d127) else Color(0xffec1000), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            Mark(if (confirm) Mark.Check else Mark.Close, Modifier.size(28.dp), Color.Black)
        }
    }
}

@Composable
public fun ZenlessDrawer(
    visible: Boolean, title: String, closeDescription: String, onDismissRequest: () -> Unit,
    footer: @Composable RowScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit,
) {
    Modal(visible, onDismissRequest, true) { frame, ready, close ->
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.CenterEnd) {
            Column(Modifier.width(if (maxWidth < 600.dp) maxWidth else (maxWidth * .46f).coerceAtMost(680.dp)).fillMaxHeight().graphicsLayer { translationX = size.width * (1 - frame.band) }
                .background(Palette.panel).border(2.dp, Palette.line).pointerInput(Unit) { detectTapGestures {} }) {
                Row(Modifier.fillMaxWidth().background(Color.Black).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ZenlessText(title, Modifier.weight(1f), ZenlessTextStyle.Subtitle)
                    ZenlessCloseButton({ close(onDismissRequest) }, closeDescription, enabled = ready)
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp), content = content)
                Row(Modifier.fillMaxWidth().background(Color.Black).padding(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), content = footer)
            }
        }
    }
}
