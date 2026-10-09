package moe.tlaster.zenlessui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Redrawn folder faces retain their rail, rounded silhouette and seven-unit rise. */
@Composable
public fun ZenlessTabs(items: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier = Modifier, folder: Boolean = false) {
    require(items.isNotEmpty() && selectedIndex in items.indices)
    val base = remember { Feedback() }
    val outlineWidth=with(LocalDensity.current) { 4.dp.toPx() }
    Row(modifier.then(if (!folder) Modifier.plate(base) else Modifier.height(84.dp).drawWithCache {
        onDrawBehind {
            val y=size.height-8.5.dp.toPx()
            drawLine(Color.Black,Offset(7.5.dp.toPx(),y),Offset(size.width-7.5.dp.toPx(),y),3.dp.toPx(),StrokeCap.Round)
            drawCircle(Color.Black,7.5.dp.toPx(),Offset(7.5.dp.toPx(),y))
            drawCircle(Color.Black,7.5.dp.toPx(),Offset(size.width-7.5.dp.toPx(),y))
        }
    }).padding(horizontal=if(folder)14.dp else 0.dp).padding(bottom=if(folder)6.dp else 0.dp),
        horizontalArrangement=Arrangement.spacedBy(if(folder)3.5.dp else 0.dp), verticalAlignment=Alignment.Bottom) {
        items.forEachIndexed { index, title ->
            val selected=selectedIndex==index
            val source=remember { MutableInteractionSource() }
            val pressed by source.collectIsPressedAsState()
            val hovered by source.collectIsHoveredAsState()
            val feedback=rememberFeedback(source,selected && !folder,true)
            val rise by animateFloatAsState(if(selected)0f else 7f,tween(160,easing=CubicBezierEasing(0f,0f,.58f,1f)))
            val emphasis by animateFloatAsState(if(selected)1f else 0f,tween(160,easing=CubicBezierEasing(0f,0f,.58f,1f)))
            Box(Modifier.weight(1f).heightIn(min=if(folder)78.dp else 40.dp).semantics { this.selected=selected }
                .hoverable(source).drawWithCache {
                    onDrawBehind {
                        if(folder) {
                            val top=rise.dp.toPx()
                            val rect=Rect(0f,top,size.width,size.height)
                            val shape=Path().apply {addRoundRect(RoundRect(rect,CornerRadius(18.dp.toPx()),CornerRadius(18.dp.toPx()),CornerRadius.Zero,CornerRadius.Zero))}
                            clipPath(shape) {
                                drawRect(Brush.verticalGradient(listOf(mix(Color(0xff555555),Color(0xffffe600),emphasis),mix(Color(0xff262626),Color(0xffffb500),emphasis)),startY=top,endY=size.height))
                                val motif=Path().apply {
                                    moveTo(12.dp.toPx(),top+9.dp.toPx());lineTo(size.width*.42f,top+9.dp.toPx());lineTo(size.width*.35f,top+24.dp.toPx());lineTo(18.dp.toPx(),top+32.dp.toPx());close()
                                    moveTo(15.dp.toPx(),size.height-24.dp.toPx());lineTo(size.width*.52f,top+28.dp.toPx());lineTo(size.width*.47f,top+38.dp.toPx());lineTo(18.dp.toPx(),size.height-10.dp.toPx());close()
                                }
                                drawPath(motif,Color.White.copy(alpha=.045f))
                                drawRect(Brush.verticalGradient(listOf(Color.Transparent,Color.Black.copy(alpha=.8f)),startY=size.height-12.dp.toPx(),endY=size.height))
                                if(pressed)drawRect(Color.Black.copy(alpha=.18f))
                                else if(hovered && !selected)drawRect(Color.White.copy(alpha=.025f))
                            }
                            drawPath(shape,Color.Black,style=Stroke(2.5.dp.toPx()))
                            val lip=Path().apply {addRoundRect(RoundRect(rect.deflate(3.dp.toPx()),CornerRadius(15.dp.toPx()),CornerRadius(15.dp.toPx()),CornerRadius.Zero,CornerRadius.Zero))}
                            clipRect(bottom=top+22.dp.toPx()) { drawPath(lip,Brush.verticalGradient(listOf(Color.White.copy(alpha=.12f),Color.Transparent),startY=top,endY=top+22.dp.toPx()),style=Stroke(2.dp.toPx())) }
                        } else if(selected) {
                            val outset=size.minDimension*.15f*Motion.pulse(feedback.seconds)
                            scale(density,density,pivot=Offset.Zero) {
                                drawPath(skewTabPath(Rect(-1f,-1f,size.width/density+1,size.height/density+1),outset/density),mix(Color.Yellow,Color(0xff80c800),Motion.color(feedback.seconds)))
                            }
                        }
                    }
                }.pointerClick(true,source,Role.Tab) { if(!selected)onSelected(index) }
                .padding(horizontal=if(folder)12.dp else 20.dp).padding(top=if(folder)rise.dp else 0.dp),contentAlignment=Alignment.Center) {
                val style=LocalTextStyle.current.copy(fontSize=if(folder)28.sp else 14.sp,letterSpacing=0.sp,
                    fontWeight=if(folder)FontWeight.Bold else FontWeight.Normal,color=if(folder)mix(Color(0xffb7b8b8),Color.Black,emphasis) else if(selected)Color.Black else Color.White)
                if(folder && emphasis<1f) BasicText(title,modifier=Modifier.clearAndSetSemantics {},style=style.copy(color=Color.Black.copy(alpha=1-emphasis),drawStyle=Stroke(outlineWidth)),maxLines=1,overflow=TextOverflow.Ellipsis)
                BasicText(title,style=style,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
public fun ZenlessNavigation(items: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    require(selectedIndex in items.indices)
    Box(modifier.background(Color.Black,RoundedCornerShape(20.dp)).border(3.dp,Color(0xff404040),RoundedCornerShape(20.dp))) {
        Column(Modifier.padding(horizontal=12.dp,vertical=59.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
            items.forEachIndexed { index,text ->
                val source=remember { MutableInteractionSource() }
                val selected=selectedIndex==index
                val feedback=rememberFeedback(source,selected,true)
                Box(Modifier.fillMaxWidth().heightIn(min=46.dp).semantics { this.selected=selected }
                    .drawWithCache { onDrawBehind {
                        drawLine(Color(0xff181818),Offset(0f,size.height-1.5.dp.toPx()),Offset(size.width,size.height-1.5.dp.toPx()),3.dp.toPx())
                        if(selected)drawRoundRect(mix(Palette.signal,Color(0xff91bc00),Motion.signalColor(feedback.seconds)),cornerRadius=CornerRadius(6.dp.toPx()))
                    }}.pointerClick(true,source,Role.Tab) { if(!selected)onSelected(index) }.padding(horizontal=20.dp,vertical=6.dp),contentAlignment=Alignment.CenterStart) {
                    CompositionLocalProvider(LocalInk provides if(selected)Color.Black else Color.White,LocalTextStyle provides LocalTextStyle.current.copy(fontSize=14.sp)) { ZenlessText(text,maxLines=1) }
                }
            }
        }
        for(top in listOf(true,false)) Canvas(Modifier.align(if(top)Alignment.TopCenter else Alignment.BottomCenter).padding(horizontal=7.dp,vertical=8.dp).fillMaxWidth().height(26.dp)) {
            drawRoundRect(Color(0xff323232),cornerRadius=CornerRadius(13.dp.toPx()))
            clipRect(bottom=1.dp.toPx()) { drawRoundRect(Color(0xff404040),cornerRadius=CornerRadius(13.dp.toPx())) }
            val p=Path().apply { val cy=size.height/2; moveTo(size.width/2-18.dp.toPx(),cy+(if(top)5 else -5).dp.toPx());lineTo(size.width/2+18.dp.toPx(),cy+(if(top)5 else -5).dp.toPx());lineTo(size.width/2,cy+(if(top)-5 else 5).dp.toPx());close() }
            drawPath(p,Color.Black)
        }
    }
}
