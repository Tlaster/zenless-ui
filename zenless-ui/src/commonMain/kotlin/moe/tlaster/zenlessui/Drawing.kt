package moe.tlaster.zenlessui

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.dp
import kotlin.math.min

internal fun roundedPath(rect: Rect, radius: Float, squareBottomRight: Boolean = false) = Path().apply {
    val r = CornerRadius(radius.coerceIn(0f, rect.minDimension / 2))
    addRoundRect(RoundRect(rect, r, r, if (squareBottomRight) CornerRadius.Zero else r, r))
}

private val checkerTile by lazy {
    val image=ImageBitmap(6,6)
    val canvas=Canvas(image)
    val coverage=arrayOf(
        intArrayOf(12,11,5,3,5,11),intArrayOf(4,5,10,12,11,6),intArrayOf(0,3,12,15,12,4),
        intArrayOf(3,5,11,12,11,5),intArrayOf(12,11,6,4,5,10),intArrayOf(15,12,4,0,3,12))
    val paint=Paint().apply { isAntiAlias=false }
    for(y in 0..5)for(x in 0..5) { paint.color=Color.White.copy(alpha=coverage[y][x]/15f);canvas.drawRect(Rect(x.toFloat(),y.toFloat(),x+1f,y+1f),paint) }
    image
}

internal fun checkerPaint(color:Color)=Paint().apply {
    shader=ImageShader(checkerTile,TileMode.Repeated,TileMode.Repeated)
    colorFilter=ColorFilter.tint(color)
    filterQuality=FilterQuality.None
}

internal fun DrawScope.checker(paint:Paint,width:Float=size.width,height:Float=size.height) {
    scale(density,density,pivot=Offset.Zero) { drawContext.canvas.drawRect(Rect(0f,0f,width/density,height/density),paint) }
}

internal fun DrawScope.layeredPlate(fill: Color, edge: Color, radius: Float, faceInset: Float, reflection: Boolean) {
    fun layer(d: Float, color: Color) = drawPath(roundedPath(Rect(Offset.Zero, size).deflate(d), radius - d), color)
    layer(0f, Color.Black)
    layer(1.dp.toPx(), edge)
    if (faceInset == 5.dp.toPx()) layer(4.dp.toPx(), Color.Black)
    layer(faceInset, fill)
    if (reflection) drawPath(roundedPath(Rect(Offset.Zero, size).deflate(1.5.dp.toPx()), radius - 1.5.dp.toPx()),
        Brush.verticalGradient(listOf(mix(edge, Color.White, .2f), edge), endY = 3.dp.toPx()), style = Stroke(1.dp.toPx()))
}

private val headerContour by lazy {
    val points=mutableListOf(Offset(42f,1f))
    fun line(x:Float,y:Float) { points.add(Offset(x,y)) }
    fun curve(a:Offset,b:Offset,c:Offset) {
        val from=points.last()
        for(i in 1..16) {
            val t=i/16f;val u=1-t
            points.add(from*(u*u*u)+a*(3*u*u*t)+b*(3*u*t*t)+c*(t*t*t))
        }
    }
    line(99f,1f);curve(Offset(119f,1f),Offset(124f,10f),Offset(117f,27f));line(104f,56f)
    curve(Offset(95f,73f),Offset(85f,79f),Offset(68f,79f));line(38f,79f)
    curve(Offset(13f,79f),Offset(0f,59f),Offset(2f,37f))
    curve(Offset(3f,15f),Offset(19f,1f),Offset(42f,1f))
    points.dropLast(1)
}

internal fun headerPath(rect: Rect, back: Boolean, outset: Float = 0f): Path = Path().apply {
    fun x(v: Float) = rect.left + (if (back) 120 - v else v) * rect.width / 120
    fun y(v: Float) = rect.top + v * rect.height / 80
    if(outset>0f) {
        fun point(index:Int)=headerContour[(index+headerContour.size)%headerContour.size].let { Offset(x(it.x),y(it.y)) }
        fun normalized(value:Offset)=value/value.getDistance().coerceAtLeast(.0001f)
        headerContour.indices.forEach { i ->
            val at=point(i);val a=normalized(at-point(i-1));val b=normalized(point(i+1)-at)
            val normal=Offset(a.y+b.y,-a.x-b.x)*(if(back)-1f else 1f)
            val expanded=at+normal*(outset/(1+a.x*b.x+a.y*b.y).coerceAtLeast(.1f))
            if(i==0)moveTo(expanded.x,expanded.y) else lineTo(expanded.x,expanded.y)
        }
        close()
        return@apply
    }
    moveTo(x(42f), y(1f)); lineTo(x(99f), y(1f))
    cubicTo(x(119f), y(1f), x(124f), y(10f), x(117f), y(27f)); lineTo(x(104f), y(56f))
    cubicTo(x(95f), y(73f), x(85f), y(79f), x(68f), y(79f)); lineTo(x(38f), y(79f))
    cubicTo(x(13f), y(79f), x(0f), y(59f), x(2f), y(37f))
    cubicTo(x(3f), y(15f), x(19f), y(1f), x(42f), y(1f)); close()
}

internal fun skewTabPath(rect: Rect, outset: Float = 0f): Path = Path().apply {
    val r = rect.inflate(outset)
    val a = min(10f + outset, r.height / 2)
    val k = .55228475f
    fun sx(x: Float, y: Float) = x - .3639702f * (y - r.center.y)
    fun line(x: Float, y: Float) = lineTo(sx(x, y), y)
    fun curve(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) = cubicTo(sx(x1,y1),y1,sx(x2,y2),y2,sx(x3,y3),y3)
    val l=r.left; val t=r.top; val b=r.bottom; val rr=r.right
    moveTo(sx(l+a,t),t); line(rr-a,t); curve(rr-a+k*a,t,rr,t+a-k*a,rr,t+a)
    line(rr,b-a); curve(rr,b-a+k*a,rr-a+k*a,b,rr-a,b); line(l+a,b)
    curve(l+a-k*a,b,l,b-a+k*a,l,b-a); line(l,t+a); curve(l,t+a-k*a,l+a-k*a,t,l+a,t); close()
}

internal fun DrawScope.metalKnob(center: Offset) {
    val metal = Color(0xff585858)
    fun ring(radius: Float, rotation: Float, stops: List<Float>, shades: List<Float>) {
        val colors = stops.zip(shades).map { (stop, shade) -> stop / 360f to mix(metal, if (shade >= 0) Color.White else Color.Black, kotlin.math.abs(shade)) }.toTypedArray()
        rotate(rotation - 90, center) { drawCircle(Brush.sweepGradient(*colors, center = center), radius.dp.toPx(), center) }
    }
    drawCircle(Color.Black, 12.dp.toPx(), center)
    ring(11f, 0f, listOf(0f,10f,90f,200f,230f,260f,320f,360f), listOf(0f,0f,-.7f,0f,-.3f,0f,.5f,0f))
    ring(8f,140f,listOf(0f,60f,170f,280f,360f),listOf(-.2f,0f,.8f,0f,-.2f))
    drawCircle(metal, 6.8.dp.toPx(), center)
    drawRoundRect(mix(metal,Color.White,.4f),center+Offset((-5.5).dp.toPx(),(-.5).dp.toPx()),Size(4.dp.toPx(),2.dp.toPx()),CornerRadius(1.dp.toPx()))
    drawRoundRect(mix(metal,Color.Black,.3f),center+Offset((-6).dp.toPx(),(-1).dp.toPx()),Size(4.dp.toPx(),2.dp.toPx()),CornerRadius(1.dp.toPx()))
}


// Match channel interpolation used by the plate artwork, rather than perceptual color interpolation.
internal fun mix(a: Color, b: Color, amount: Float): Color {
    val t=amount.coerceIn(0f,1f)
    return Color(a.red+(b.red-a.red)*t,a.green+(b.green-a.green)*t,a.blue+(b.blue-a.blue)*t,a.alpha+(b.alpha-a.alpha)*t)
}
