package moe.tlaster.zenlessui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
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
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val feedback = rememberFeedback(source, false, enabled)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLayout(label) {
        BasicTextField(value, onValueChange, Modifier.fillMaxWidth().heightIn(min = 40.dp)
            .semantics { if (label.isNotEmpty()) contentDescription = label; if (error != null) error(error) }
            .plate(feedback, if (enabled) Color(0xff1c1c1c) else Color(0xff2e2e2e), if (error != null) Palette.tone(ZenlessTone.Danger) else if (focused) Palette.signal else Color(0xff323232), pattern = false, expand = false, highlightFill = false, enabled = enabled, input = true)
            .padding(horizontal = 17.dp, vertical = 10.dp),
            enabled = enabled, readOnly = readOnly, singleLine = singleLine, keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation, interactionSource = source,
            textStyle = LocalTextStyle.current.copy(fontSize=14.sp,color = if (enabled) Color.White else Color(0xff808080)),
            cursorBrush = SolidColor(Palette.signal),
            decorationBox = { inner -> Box { if (value.isEmpty()) CompositionLocalProvider(LocalInk provides Palette.muted) { ZenlessText(placeholder) }; inner() } })
        }
        val hint = error ?: supportingText
        if (!hint.isNullOrEmpty()) CompositionLocalProvider(LocalInk provides if (error != null) Color(0xffff7358) else Palette.muted) { ZenlessText(hint) }
    }
}

internal fun nextCheckState(state: ToggleableState): ToggleableState = if (state == ToggleableState.On) ToggleableState.Off else ToggleableState.On

@Composable
public fun ZenlessCheckbox(state: ToggleableState, onStateChange: (ToggleableState) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, label: String = "") {
    val source = remember { MutableInteractionSource() }
    val feedback = rememberFeedback(source, false, enabled)
    val fill by animateFloatAsState(if (state == ToggleableState.Off) 0f else 1f, tween(160))
    Row(modifier.heightIn(min = 48.dp).focusProperties { canFocus = false }
        .triStateToggleable(state, interactionSource = source, indication = null, enabled = enabled, role = Role.Checkbox) { onStateChange(nextCheckState(state)) }.padding(6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(28.dp).plate(feedback, if(enabled)mix(Color.Black, Palette.signal, fill) else Color(0xff2e2e2e), Color(0xff333333), round = false, pattern = false,enabled=enabled), contentAlignment = Alignment.Center) {
            if (state != ToggleableState.Off) Mark(if (state == ToggleableState.Indeterminate) Mark.Minus else Mark.Check, Modifier.size(20.dp), if (enabled) Color.Black else Palette.muted)
        }
        if (label.isNotEmpty()) CompositionLocalProvider(LocalInk provides if (enabled) Color.White else Palette.muted) { ZenlessText(label) }
    }
}

@Composable
public fun ZenlessRadioButton(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, label: String = "") {
    val source = remember { MutableInteractionSource() }
    val feedback = rememberFeedback(source, false, enabled)
    val fraction by animateFloatAsState(if (selected) 1f else 0f, tween(160))
    Row(modifier.heightIn(min = 48.dp).focusProperties { canFocus = false }.selectable(selected, source, null, enabled, Role.RadioButton, onClick).padding(6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Canvas(Modifier.size(28.dp).plate(feedback, pattern = false,enabled=enabled)) {
            drawCircle(if (enabled) Palette.signal else Palette.muted, 7.dp.toPx() * fraction)
        }
        if (label.isNotEmpty()) CompositionLocalProvider(LocalInk provides if (enabled) Color.White else Palette.muted) { ZenlessText(label) }
    }
}

@Composable
public fun ZenlessSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, label: String = "") {
    val source = remember { MutableInteractionSource() }
    val feedback = rememberFeedback(source, false, enabled)
    val fraction by animateFloatAsState(if (checked) 1f else 0f, tween(160))
    Row(modifier.heightIn(min = 48.dp).focusProperties { canFocus = false }
        .triStateToggleable(if (checked) ToggleableState.On else ToggleableState.Off, interactionSource = source, indication = null, enabled = enabled, role = Role.Switch) { onCheckedChange(!checked) }.padding(6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Canvas(Modifier.size(52.dp, 30.dp).plate(feedback, if (enabled) mix(Color.Black, Palette.signal, fraction) else Color(0xff292929), pattern = false)) {
            val center=Offset(15.dp.toPx()+(size.width-30.dp.toPx())*fraction,size.height/2)
            metalKnob(center)
        }
        if (label.isNotEmpty()) CompositionLocalProvider(LocalInk provides if (enabled) Color.White else Palette.muted) { ZenlessText(label) }
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
    label: String = "", onValueChangeFinished: () -> Unit = {},
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
    FieldLayout(label, modifier) {
        Row(verticalAlignment=Alignment.CenterVertically) {
        Canvas(Modifier.weight(1f).padding(end=20.dp).height(40.dp).onSizeChanged { width = it.width }
            .semantics {
                if (label.isNotEmpty()) contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(valueRange), valueRange, steps)
                if (!enabled) disabled() else setProgress { update(sliderValue((it - valueRange.start) / (valueRange.endInclusive - valueRange.start), valueRange, steps)); finished(); true }
            }
            .pointerInput(enabled, valueRange, steps) { if (enabled) detectTapGestures { position -> update(sliderValue((position.x - 12.dp.toPx()) / (width - 24.dp.toPx()).coerceAtLeast(1f), valueRange, steps)); finished() } }
            .pointerInput(enabled, valueRange, steps) { if (enabled) detectHorizontalDragGestures(onDragEnd = { finished() }) { change, _ ->
                change.consume(); update(sliderValue((change.position.x - 12.dp.toPx()) / (width - 24.dp.toPx()).coerceAtLeast(1f), valueRange, steps))
            } }) {
            val start = Offset(12.dp.toPx(), size.height / 2)
            val end = Offset(size.width - 12.dp.toPx(), size.height / 2)
            val knob = Offset(start.x + (end.x - start.x) * fraction, start.y)
            drawLine(Color(0xff383838), start, end, 6.dp.toPx(), StrokeCap.Round)
            drawLine(if (enabled) Color(0xff737373) else Palette.line, start, knob, 6.dp.toPx(), StrokeCap.Round)
            metalKnob(knob)
        }
        BasicTextField(numberText, { raw ->
            numberText=raw
            raw.toFloatOrNull()?.takeIf { it.isFinite() }?.let { update(sliderValue((it-valueRange.start)/(valueRange.endInclusive-valueRange.start),valueRange,steps));finished() }
        },
            Modifier.width(54.dp).heightIn(min=32.dp).background(Color.Black,RoundedCornerShape(16.dp)).border(3.dp,Color(0xff323232),RoundedCornerShape(16.dp)).padding(horizontal=8.dp,vertical=6.dp),enabled=enabled,singleLine=true,
            interactionSource=numberSource,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),
            textStyle=LocalTextStyle.current.copy(fontSize=14.sp,color=if(enabled)Color.White else Palette.muted),cursorBrush=SolidColor(Palette.signal))
        }
    }
}


@Composable
internal fun FieldLayout(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier) {
        if (label.isNotEmpty() && maxWidth >= 540.dp) Row(verticalAlignment=Alignment.CenterVertically) {
            CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontSize=18.sp)) { ZenlessText(label,Modifier.width(180.dp)) }
            Box(Modifier.weight(1f)) { content() }
        } else Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            if(label.isNotEmpty()) CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontSize=18.sp)) { ZenlessText(label) }
            content()
        }
    }
}
