package moe.tlaster.zenlessui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.*
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/** Editing, IME and clipboard use Compose's text engine; no custom shortcut navigation. */
@Composable
public fun ZenlessTextField(
    value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
    label: String = "", placeholder: String = "", enabled: Boolean = true, readOnly: Boolean = false,
    error: String? = null, supportingText: String? = null, singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    size: ZenlessSize = LocalControlSize.current,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val feedback = rememberFeedback(source, false, enabled)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLayout(label, size = size) {
        BasicTextField(value, onValueChange, Modifier.fillMaxWidth().heightIn(min = size.height.dp)
            .semantics { if (label.isNotEmpty()) contentDescription = label; if (error != null) error(error) }
            .plate(feedback, if (enabled) Color(0xff1c1c1c) else Color(0xff2e2e2e), if (error != null) Palette.tone(ZenlessTone.Danger) else if (focused) Palette.signal else Color(0xff323232), pattern = false, expand = false, highlightFill = false, enabled = enabled, input = true)
            .padding(horizontal = size.horizontalPadding.dp, vertical = 8.dp),
            enabled = enabled, readOnly = readOnly, singleLine = singleLine, keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation, interactionSource = source,
            textStyle = LocalTextStyle.current.copy(color = if (enabled) Color.White else Color(0xff808080)),
            cursorBrush = SolidColor(Palette.signal),
            decorationBox = { inner -> Box(contentAlignment=Alignment.CenterStart) { if (value.isEmpty()) CompositionLocalProvider(LocalInk provides Palette.muted) { ZenlessText(placeholder) }; inner() } })
        }
        val hint = error ?: supportingText
        if (!hint.isNullOrEmpty()) CompositionLocalProvider(LocalInk provides if (error != null) Color(0xffff7358) else Palette.muted,LocalTextStyle provides LocalTextStyle.current.copy(fontSize=size.fontSize.sp)) { ZenlessText(hint) }
    }
}

internal fun nextCheckState(state: ToggleableState): ToggleableState = if (state == ToggleableState.On) ToggleableState.Off else ToggleableState.On

/** Caller-owned tri-state selection; the capsule and label share one click target. */
@Composable
public fun ZenlessCheckbox(state: ToggleableState, onStateChange: (ToggleableState) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, label: String = "", size: ZenlessSize = LocalControlSize.current) {
    val source = remember { MutableInteractionSource() }
    SelectionCapsule(state, source, modifier.heightIn(min = size.height.dp).focusProperties { canFocus = false }
        .triStateToggleable(state, source, null, enabled, Role.Checkbox) { onStateChange(nextCheckState(state)) }, enabled, label, size, radio = false)
}

/** Caller-owned single selection; activating an already selected row does not replay its animation. */
@Composable
public fun ZenlessRadioButton(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, label: String = "", size: ZenlessSize = LocalControlSize.current) {
    val source = remember { MutableInteractionSource() }
    SelectionCapsule(if (selected) ToggleableState.On else ToggleableState.Off, source,
        modifier.heightIn(min = size.height.dp).focusProperties { canFocus = false }
            .selectable(selected, source, null, enabled, Role.RadioButton, onClick), enabled, label, size, radio = true)
}

@Composable
private fun SelectionCapsule(state: ToggleableState, source: MutableInteractionSource, modifier: Modifier, enabled: Boolean, label: String, size: ZenlessSize, radio: Boolean) {
    val pressed by source.collectIsPressedAsState()
    val rim by animateFloatAsState(if (pressed && enabled) 1f else 0f, tween(100, easing = LinearEasing))
    // shortcut: input timestamps are absent; retain the shared rim cycle until a timed hold capture is available.
    val feedback = rememberFeedback(source, pressed || rim > 0f, enabled)
    val progress by animateFloatAsState(if (state == ToggleableState.On) 1f else 0f,
        if (enabled && state == ToggleableState.On) tween(400, easing = LinearEasing) else snap())
    val scale = size.iconSize / 28f
    val verticalInset = ((size.height - 38 * scale) / 2).dp
    Row(modifier.drawWithCache {
            val inset = verticalInset.toPx().coerceAtMost(this.size.height / 2)
            val bounds = Rect(0f, inset, this.size.width, this.size.height - inset)
            val radius = minOf(19 * scale.dp.toPx(), bounds.minDimension / 2)
            val shell = roundedPath(bounds, radius)
            val border = 4.5f * scale.dp.toPx()
            val outline = roundedPath(bounds.inflate(border), radius + border)
            onDrawBehind {
                if (enabled && rim > 0f) drawPath(outline,
                    mix(Palette.signal, Color(0xff91bc00), Motion.signalColor(feedback.seconds)), rim)
                drawPath(shell, Color.Black)
            }
        }
        .padding(start = (6 * scale).dp, end = ((if (label.isEmpty()) 6 else 18) * scale).dp, top = (8 * scale).dp, bottom = (8 * scale).dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy((12 * scale).dp)) {
        Canvas(Modifier.size(size.iconSize.dp)) {
            scale(this.size.width / 56f, this.size.height / 56f, Offset.Zero) {
                val pose = selectionPose(if (state == ToggleableState.On && enabled) progress else 1f)
                translate(28.4f + pose[3], 28f) {
                    rotate(pose[2], Offset.Zero) {
                        scale(pose[0], pose[1], Offset.Zero) {
                            val ink = if (enabled) Color(0xff00cd00) else Palette.muted
                            val ring = if (enabled) Brush.verticalGradient(
                                0f to Color(0xff00ff00), .14f to Color(0xff00ff00), .5f to Color(0xff00ca00), .88f to Color(0xff009913), 1f to Color(0xff009913),
                                startY = -25f, endY = 25f) else SolidColor(Palette.muted)
                            drawCircle(ring, 21.75f, Offset.Zero, style = Stroke(6.17f))
                            when {
                                state == ToggleableState.Indeterminate -> drawLine(ink, Offset(-12f, 0f), Offset(12f, 0f), 5f, StrokeCap.Round)
                                state == ToggleableState.On && radio -> {
                                    // shortcut: dot growth is inferred from Checkbox; recalibrate when a Radio reference is available.
                                    val growth = if (enabled) Motion.smoothEasing.transform(((progress - .25f) / .35f).coerceIn(0f, 1f)) else 1f
                                    drawCircle(ink, 14f * growth, Offset.Zero)
                                }
                                state == ToggleableState.On && progress >= .25f -> {
                                    val reveal = ((progress - .25f) / .3f).coerceIn(0f, 1f)
                                    clipRect(-30f, -30f, -25f + 55f * reveal, 30f) {
                                        drawPath(checkboxCheck, Color.Black, style = Stroke(6.5f, join = StrokeJoin.Round))
                                        drawPath(checkboxCheck, ink, style = Stroke(1.5f, join = StrokeJoin.Round))
                                        drawPath(checkboxCheck, ink)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (label.isNotEmpty()) CompositionLocalProvider(LocalInk provides if (enabled) Color(0xffc1c2c2) else Palette.muted,
            LocalTextStyle provides LocalTextStyle.current.copy(fontSize = size.fontSize.sp)) { ZenlessText(label) }
    }
}

// Checkbox's sampled projection, shared with Radio: time, scale X/Y, roll, translation X in the 56px icon.
private val selectionFrames = arrayOf(
    floatArrayOf(0f, 1f, 1f, 0f, 0f),
    floatArrayOf(67f, 1.18f, .48f, 0f, 0f),
    floatArrayOf(108f, .98f, .8f, -5f, -1f),
    floatArrayOf(167f, .7f, 1.13f, -7f, -1.5f),
    floatArrayOf(242f, 1.13f, .78f, 12f, 1f),
    floatArrayOf(308f, .97f, 1.02f, 6f, 1f),
    floatArrayOf(400f, 1f, 1f, 0f, 0f),
)

private fun selectionPose(progress: Float): FloatArray {
    val time = progress.coerceIn(0f, 1f) * 400
    val index = selectionFrames.indexOfFirst { it[0] >= time }.coerceAtLeast(1)
    val from = selectionFrames[index - 1]; val to = selectionFrames[index]
    val fraction = Motion.smoothEasing.transform((time - from[0]) / (to[0] - from[0]))
    return FloatArray(4) { from[it + 1] + (to[it + 1] - from[it + 1]) * fraction }
}

private val checkboxCheck = Path().apply {
    moveTo(-14.8f, -2.8f)
    quadraticTo(-16f, -3.8f, -17f, -2.5f)
    lineTo(-18.8f, -.1f); quadraticTo(-19.8f, 1.2f, -18.3f, 2.6f)
    lineTo(-2.4f, 16.7f); quadraticTo(-.5f, 18.5f, 1.5f, 16.2f)
    lineTo(22.7f, -9.3f); quadraticTo(24.1f, -11.1f, 22.2f, -12.5f)
    lineTo(19.3f, -14.3f); quadraticTo(17.8f, -15.2f, 16.4f, -13.4f)
    lineTo(-1f, 7.1f); close()
}

@Composable
public fun ZenlessSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, label: String = "", size: ZenlessSize = LocalControlSize.current) {
    val source = remember { MutableInteractionSource() }
    val controlScale = size.height / 62f
    val transition = updateTransition(checked)
    // The recording separates the departing legend, thumb travel, and arriving green face.
    val position by transition.animateFloat(transitionSpec = { tween(133, if (targetState) 0 else 33, CubicBezierEasing(.2f, 0f, .2f, 1f)) }) { if (it) 1f else 0f }
    val green by transition.animateFloat(transitionSpec = { if (targetState) tween(83, 50, Motion.smoothEasing) else tween(33, easing = Motion.smoothEasing) }) { if (it) 1f else 0f }
    val onInk by transition.animateFloat(transitionSpec = { if (targetState) tween(67, 67) else snap() }) { if (it) 1f else 0f }
    val offInk by transition.animateFloat(transitionSpec = { if (targetState) snap() else tween(67, 67) }) { if (it) 0f else 1f }
    Row(modifier.heightIn(min = size.height.dp).focusProperties { canFocus = false }
        .toggleable(checked, source, null, enabled, Role.Switch, onCheckedChange)
        .drawBehind {
            if (label.isNotEmpty()) {
                val bounds = Rect(0f, (3*controlScale).dp.toPx(), this.size.width, this.size.height - (2*controlScale).dp.toPx())
                drawPath(roundedPath(bounds, bounds.height / 2), Color.Black)
                drawPath(roundedPath(bounds.deflate(2.dp.toPx()), bounds.height / 2 - 2.dp.toPx()),
                    Brush.verticalGradient(listOf(Color(0xff282828), Color(0xff171717)), startY = bounds.top, endY = bounds.bottom))
                drawPath(roundedPath(bounds.deflate(2.5.dp.toPx()), bounds.height / 2 - 2.5.dp.toPx()),
                    Brush.verticalGradient(listOf(Color(0xff343434), Color.Transparent), startY = bounds.top, endY = 14.dp.toPx()), style = Stroke(1.dp.toPx()))
            }
        }, verticalAlignment = Alignment.CenterVertically) {
        if (label.isNotEmpty()) CompositionLocalProvider(
            LocalInk provides if (enabled) Color.White else Palette.muted,
            LocalTextStyle provides LocalTextStyle.current.copy(fontSize = size.fontSize.sp),
        ) { ZenlessText(label, Modifier.weight(1f, fill = false).padding(start = (23*controlScale).dp, end = (17*controlScale).dp)) }
        Canvas(Modifier.size((142*controlScale).dp, size.height.dp)) {
            scale(density*controlScale, density*controlScale, pivot = Offset.Zero) {
                fun pill(rect: Rect, color: Color) = drawPath(roundedPath(rect, rect.height / 2), color)
                val outer = Rect(0f, 0f, 142f, 62f)
                pill(outer, Color.Black)
                drawPath(roundedPath(outer.deflate(2.5f), 28.5f), Brush.verticalGradient(
                    0f to Color(0xff0b0b0b), .55f to Color(0xff0b0b0b), 1f to Color(0xff191919), endY = 60f))
                drawPath(roundedPath(outer.deflate(3.25f), 27.75f), Brush.linearGradient(
                    listOf(Color(0xff2c2c2c), Color(0xff161616)), end = Offset(130f, 50f)), style = Stroke(1.5f))
                pill(Rect(7.5f, 10.5f, 130.5f, 52f), Color.Black)
                drawRoundRect(mix(Color(0xff272727), if (enabled) Color(0xff36a100) else Color(0xff455335), green),
                    Offset(14.25f, 14.5f), Size(112.25f, 33.5f), CornerRadius(15.75f, 16.75f))
                translate(40.5f, 21f) { scale(1f, 1.1f, Offset.Zero) { drawPath(switchOn, Color.Black, onInk) } }
                translate(72.5f, 22f) { drawPath(switchOff, Color.Black, offInk) }
                switchKnob(Offset(28.5f + 86.5f * position, 31f))
            }
        }
    }
}

private fun DrawScope.switchKnob(center: Offset) {
    drawCircle(Color.Black, 21f, center + Offset(-.5f, 0f))
    fun ring(radius: Float, shades: List<Int>) {
        drawCircle(Brush.sweepGradient(shades.map { Color(it, it, it) }, center), radius, center)
    }
    ring(16.75f, listOf(11, 9, 26, 34, 46, 37, 76, 149, 97, 55, 18, 6, 11))
    ring(16f, listOf(14, 22, 40, 47, 55, 37, 85, 151, 209, 108, 67, 35, 14))
    ring(14.5f, listOf(10, 21, 39, 47, 55, 44, 74, 170, 230, 114, 67, 35, 10))
    ring(12.7f, listOf(14, 21, 36, 57, 69, 75, 114, 255, 255, 123, 89, 29, 14))
    val face = center + Offset(0f, .5f)
    drawCircle(Brush.radialGradient(0f to Color(0xff4b4b4b), .92f to Color(0xff4b4b4b), 1f to Color(0xff4b4b4b).copy(alpha = 0f), center = face, radius = 11.4f), 11.4f, face)
    drawRoundRect(Color(0xff656565), center + Offset(-9f, -1.1f), Size(7.5f, 3f), CornerRadius(.8f))
    drawRoundRect(Color(0xff353535), center + Offset(-8.7f, -1.6f), Size(7f, 2f), CornerRadius(.5f))
}

// Intrinsic ON/OFF artwork keeps the condensed lettering identical across platform fonts.
private fun Path.switchO(x: Float, width: Float, height: Float) {
    addRoundRect(RoundRect(Rect(x, -.5f, x + width, height - .5f), CornerRadius(width / 2, 4f)))
    addRoundRect(RoundRect(Rect(x + width / 2 - .85f, 3.5f, x + width / 2 + .85f, height - 4f), CornerRadius(.85f)))
}

private val switchOn = Path().apply {
    fillType = PathFillType.EvenOdd
    switchO(0f, 13f, 20.5f)
    moveTo(15f, 0f); lineTo(19.5f, 0f); lineTo(23f, 9f); lineTo(23f, 0f); lineTo(27.5f, 0f)
    lineTo(27.5f, 19.5f); lineTo(22.5f, 19.5f); lineTo(19f, 11f); lineTo(19f, 19.5f); lineTo(15f, 19.5f); close()
}

private val switchOff = Path().apply {
    fillType = PathFillType.EvenOdd
    switchO(0f, 11.5f, 20f)
    for (x in listOf(13.5f, 23f)) {
        moveTo(x, 0f); lineTo(x + 8.5f, 0f); lineTo(x + 8.5f, 4f); lineTo(x + 5f, 4f)
        lineTo(x + 5f, 8f); lineTo(x + 7.5f, 8f); lineTo(x + 7.5f, 12f)
        lineTo(x + 5f, 12f); lineTo(x + 5f, 19f); lineTo(x, 19f); close()
    }
}

internal fun sliderValue(fraction: Float, range: ClosedFloatingPointRange<Float>, steps: Int): Float {
    val normalized = fraction.coerceIn(0f, 1f)
    val snapped = if (steps == 0) normalized else (normalized * (steps + 1)).roundToInt().toFloat() / (steps + 1)
    return range.start + snapped * (range.endInclusive - range.start)
}

@Composable
public fun ZenlessSlider(
    value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f, steps: Int = 0, enabled: Boolean = true,
    label: String = "", size: ZenlessSize = LocalControlSize.current, onValueChangeFinished: () -> Unit = {},
) {
    require(value.isFinite() && valueRange.start.isFinite() && valueRange.endInclusive.isFinite() && valueRange.endInclusive > valueRange.start)
    require(steps in 0..10000)
    var width by remember { mutableIntStateOf(1) }
    val update by rememberUpdatedState(onValueChange)
    val finished by rememberUpdatedState(onValueChangeFinished)
    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    val numberSource = remember { MutableInteractionSource() }
    val numberFocused by numberSource.collectIsFocusedAsState()
    var numberText by remember { mutableStateOf(value.toString().removeSuffix(".0")) }
    LaunchedEffect(value, numberFocused) { if (!numberFocused) numberText=value.toString().removeSuffix(".0") }
    val knobScale = size.iconSize / 28f
    val knobRadius = (12*knobScale).dp
    FieldLayout(label, modifier, size) {
        Row(verticalAlignment=Alignment.CenterVertically) {
        Canvas(Modifier.weight(1f).padding(end=size.horizontalPadding.dp).height(size.height.dp).onSizeChanged { width = it.width }
            .semantics {
                if (label.isNotEmpty()) contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(valueRange), valueRange, steps)
                if (!enabled) disabled() else setProgress { update(sliderValue((it - valueRange.start) / (valueRange.endInclusive - valueRange.start), valueRange, steps)); finished(); true }
            }
            .pointerInput(enabled, valueRange, steps, size) { if (enabled) detectTapGestures { position -> update(sliderValue((position.x - knobRadius.toPx()) / (width - 2*knobRadius.toPx()).coerceAtLeast(1f), valueRange, steps)); finished() } }
            .pointerInput(enabled, valueRange, steps, size) { if (enabled) detectHorizontalDragGestures(onDragEnd = { finished() }) { change, _ ->
                change.consume(); update(sliderValue((change.position.x - knobRadius.toPx()) / (width - 2*knobRadius.toPx()).coerceAtLeast(1f), valueRange, steps))
            } }) {
            val start = Offset(knobRadius.toPx(), this.size.height / 2)
            val end = Offset(this.size.width - knobRadius.toPx(), this.size.height / 2)
            val knob = Offset(start.x + (end.x - start.x) * fraction, start.y)
            drawLine(Color(0xff383838), start, end, 6.dp.toPx(), StrokeCap.Round)
            drawLine(if (enabled) Color(0xff737373) else Palette.line, start, knob, 6.dp.toPx(), StrokeCap.Round)
            scale(knobScale,knobScale,knob) { metalKnob(knob) }
        }
        BasicTextField(numberText, { raw ->
            numberText=raw
            raw.toFloatOrNull()?.takeIf { it.isFinite() }?.let { update(sliderValue((it-valueRange.start)/(valueRange.endInclusive-valueRange.start),valueRange,steps));finished() }
        },
            Modifier.width((size.fontSize*2+28).dp).heightIn(min=size.height.dp).background(Color.Black,RoundedCornerShape(50)).border(3.dp,Color(0xff323232),RoundedCornerShape(50)).padding(horizontal=8.dp,vertical=6.dp),enabled=enabled,singleLine=true,
            interactionSource=numberSource,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),
            textStyle=LocalTextStyle.current.copy(color=if(enabled)Color.White else Palette.muted),cursorBrush=SolidColor(Palette.signal),
            decorationBox={ inner -> Box(contentAlignment=Alignment.Center) { inner() } })
        }
    }
}


@Composable
internal fun FieldLayout(label: String, modifier: Modifier = Modifier, size: ZenlessSize = LocalControlSize.current, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontSize=size.fontSize.sp)) {
    BoxWithConstraints(modifier) {
        if (label.isNotEmpty() && maxWidth >= 540.dp) Row(verticalAlignment=Alignment.CenterVertically) {
            ZenlessText(label,Modifier.width(180.dp).padding(end=16.dp))
            Box(Modifier.weight(1f)) { content() }
        } else Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            if(label.isNotEmpty()) ZenlessText(label)
            content()
        }
    }
    }
}
