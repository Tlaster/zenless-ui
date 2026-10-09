package moe.tlaster.zenlessui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Folder plates are original vector geometry, with no screenshot-derived assets. */
@Composable
public fun ZenlessTabs(items: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier = Modifier, folder: Boolean = false) {
    require(items.isNotEmpty() && selectedIndex in items.indices)
    Row(modifier.background(Color.Black, RoundedCornerShape(if (folder) 0.dp else 28.dp)).padding(if (folder) 0.dp else 5.dp),
        verticalAlignment = Alignment.Bottom) {
        items.forEachIndexed { index, title ->
            val selected = selectedIndex == index
            val source = remember { MutableInteractionSource() }
            val feedback = rememberFeedback(source, selected && !folder, true)
            val rise by animateFloatAsState(if (selected) 0f else 7f, tween(160))
            val emphasis by animateFloatAsState(if (selected) 1f else 0f, tween(160))
            Box(Modifier.weight(1f).heightIn(min = if (folder) 66.dp else 44.dp).semantics { this.selected = selected }
                .drawWithCache {
                    val top = if (folder) rise.dp.toPx() else 0f
                    val path = Path().apply {
                        if (folder) {
                            moveTo(0f, size.height); lineTo(8.dp.toPx(), top + 10.dp.toPx()); lineTo(19.dp.toPx(), top)
                            lineTo(size.width * .55f, top); lineTo(size.width * .65f, top + 7.dp.toPx())
                            lineTo(size.width - 8.dp.toPx(), top + 7.dp.toPx()); lineTo(size.width, size.height)
                        } else {
                            moveTo(12.dp.toPx(), 0f); lineTo(size.width, 0f); lineTo(size.width - 12.dp.toPx(), size.height); lineTo(0f, size.height)
                        }; close()
                    }
                    onDrawBehind {
                        val color = if (folder) lerp(Color(0xff454545), Palette.signal, emphasis)
                            else if (selected) lerp(Color.Yellow, Color(0xff80c800), Motion.color(feedback.seconds)) else Color.Transparent
                        if (!folder && selected) {
                            val outset = kotlin.math.min(size.width, size.height) * .15f * Motion.pulse(feedback.seconds)
                            scale((size.width + 2 * outset) / size.width, (size.height + 2 * outset) / size.height) { drawPath(path, color) }
                        } else drawPath(path, color)
                        if (folder) { drawPath(path, Color.Black, style = Stroke(2.dp.toPx())); drawLine(Color(0xff777777), Offset(13.dp.toPx(), top + 11.dp.toPx()), Offset(size.width * .53f, top + 11.dp.toPx()), 1.dp.toPx()) }
                    }
                }.pointerClick(true, source, Role.Tab) { if (!selected) onSelected(index) }.padding(horizontal = 16.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
                CompositionLocalProvider(LocalInk provides if (selected) Color.Black else Color(0xffb7b8b8), LocalTextStyle provides LocalTextStyle.current.copy(fontSize = 16.sp)) { ZenlessText(title, maxLines = 1) }
            }
        }
    }
}

@Composable
public fun ZenlessNavigation(items: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    require(selectedIndex in items.indices)
    Column(modifier.background(Color.Black, RoundedCornerShape(20.dp)).padding(6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(Modifier.fillMaxWidth().height(14.dp).background(Color(0xff333333), RoundedCornerShape(12.dp)))
        items.forEachIndexed { index, text ->
            val source = remember { MutableInteractionSource() }
            val selected = selectedIndex == index
            val feedback = rememberFeedback(source, selected, true)
            Box(Modifier.fillMaxWidth().heightIn(min = 46.dp).semantics { this.selected = selected }
                .drawWithCache { onDrawBehind {
                    if (selected) drawRoundRect(lerp(Color.Yellow, Color(0xff80c800), Motion.color(feedback.seconds)), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
                } }.pointerClick(true, source, Role.Tab) { if (!selected) onSelected(index) }.padding(horizontal = 14.dp, vertical = 12.dp)) {
                CompositionLocalProvider(LocalInk provides if (selected) Color.Black else Color.White) { ZenlessText(text, maxLines = 1) }
            }
        }
        Box(Modifier.fillMaxWidth().height(14.dp).background(Color(0xff333333), RoundedCornerShape(12.dp)))
    }
}
