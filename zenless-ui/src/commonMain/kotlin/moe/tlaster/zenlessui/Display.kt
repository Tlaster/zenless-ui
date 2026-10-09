package moe.tlaster.zenlessui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

@Composable
public fun ZenlessCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, selected: Boolean = false, enabled: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val source = remember { MutableInteractionSource() }
    val feedback = rememberFeedback(source, selected, enabled)
    val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 0.dp, bottomStart = 20.dp)
    Column(modifier.background(Color(0xff222222), shape).border(2.dp, if (feedback.highlight > 0f) lerp(Color.Black, lerp(Color.Yellow, Color(0xff80c800), Motion.color(feedback.seconds)), feedback.highlight) else Color.Black, shape)
        .semantics { this.selected = selected }
        .then(if (onClick != null) Modifier.pointerClick(enabled, source, onClick = onClick) else Modifier)
        .padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
public fun ZenlessBadge(text: String, modifier: Modifier = Modifier, tone: ZenlessTone = ZenlessTone.Neutral) {
    val color = Palette.tone(tone)
    Box(modifier.background(color, RoundedCornerShape(5.dp)).border(2.dp, Color.Black, RoundedCornerShape(5.dp)).padding(horizontal = 14.dp, vertical = 6.dp)) {
        CompositionLocalProvider(LocalInk provides if (tone == ZenlessTone.Neutral) Color.White else Color.Black) { ZenlessText(text) }
    }
}

@Composable
public fun ZenlessProgress(value: Float, modifier: Modifier = Modifier, tone: ZenlessTone = ZenlessTone.Primary) {
    require(value.isFinite())
    val amount by animateFloatAsState(value.coerceIn(0f, 1f), tween(180))
    Canvas(modifier.fillMaxWidth().height(10.dp).semantics { progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(0f, 1f), 0f..1f) }) {
        drawRoundRect(Color(0xff222222), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
        if (amount > 0f) drawRoundRect(Brush.horizontalGradient(listOf(Palette.tone(tone), lerp(Palette.tone(tone), Color.White, .5f))),
            size = androidx.compose.ui.geometry.Size(size.width * amount, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
        drawRoundRect(Color.Black, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2), style = Stroke(1.dp.toPx()))
    }
}

@Composable
public fun ZenlessMetric(value: String, modifier: Modifier = Modifier, unit: String = "", change: String = "") {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        ZenlessText(value, style = ZenlessTextStyle.Number)
        if (unit.isNotEmpty()) ZenlessText(unit, Modifier.padding(bottom = 4.dp), ZenlessTextStyle.Caption)
        if (change.isNotEmpty()) CompositionLocalProvider(LocalInk provides Palette.signal) { ZenlessText(change, Modifier.padding(bottom = 4.dp)) }
    }
}

@Composable
public fun ZenlessInfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().background(Color.Black, RoundedCornerShape(24.dp)).padding(horizontal = 18.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ZenlessText(label, Modifier.weight(1f)); ZenlessText(value)
    }
}

@Composable
public fun ZenlessNotice(message: String, modifier: Modifier = Modifier, tone: ZenlessTone = ZenlessTone.Info) {
    Box(modifier.background(lerp(Palette.tone(tone), Color.Black, .5f), RoundedCornerShape(20.dp))
        .semantics { liveRegion = LiveRegionMode.Polite }.padding(horizontal = 20.dp, vertical = 10.dp)) { ZenlessText(message) }
}

@Composable
public fun ZenlessCollapse(title: String, expanded: Boolean, onExpandedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier) {
        ZenlessButton({ onExpandedChange(!expanded) }, Modifier.fillMaxWidth().semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }) {
            ZenlessText(title, Modifier.weight(1f))
            val angle by animateFloatAsState(if (expanded) 180f else 0f, tween(160))
            Mark(Mark.Down, Modifier.size(20.dp).graphicsLayer { rotationZ = angle })
        }
        AnimatedVisibility(expanded, enter = expandVertically(tween(180)) + fadeIn(tween(160)), exit = shrinkVertically(tween(180)) + fadeOut(tween(120))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        }
    }
}
