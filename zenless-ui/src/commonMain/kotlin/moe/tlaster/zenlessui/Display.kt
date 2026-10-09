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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.*

@Composable
public fun ZenlessCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, selected: Boolean = false, enabled: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val source = remember { MutableInteractionSource() }
    val feedback = rememberFeedback(source, selected, enabled)
    val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 0.dp, bottomStart = 20.dp)
    Column(modifier.graphicsLayer { alpha=if(enabled)1f else .5f }.background(Color(0xff222222), shape).border(2.dp, if (feedback.highlight > 0f) mix(Color.Black, mix(Palette.signal, Color(0xff91bc00), Motion.signalColor(feedback.seconds)), feedback.highlight) else Color.Black, shape)
        .semantics { this.selected = selected }
        .then(if (onClick != null) Modifier.pointerClick(enabled, source, onClick = onClick) else Modifier)
        .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
public fun ZenlessBadge(text: String, modifier: Modifier = Modifier, tone: ZenlessTone = ZenlessTone.Neutral) {
    val color = if(tone==ZenlessTone.Neutral)Color(0xff1c1c1c) else Palette.tone(tone)
    val edge=if(tone==ZenlessTone.Primary || tone==ZenlessTone.Info)color else Color(0xff222222)
    Box(modifier.heightIn(min=30.dp).background(color, RoundedCornerShape(5.dp)).border(2.dp, edge, RoundedCornerShape(5.dp)).padding(horizontal=18.dp,vertical=2.dp),contentAlignment=Alignment.Center) {
        CompositionLocalProvider(LocalInk provides if(tone==ZenlessTone.Neutral || tone==ZenlessTone.Primary)Color.White else Color.Black,LocalTextStyle provides LocalTextStyle.current.copy(fontSize=14.sp)) { ZenlessText(text) }
    }
}

@Composable
public fun ZenlessProgress(value: Float, modifier: Modifier = Modifier, tone: ZenlessTone = ZenlessTone.Primary) {
    require(value.isFinite())
    val amount by animateFloatAsState(value.coerceIn(0f,1f),tween(180))
    Canvas(modifier.fillMaxWidth().height(10.dp).semantics { progressBarRangeInfo=ProgressBarRangeInfo(value.coerceIn(0f,1f),0f..1f) }) {
        val border=1.dp.toPx()
        val color=if(tone==ZenlessTone.Primary)Color(0xff4664ff) else Palette.tone(tone)
        drawRoundRect(Color.Black,cornerRadius=CornerRadius(size.height/2))
        drawRoundRect(Color(0xff222222),Offset(border,border),Size(size.width-2*border,size.height-2*border),CornerRadius(size.height/2-border))
        val length=(size.width-2*border)*amount
        if(length>0) {
            fun overlay(a:Color,b:Color,t:Float):Color {
                fun blend(x:Float,y:Float)=if(x<=.5f)2*x*y else 1-2*(1-x)*(1-y)
                return mix(a,Color(blend(a.red,b.red),blend(a.green,b.green),blend(a.blue,b.blue)),t)
            }
            val stops=(0..7).map { val t=it/7f; t to overlay(overlay(overlay(color,Color.White,.3f*t),color,t),Color.White,t) }.toTypedArray()
            drawRoundRect(Brush.horizontalGradient(*stops,startX=border,endX=border+length),Offset(border,border),Size(length,size.height-2*border),CornerRadius(size.height/2-border))
        }
    }
}

@Composable
public fun ZenlessMetric(value: String, modifier: Modifier = Modifier, unit: String = "", change: String = "") {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        ZenlessText(value, style = ZenlessTextStyle.Number)
        if (unit.isNotEmpty()) ZenlessText(unit, style=ZenlessTextStyle.Caption)
        if (change.isNotEmpty()) CompositionLocalProvider(LocalInk provides Color(0xff00cc0d),LocalTextStyle provides LocalTextStyle.current.copy(fontSize=20.sp)) { ZenlessText(change) }
    }
}

@Composable
public fun ZenlessInfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().heightIn(min=40.dp).background(Color.Black, RoundedCornerShape(20.dp)).padding(horizontal = 24.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ZenlessText(label, Modifier.weight(1f)); ZenlessText(value)
    }
}

@Composable
public fun ZenlessNotice(message: String, modifier: Modifier = Modifier, tone: ZenlessTone = ZenlessTone.Info) {
    val fill=when(tone) { ZenlessTone.Success->Color(0xff006607); ZenlessTone.Danger->Color(0xff600e00); ZenlessTone.Warning->Color(0xff806200); else->Color.Black }
    val ink=when(tone) { ZenlessTone.Success->Color(0xff80e687); ZenlessTone.Danger->Color(0xffe08e80); ZenlessTone.Warning->Color(0xffffe180); else->Color.White }
    Box(modifier.heightIn(min=34.dp).background(fill,RoundedCornerShape(17.dp)).semantics { liveRegion=LiveRegionMode.Polite }.padding(horizontal=34.dp,vertical=7.dp),contentAlignment=Alignment.Center) {
        CompositionLocalProvider(LocalInk provides ink,LocalTextStyle provides LocalTextStyle.current.copy(fontSize=14.sp)) { ZenlessText(message) }
    }
}

@Composable
public fun ZenlessCollapse(title: String, expanded: Boolean, onExpandedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier) {
        val source=remember { MutableInteractionSource() }
        val feedback=rememberFeedback(source,false,true,buttonFeedback=true)
        Row(Modifier.fillMaxWidth().heightIn(min=40.dp).background(Color.Black,RoundedCornerShape(20.dp)).drawWithCache { onDrawBehind {
            if(feedback.highlight>0f) {
                val outset=2.dp.toPx()+if(feedback.release<0)size.minDimension*.15f*Motion.pulse(feedback.seconds) else 0f
                drawPath(roundedPath(Rect(Offset.Zero,size).inflate(outset),size.height/2+outset),mix(Color.Yellow,Color(0xff80c800),Motion.color(feedback.seconds)).copy(alpha=feedback.highlight))
            }
        }}.semantics { stateDescription=if(expanded) "Expanded" else "Collapsed" }.pointerClick(true,source) { onExpandedChange(!expanded) }.padding(start=24.dp,end=8.dp),verticalAlignment=Alignment.CenterVertically) {
            CompositionLocalProvider(LocalInk provides mix(Color.White,Color.Black,feedback.highlight),LocalTextStyle provides LocalTextStyle.current.copy(fontSize=14.sp,letterSpacing=0.sp)) {
            ZenlessText(title, Modifier.weight(1f))
            val angle by animateFloatAsState(if (expanded) 180f else 0f, tween(160))
            Mark(Mark.Down, Modifier.size(32.dp).graphicsLayer { rotationZ = angle })
            }
        }
        AnimatedVisibility(expanded, enter = expandVertically(tween(180)) + fadeIn(tween(160)), exit = shrinkVertically(tween(180)) + fadeOut(tween(120))) {
            Column(Modifier.padding(start=28.dp,end=28.dp,top=12.dp,bottom=8.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        }
    }
}
