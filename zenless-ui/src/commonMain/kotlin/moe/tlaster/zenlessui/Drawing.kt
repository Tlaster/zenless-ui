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
    for(y in 0..5)for(x in 0..5) { paint.color=Color.White.copy(alpha=coverage[y][(x+4)%6]/15f);canvas.drawRect(Rect(x.toFloat(),y.toFloat(),x+1f,y+1f),paint) }
    image
}

internal fun checkerPaint(color:Color)=Paint().apply {
    shader=ImageShader(checkerTile,TileMode.Repeated,TileMode.Repeated)
    colorFilter=ColorFilter.tint(color)
    filterQuality=FilterQuality.None
}

internal fun DrawScope.checker(paint:Paint,width:Float=size.width,height:Float=size.height) {
    val w=width/density;val h=height/density
    // Center the repeated tile so resizing a plate preserves its texture phase.
    val x=(w-6)/2%6;val y=(h-6)/2%6
    scale(density,density,pivot=Offset.Zero) {
        translate(x,y) { drawContext.canvas.drawRect(Rect(-x,-y,w-x,h-y),paint) }
    }
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

internal fun DrawScope.buttonShell(fill:Color,edge:Color,round:Boolean,pattern:Boolean) {
    val bounds=Rect(.5.dp.toPx(),.5.dp.toPx(),size.width-.5.dp.toPx(),size.height)
    val radius=if(round)bounds.minDimension/2 else 6.dp.toPx()
    val inset=size.minDimension*.0835f
    val face=roundedPath(bounds.deflate(inset),radius-inset)
    drawPath(roundedPath(bounds,radius),edge)
    drawPath(face,fill)
    val circular=size.width<size.height*1.1f
    drawPath(roundedPath(bounds.deflate(.8.dp.toPx()),radius-.8.dp.toPx()),Brush.linearGradient(
        0f to Color(0xff3d3d3d),(if(circular).6f else .67f) to Color(0xff3d3d3d),1f to edge,
        end=Offset(size.width*(if(circular).22f else .0265f),size.height*.672f)),style=Stroke(1.6.dp.toPx()))
    if(pattern)clipPath(face) {
        if(fill==Color.Black)buttonTexture() else checker(checkerPaint(mix(fill,Color.White,.06f)))
    }
}

internal fun DrawScope.buttonTexture() = scale(density*.08f,density*.08f,pivot=Offset.Zero) {
    translate(-55f,-24f) { drawContext.canvas.drawRect(Rect(55f,24f,55f+size.width/(density*.08f),24f+size.height/(density*.08f)),backCheckerPaint) }
}

// Measured in the navigation button's 90 by 60 logical coordinate space.
private val closeOuter = floatArrayOf(
    31.235f,0.332f,
    46.256f,0.333f,59.491f,0.333f,74.613f,0.334f,
    87.621f,0.332f,91.275f,10.334f,87.954f,19.498f,
    84.094f,28.867f,81.396f,33.444f,76.569f,41.999f,
    71.645f,52.272f,61.290f,58.306f,51.324f,58.306f,
    43.128f,58.306f,35.912f,58.306f,28.738f,58.305f,
    12.375f,58.305f,-0.471f,43.742f,0.308f,27.981f,
    2.000f,10.494f,14.095f,0.331f,31.235f,0.332f,
)
internal val closeRing = floatArrayOf(
    32.485f,5.458f,
    46.467f,5.459f,58.112f,5.459f,70.939f,5.458f,
    85.537f,5.458f,85.859f,11.710f,81.557f,21.142f,
    78.499f,27.488f,75.852f,32.721f,72.055f,39.326f,
    68.084f,47.575f,59.825f,53.201f,50.527f,53.201f,
    43.251f,53.201f,37.462f,53.201f,30.076f,53.201f,
    15.568f,53.201f,5.193f,41.623f,5.614f,28.342f,
    7.142f,13.546f,17.439f,5.458f,32.485f,5.458f,
)
internal val closeFace = floatArrayOf(
    31.334f,7.903f,
    48.999f,7.904f,55.029f,7.904f,70.571f,7.904f,
    81.458f,7.902f,85.543f,8.662f,78.913f,22.448f,
    77.090f,26.503f,73.494f,32.915f,70.643f,38.146f,
    66.063f,46.687f,59.833f,51.062f,50.507f,51.062f,
    42.543f,51.061f,39.206f,51.061f,29.653f,51.063f,
    16.571f,51.063f,7.699f,41.266f,7.313f,28.787f,
    9.768f,12.544f,18.878f,7.901f,31.334f,7.903f,
)
private val backOuter = floatArrayOf(
    14.25f,1.02f,
    29.58f,1.02f,44.92f,1.02f,60.25f,1.02f,
    76.93f,.78f,89.68f,14.28f,89.745f,30.75f,
    88.78f,46.77f,76.54f,59.01f,60.25f,58.90f,
    53.25f,58.90f,46.25f,58.90f,39.25f,58.90f,
    30.93f,58.99f,22.04f,54.91f,17.01f,48.25f,
    11.34f,39.81f,6.37f,30.24f,2.25f,21.04f,
    1.05f,18.76f,.99f,16.77f,.664f,14.25f,
    .674f,6.20f,6.095f,.673f,14.25f,1.02f,
)
private val backInner = floatArrayOf(
    13.84f,6.25f,
    29.30f,6.07f,44.78f,6.22f,60.25f,6.17f,
    73.73f,6.37f,84.71f,17.07f,84.30f,30.75f,
    83.95f,43.59f,73.40f,53.38f,60.75f,53.834f,
    53.58f,53.82f,46.42f,53.84f,39.25f,53.825f,
    32.87f,53.48f,27.20f,51.28f,22.75f,46.69f,
    16.68f,39.53f,12.33f,29.50f,8.25f,21.01f,
    7.30f,18.96f,6.02f,16.57f,6.071f,14.25f,
    6.11f,8.95f,8.92f,6.98f,13.84f,6.25f,
)
internal fun backPlatePath(rect: Rect, face: Boolean = false): Path = navigationPlatePath(rect,if(face)backInner else backOuter)
internal fun navigationPlatePath(rect: Rect, p: FloatArray): Path = Path().apply {
    fun x(v:Float)=rect.left+v*rect.width/90
    fun y(v:Float)=rect.top+v*rect.height/60
    moveTo(x(p[0]),y(p[1]))
    for(i in 2 until p.size step 6) cubicTo(x(p[i]),y(p[i+1]),x(p[i+2]),y(p[i+3]),x(p[i+4]),y(p[i+5]))
    close()
}
private val backContour by lazy { navigationContour(backOuter) }
private val closeContour by lazy { navigationContour(closeOuter) }
private fun navigationContour(p:FloatArray):List<Offset> {
    val points=mutableListOf(Offset(p[0],p[1]))
    for(i in 2 until p.size step 6) {
        val from=points.last();val a=Offset(p[i],p[i+1]);val b=Offset(p[i+2],p[i+3]);val c=Offset(p[i+4],p[i+5])
        for(j in 1..16) { val t=j/16f;val u=1-t;points.add(from*(u*u*u)+a*(3*u*u*t)+b*(3*u*t*t)+c*(t*t*t)) }
    }
    return points.dropLast(1)
}
internal val backCheckerPaint by lazy { navigationCheckerPaint(Color.Black,Color(0xff090909)) }
internal val closeCheckerPaint by lazy { navigationCheckerPaint(Color(0xffc50600),Color(0xffcb1100)) }
private fun navigationCheckerPaint(dark:Color,light:Color):Paint {
    // Bake the soft cell edges because repeated image shaders use nearest sampling on Skia.
    val tile=ImageBitmap(60,60);val canvas=Canvas(tile)
    val coverage=FloatArray(60) { i ->
        val t=i+.5f;val distance=minOf(t,kotlin.math.abs(t-30),60-t)
        (.5f+(if(t<30)distance else -distance)/9.5f).coerceIn(0f,1f)
    }
    val paint=Paint().apply { isAntiAlias=false }
    for(y in 0..59)for(x in 0..59) {
        val a=coverage[x];val b=coverage[y]
        paint.color=mix(dark,light,a*b+(1-a)*(1-b))
        canvas.drawRect(Rect(x.toFloat(),y.toFloat(),x+1f,y+1f),paint)
    }
    return Paint().apply { shader=ImageShader(tile,TileMode.Repeated,TileMode.Repeated) }
}
internal fun backArrowPath(rect:Rect):Path=Path().apply {
    fun x(v:Float)=rect.left+v*rect.width/90
    fun y(v:Float)=rect.top+v*rect.height/60
    moveTo(x(39.457f),y(13.25f))
    cubicTo(x(40.192f),y(14.708f),x(39.386f),y(16.51f),x(40.074f),y(17.75f))
    cubicTo(x(43.415f),y(17.938f),x(46.87f),y(17.75f),x(50.25f),y(17.884f))
    cubicTo(x(57.101f),y(17.386f),x(63.536f),y(23.918f),x(62.914f),y(30.75f))
    cubicTo(x(63.244f),y(37.558f),x(56.523f),y(43.714f),x(49.75f),y(42.904f))
    cubicTo(x(46.443f),y(42.819f),x(42.87f),y(43.104f),x(39.639f),y(42.75f))
    cubicTo(x(39.291f),y(40.901f),x(39.554f),y(38.667f),x(39.502f),y(36.75f))
    cubicTo(x(41.476f),y(35.524f),x(47.519f),y(36.427f),x(50.25f),y(36.087f))
    cubicTo(x(53.616f),y(35.771f),x(55.58f),y(34.169f),x(56.038f),y(30.75f))
    cubicTo(x(55.963f),y(26.815f),x(53.648f),y(25.167f),x(49.982f),y(24.75f))
    cubicTo(x(46.791f),y(24.814f),x(42.865f),y(24.248f),x(39.873f),y(24.75f))
    cubicTo(x(39.581f),y(25.945f),x(40.122f),y(27.686f),x(39.514f),y(28.75f))
    cubicTo(x(35.965f),y(27.727f),x(30.815f),y(23.879f),x(27.164f),y(21.75f))
    cubicTo(x(29.073f),y(18.746f),x(36.353f),y(15.486f),x(39.457f),y(13.25f))
    close()
}

internal fun headerPath(rect: Rect, back: Boolean, outset: Float = 0f): Path = Path().apply {
    fun x(v: Float) = rect.left + v * rect.width / 90
    fun y(v: Float) = rect.top + v * rect.height / 60
    if(outset!=0f) {
        val contour=if(back)backContour else closeContour
        fun point(index:Int)=contour[(index+contour.size)%contour.size].let { Offset(x(it.x),y(it.y)) }
        fun normalized(value:Offset)=value/value.getDistance().coerceAtLeast(.0001f)
        contour.indices.forEach { i ->
            val at=point(i);val a=normalized(at-point(i-1));val b=normalized(point(i+1)-at)
            val normal=Offset(a.y+b.y,-a.x-b.x)
            val expanded=at+normal*(outset/(1+a.x*b.x+a.y*b.y).coerceAtLeast(.1f))
            if(i==0)moveTo(expanded.x,expanded.y) else lineTo(expanded.x,expanded.y)
        }
        close()
        return@apply
    }
    addPath(navigationPlatePath(rect,if(back)backOuter else closeOuter))
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
