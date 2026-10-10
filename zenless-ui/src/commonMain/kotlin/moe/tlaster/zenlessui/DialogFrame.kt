package moe.tlaster.zenlessui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.LayoutDirection

private fun frameRect(size: Size, unit: Float, inset: Float, rtl: Boolean): RoundRect {
    // The native capture's silhouette falls between pixels, rather than on the layout bounds.
    val rect = Rect(-.125f * unit, unit / 3, size.width + .125f * unit, size.height - unit / 3).deflate(inset)
    val corner = CornerRadius((31.5f * unit - inset).coerceIn(0f, rect.minDimension.coerceAtLeast(0f) / 2))
    return RoundRect(rect, if (rtl) CornerRadius.Zero else corner, if (rtl) corner else CornerRadius.Zero, corner, corner)
}

internal fun Modifier.dialogFrame(scale: Float, opacity: () -> Float = { 1f }): Modifier = drawWithCache {
    val unit = density * scale
    val rtl = layoutDirection == LayoutDirection.Rtl
    val outer = Path().apply { addRoundRect(frameRect(size, unit, 0f, rtl)) }
    val rim = Path().apply { addRoundRect(frameRect(size, unit, -5 * unit, rtl)) }
    val layerBounds = Rect(-unit, -unit, size.width + unit, size.height + unit)
    val layerPaint = Paint()
    val fadePaint = Paint()
    val fadeBounds = Rect(-6 * unit, -6 * unit, size.width + 6 * unit, size.height + 6 * unit)
    // Coverage bands soften the face join without blurring text or the outer silhouette.
    val rings = listOf(5.5f to .04f, 5f to .25f, 4.5f to .78f, 4f to 1f).map { (inset, alpha) ->
        Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(layerBounds)
            addRoundRect(frameRect(size, unit, inset * unit, rtl))
        } to Color.Black.copy(alpha = alpha)
    }
    onDrawWithContent {
        fadePaint.alpha = opacity()
        val fading = fadePaint.alpha < 1f
        // Explicit bounds retain the external rim during a translated fade; layout-sized layers crop it.
        if (fading) drawContext.canvas.saveLayer(fadeBounds, fadePaint)
        try {
            drawPath(rim, Color(0xff393939).copy(alpha = .4f))
            clipPath(outer) {
                // Composite once at the silhouette: repeated AA clipping darkens the same edge pixel.
                drawContext.canvas.withSaveLayer(layerBounds, layerPaint) {
                    drawRect(Color(0xff131313))
                    this@onDrawWithContent.drawContent()
                    rings.forEach { (path, color) -> drawPath(path, color) }
                }
            }
        } finally {
            if (fading) drawContext.canvas.restore()
        }
    }
}
