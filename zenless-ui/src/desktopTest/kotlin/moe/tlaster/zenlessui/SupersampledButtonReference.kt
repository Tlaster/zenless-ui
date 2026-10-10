package moe.tlaster.zenlessui

import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.*

/** Frozen pre-optimization shell for the opt-in experiment, outside the published library. */
internal fun CacheDrawScope.supersampledButtonReference(fill:Color,edge:Color,round:Boolean,pattern:Boolean,leading:Boolean):DrawScope.()->Unit {
    val bounds=Rect(.5.dp.toPx(),.5.dp.toPx(),size.width-.5.dp.toPx(),size.height)
    val radius=if(round)bounds.minDimension/2 else 6.dp.toPx()
    val inset=size.minDimension*.0835f
    val faceBounds=bounds.deflate(inset)
    val face=roundedPath(if(leading)faceBounds.copy(top=size.height*10.684f/116,bottom=size.height*106.50f/116) else faceBounds,radius-inset)
    val outline=roundedPath(bounds,radius)
    val bevel=roundedPath(bounds.deflate(.8.dp.toPx()),radius-.8.dp.toPx())
    val circular=size.width<size.height*1.1f
    val reflection=Brush.linearGradient(
        0f to Color(0xff3d3d3d),(if(circular).6f else .67f) to Color(0xff3d3d3d),1f to edge,
        end=Offset(size.width*(if(circular).22f else .0265f),size.height*.672f))
    val unit=size.height/116f
    // Measured in the reference button's 116-pixel-high coordinate space.
    val innerBounds=Rect(11.51f*unit,10.85f*unit,106.39f*unit,106.34f*unit)
    val outerBounds=Rect(8.48f*unit,2.53f*unit,116f*unit,114.64f*unit)
    val rightHalf=Path().apply { addRect(Rect(size.height/2,0f,size.width,size.height)) }
    val leftHalf=Path().apply { addRect(Rect(0f,0f,size.height/2,size.height)) }
    val left=mutableListOf<Pair<Path,Float>>()
    val right=mutableListOf<Pair<Path,Float>>()
    if(leading)for((spread,weight) in listOf(.6f to .12f,.3f to .2816f,0f to .41888f,-.3f to .17952f)) {
        val offset=spread.dp.toPx()
        val inner=Path().apply { addOval(innerBounds.deflate(offset)) }
        val outer=Path().apply { addOval(outerBounds.inflate(offset)) }
        left.add(Path.combine(PathOperation.Intersect,face,inner) to weight)
        var bodyFace=Path.combine(PathOperation.Intersect,rightHalf,Path.combine(PathOperation.Difference,face,outer))
        if(!round)bodyFace=Path.combine(PathOperation.Union,bodyFace,Path.combine(PathOperation.Intersect,leftHalf,Path.combine(PathOperation.Difference,face,inner)))
        right.add(bodyFace to weight)
    }
    val darkChecks=Paint().apply { shader=backCheckerPaint.shader;blendMode=BlendMode.SrcAtop }
    val checks=checkerPaint(mix(fill,Color.White,.06f))
    val maskedChecks=checkerPaint(mix(fill,Color.White,.06f)).apply { blendMode=BlendMode.SrcAtop }
    val paintButton:DrawScope.()->Unit = {
        drawPath(outline,edge)
        if(leading) {
            // One continuous shell: cut both faces before antialiasing their shared junctions.
            fun maskedFace(paths:List<Pair<Path,Float>>,color:Color) {
                drawContext.canvas.saveLayer(Rect(Offset.Zero,size),Paint())
                paths.forEach { (path,weight) -> drawPath(path,color.copy(alpha=weight),blendMode=BlendMode.Plus) }
                if(pattern) { if(color==Color.Black)buttonTexture(darkChecks) else checker(maskedChecks) }
                drawContext.canvas.restore()
            }
            scale(if(layoutDirection==LayoutDirection.Rtl)-1f else 1f,1f) {
                maskedFace(left,Color.Black)
                maskedFace(right,fill)
            }
        } else {
            drawPath(face,fill)
            if(pattern)clipPath(face) { if(fill==Color.Black)buttonTexture() else checker(checks) }
        }
        drawPath(bevel,reflection,style=Stroke(1.6.dp.toPx()))
    }
    if(!leading)return paintButton
    // Cache supersampled coverage so the joins do not acquire per-frame rasterization seams.
    val width=kotlin.math.ceil(size.width).toInt().coerceAtLeast(1)
    val height=kotlin.math.ceil(size.height).toInt().coerceAtLeast(1)
    val large=ImageBitmap(width*4,height*4)
    CanvasDrawScope().draw(this,layoutDirection,Canvas(large),size) { scale(4f,4f,pivot=Offset.Zero) { paintButton() } }
    fun half(source:ImageBitmap)=ImageBitmap(source.width/2,source.height/2).also { target ->
        Canvas(target).drawImageRect(source,IntOffset.Zero,IntSize(source.width,source.height),IntOffset.Zero,IntSize(target.width,target.height),Paint().apply { filterQuality=FilterQuality.Low })
    }
    val image=half(half(large))
    return { drawImage(image) }
}
