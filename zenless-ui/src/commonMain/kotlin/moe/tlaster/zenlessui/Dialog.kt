package moe.tlaster.zenlessui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** A single-layer modal with a fixed header and a vertically scrolling content slot.
 * Keep this composable mounted when visible becomes false so its exit can finish.
 * User dismissal invokes onDismissRequest once, after exit; outside taps only block input.
 * Modifier constrains the window. Background replaces the entire body decoration.
 */
@Composable
public fun ZenlessDialog(
    visible: Boolean,
    title: String,
    closeDescription: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    size: ZenlessSize = LocalControlSize.current,
    background: @Composable () -> Unit = { DialogBackground() },
    content: @Composable ColumnScope.() -> Unit,
) {
    val context = checkNotNull(LocalOverlays.current) { "ZenlessDialog requires ZenlessOverlayHost" }
    val id = remember { Any() }
    val opacity = remember { Animatable(0f) }
    val position = remember { Animatable(120f) }
    val effect = remember { Animatable(0f) }
    val artwork = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }
    var retained by remember { mutableStateOf(false) }
    val dismiss by rememberUpdatedState(onDismissRequest)
    LaunchedEffect(visible) { if (visible) closing = false }
    LaunchedEffect(visible, closing) {
        if (visible && !closing) {
            if (!retained) { position.snapTo(120f); retained = true }
            coroutineScope {
                launch { position.animateTo(0f, tween(180, easing = EaseOutCubic)) }
                launch { opacity.animateTo(1f, tween(100)) }
                launch { effect.animateTo(1f, tween(160)) }
                artwork.animateTo(1f, tween(140, delayMillis = 60))
            }
        } else if (retained) {
            coroutineScope {
                launch { position.animateTo(-120f, tween(180, easing = EaseInCubic)) }
                launch { opacity.animateTo(0f, tween(80, delayMillis = 80)) }
                launch { effect.animateTo(0f, tween(100, delayMillis = 60)) }
                artwork.animateTo(0f, tween(120))
            }
            retained = false
            if (visible && closing) dismiss()
        }
    }
    val shown = retained || visible && !closing
    val ready = shown && visible && !closing && position.value == 0f && opacity.value == 1f
    val requestClose = { if (visible && !closing) closing = true }
    SideEffect {
        context.activeModals[id] = shown
        context.blurs[id] = effect.value
        if (shown) context.tooltip = null
    }
    val overlay by rememberUpdatedState<@Composable () -> Unit> {
        if (shown) {
            val focus = remember { FocusRequester() }
            DisposableEffect(Unit) {
                val savedFocus = context.backingFocus.saveFocusedChild()
                onDispose { if (savedFocus) context.backingFocus.restoreFocusedChild() }
            }
            LaunchedEffect(Unit) { withFrameNanos { }; focus.requestFocus() }
            ModalBackHandler(true, requestClose)
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .58f * effect.value)).drawBehind {
                val step = 7.dp.toPx()
                for (i in 0..((this.size.width + this.size.height) / step).toInt()) {
                    val x = i * step - this.size.height
                    drawLine(Color(0xff171717).copy(alpha = .55f * effect.value), Offset(x, 0f), Offset(x + this.size.height, this.size.height), 2.dp.toPx())
                }
            }.pointerInput(Unit) { detectTapGestures {} }
                .onPreviewKeyEvent {
                    if (it.key == Key.Escape) { if (it.type == KeyEventType.KeyUp) requestClose(); true }
                    else !ready
                }
                .focusProperties { onExit = { if (shown) cancelFocusChange() } }
                .focusRequester(focus).focusable()
                .semantics { paneTitle = title }) {
                BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(16.dp), contentAlignment = Alignment.Center) {
                    val scale = size.scale
                    val viewportWidth = maxWidth
                    val scroll = rememberScrollState()
                    CompositionLocalProvider(LocalControlSize provides size, LocalTextStyle provides LocalTextStyle.current.copy(fontSize = size.fontSize.sp)) {
                        Column(modifier.width(762.dp).heightIn(max = maxHeight).graphicsLayer {
                            translationX = position.value.dp.toPx(); alpha = opacity.value
                        }.dialogFrame(scale).pointerInput(ready) {
                            if (!ready) awaitPointerEventScope { while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() } }
                        }) {
                            Box(Modifier.fillMaxWidth().height((113 * scale).dp).drawWithCache {
                                val dots = dialogDotPath(this.size)
                                val shade = Brush.verticalGradient(0f to Color(0xff0c0c0c), .2f to Color(0xff0c0c0c), 1f to Color(0xff030303))
                                onDrawBehind { drawRect(shade); drawPath(dots, Color.Black.copy(alpha = .55f)) }
                            }) {
                                Box(Modifier.align(Alignment.BottomStart).padding(start = (66 * scale).dp.coerceAtMost(viewportWidth * .0866f), end = (144 * scale).dp, bottom = (22 * scale).dp)) {
                                    CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontSize = (28 * scale).sp, fontWeight = FontWeight.Bold)) {
                                        ZenlessText(title, maxLines = 1)
                                    }
                                }
                                NavigationButton(requestClose, closeDescription,
                                    Modifier.align(Alignment.TopEnd).padding(top = (29 * scale).dp, end = (29 * scale).dp)
                                        .requiredSize((90 * scale).dp, (60 * scale).dp).focusProperties { canFocus = ready },
                                    enabled = true, back = false, controlSize = size, clickDelayMillis = 0)
                            }
                            Box(Modifier.weight(1f, fill = false).fillMaxWidth()) {
                                Box(Modifier.matchParentSize().clearAndSetSemantics {}
                                    .focusProperties { canFocus = false; onEnter = { cancelFocusChange() } }.focusGroup()
                                    .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() } } }
                                    .graphicsLayer { alpha = artwork.value }) { background() }
                                Box(Modifier.padding(horizontal = (60 * scale).dp.coerceAtMost(viewportWidth * .079f))
                                    .padding(top = (43 * scale).dp, bottom = (52 * scale).dp)
                                    .fillMaxWidth().clip(RoundedCornerShape((18 * scale).dp)).background(Color.Black.copy(alpha = .78f))) {
                                    Column(Modifier.fillMaxWidth().verticalScroll(scroll).padding(horizontal = (25.5f * scale).dp).padding(top = (27 * scale).dp, bottom = (29 * scale).dp),
                                        verticalArrangement = Arrangement.spacedBy((22 * scale).dp), content = content)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    DisposableEffect(context) {
        context.modals[id] = { overlay() }
        onDispose { context.modals.remove(id); context.activeModals.remove(id); context.blurs.remove(id) }
    }
}

@Composable
private fun DialogBackground() {
    ZenlessAnimatedBackground {
        Box(Modifier.padding(24.dp)) {
            CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontSize = 150.sp, fontWeight = FontWeight.Black)) {
                ZenlessText("ZENLESS UI", maxLines = 1)
            }
        }
    }
}
