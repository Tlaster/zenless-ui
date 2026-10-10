package moe.tlaster.zenlessui

import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Test-only comparison modes, including the production CachedDirect renderer. */
internal enum class ButtonPrototype { Baseline, NativeLayers, Direct, DirectSoft, CachedDirect }

internal fun CacheDrawScope.prototypeShell(
    mode: ButtonPrototype, fill: Color, edge: Color, round: Boolean, pattern: Boolean,
): DrawScope.() -> Unit {
    if (mode == ButtonPrototype.Baseline) return supersampledButtonReference(fill, edge, round, pattern, leading = true)
    if (mode == ButtonPrototype.CachedDirect) return buttonShell(fill, edge, round, pattern, leading = true)
    val bounds = Rect(.5.dp.toPx(), .5.dp.toPx(), size.width - .5.dp.toPx(), size.height)
    val radius = if (round) bounds.minDimension / 2 else 6.dp.toPx()
    val inset = size.minDimension * .0835f
    val face = roundedPath(bounds.deflate(inset).copy(top = size.height * 10.684f / 116, bottom = size.height * 106.50f / 116), radius - inset)
    val outline = roundedPath(bounds, radius)
    val bevel = roundedPath(bounds.deflate(.8.dp.toPx()), radius - .8.dp.toPx())
    val circular = size.width < size.height * 1.1f
    val light = Offset(1.35f / size.width, 1.52f / size.height)
    val lightStart = Offset(0f, size.height * .21f)
    val reflection = if (circular) Brush.linearGradient(
        0f to Color(0xff3d3d3d), .6f to Color(0xff3d3d3d), 1f to edge,
        end = Offset(size.width * .22f, size.height * .672f),
    ) else Brush.linearGradient(
        0f to Color(0xff3d3d3d), .67f to Color(0xff3d3d3d), .8f to Color(0xff383838),
        .9f to Color(0xff2e2e2e), 1f to edge,
        start = lightStart, end = lightStart + light / (light.x * light.x + light.y * light.y),
    )
    val unit = size.height / 116f
    val innerBounds = Rect(11.51f * unit, 10.85f * unit, 106.39f * unit, 106.34f * unit)
    val outerBounds = Rect(8.48f * unit, 2.53f * unit, 116f * unit, 114.64f * unit)
    val rightHalf = Path().apply { addRect(Rect(size.height / 2, 0f, size.width, size.height)) }
    val leftHalf = Path().apply { addRect(Rect(0f, 0f, size.height / 2, size.height)) }
    val spreads = if (mode == ButtonPrototype.Direct) listOf(0f to 1f)
        else listOf(.6f to .12f, .3f to .2816f, 0f to .41888f, -.3f to .17952f)
    val left = mutableListOf<Pair<Path, Float>>()
    val right = mutableListOf<Pair<Path, Float>>()
    for ((spread, weight) in spreads) {
        val offset = spread.dp.toPx()
        val inner = Path().apply { addOval(innerBounds.deflate(offset)) }
        val outer = Path().apply { addOval(outerBounds.inflate(offset)) }
        left += Path.combine(PathOperation.Intersect, face, inner) to weight
        var body = Path.combine(PathOperation.Intersect, rightHalf, Path.combine(PathOperation.Difference, face, outer))
        if (!round) body = Path.combine(PathOperation.Union, body,
            Path.combine(PathOperation.Intersect, leftHalf, Path.combine(PathOperation.Difference, face, inner)))
        right += body to weight
    }
    if (mode == ButtonPrototype.NativeLayers) {
        val dark = Paint().apply { shader = backCheckerPaint.shader; blendMode = BlendMode.SrcAtop }
        val checks = checkerPaint(mix(fill, Color.White, .06f)).apply { blendMode = BlendMode.SrcAtop }
        val layerPaint = Paint()
        return {
            drawPath(outline, edge)
            fun masked(paths: List<Pair<Path, Float>>, color: Color) {
                drawContext.canvas.saveLayer(Rect(Offset.Zero, size), layerPaint)
                paths.forEach { (path, weight) -> drawPath(path, color.copy(alpha = weight), blendMode = BlendMode.Plus) }
                if (pattern) { if (color == Color.Black) buttonTexture(dark) else checker(checks) }
                drawContext.canvas.restore()
            }
            scale(if (layoutDirection == LayoutDirection.Rtl) -1f else 1f, 1f) {
                masked(left, Color.Black); masked(right, fill)
            }
            drawPath(bevel, reflection, style = Stroke(1.6.dp.toPx()))
        }
    }

    fun directFace(paths: List<Pair<Path, Float>>, color: Color): DrawScope.() -> Unit {
        val factor = if (color == Color.Black) density * .08f else density
        val shift = if (color == Color.Black) Offset(-55f, -24f)
            else Offset((size.width / density - 6) / 2 % 6, (size.height / density - 6) / 2 % 6)
        val shader = if (!pattern) null else if (color == Color.Black) backCheckerPaint.shader else {
            val tile = ImageBitmap(6, 6)
            val canvas = Canvas(tile)
            val rect = Rect(0f, 0f, 6f, 6f)
            canvas.drawRect(rect, Paint().apply { this.color = color; isAntiAlias = false })
            canvas.drawRect(rect, checkerPaint(mix(color, Color.White, .06f)))
            ImageShader(tile, TileMode.Repeated, TileMode.Repeated)
        }
        var coverage = 0f
        val bands = paths.asReversed().map { (path, weight) ->
            coverage = (coverage + weight).coerceAtMost(1f)
            val c = coverage
            val paint = Paint().apply {
                if (shader == null) this.color = mix(edge, color, c) else {
                    this.shader = shader
                    // Encode the soft-edge coverage in the opaque fill, without an offscreen mask.
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                        c, 0f, 0f, 0f, edge.red * (1 - c) * 255,
                        0f, c, 0f, 0f, edge.green * (1 - c) * 255,
                        0f, 0f, c, 0f, edge.blue * (1 - c) * 255,
                        0f, 0f, 0f, 1f, 0f,
                    )))
                }
            }
            val transformed = if (!pattern) path else Path().apply {
                addPath(path)
                transform(Matrix().apply { scale(1 / factor, 1 / factor) })
                translate(-shift)
            }
            transformed to paint
        }
        return {
            if (pattern) scale(factor, factor, pivot = Offset.Zero) {
                translate(shift.x, shift.y) { bands.forEach { (path, paint) -> drawContext.canvas.drawPath(path, paint) } }
            } else bands.forEach { (path, paint) -> drawContext.canvas.drawPath(path, paint) }
        }
    }
    val drawLeft = directFace(left, Color.Black)
    val drawRight = directFace(right, fill)
    return {
        drawPath(outline, edge)
        scale(if (layoutDirection == LayoutDirection.Rtl) -1f else 1f, 1f) { drawLeft(); drawRight() }
        drawPath(bevel, reflection, style = Stroke(1.6.dp.toPx()))
    }
}
