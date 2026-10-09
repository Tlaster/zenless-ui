package moe.tlaster.zenlessui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

/** Fixed semantic variants; applications cannot replace the palette. */
public enum class ZenlessTone { Neutral, Accent, Primary, Success, Warning, Danger, Info }
public enum class ZenlessSize { Mini, Small, Default, Large, Extra }
public enum class ZenlessButtonVariant { Filled, Plain, Hollow }
public enum class ZenlessTextStyle { Title, Subtitle, Body, Caption, Number }

internal object Palette {
    val background = Color(0xff101012)
    val panel = Color(0xff19191c)
    val muted = Color(0xff9d9da4)
    val line = Color(0xff444449)
    val signal = Color(0xffffea00)
    fun tone(tone: ZenlessTone): Color = when (tone) {
        ZenlessTone.Neutral -> Color(0xff333333)
        ZenlessTone.Accent -> signal
        ZenlessTone.Primary -> Color(0xff008bff)
        ZenlessTone.Success -> Color(0xff00cc0d)
        ZenlessTone.Warning -> Color(0xffffc300)
        ZenlessTone.Danger -> Color(0xffc01c00)
        ZenlessTone.Info -> Color(0xffcccccc)
    }
}
internal val LocalInk = compositionLocalOf { Color.White }
internal val LocalTextStyle = compositionLocalOf { TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Default) }

/** The fixed dark theme. Put [ZenlessOverlayHost] inside this theme when using overlays. */
@Composable
public fun ZenlessTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalInk provides Color.White, LocalTextStyle provides TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Default)) {
        content()
    }
}

@Composable
public fun ZenlessText(
    text: String,
    modifier: Modifier = Modifier,
    style: ZenlessTextStyle = ZenlessTextStyle.Body,
    maxLines: Int = Int.MAX_VALUE,
) {
    val base = LocalTextStyle.current
    val typography = when (style) {
        ZenlessTextStyle.Title -> base.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold)
        ZenlessTextStyle.Subtitle -> base.copy(fontSize = 21.sp, fontWeight = FontWeight.Bold)
        ZenlessTextStyle.Body -> base
        ZenlessTextStyle.Caption -> base.copy(fontSize = 12.sp)
        ZenlessTextStyle.Number -> base.copy(fontSize = 36.sp, fontWeight = FontWeight.Bold)
    }
    BasicText(text, modifier, typography.copy(color = if (style == ZenlessTextStyle.Caption) Palette.muted else LocalInk.current),
        maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@Composable
public fun ZenlessSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.background(Palette.background)) { content() }
}
