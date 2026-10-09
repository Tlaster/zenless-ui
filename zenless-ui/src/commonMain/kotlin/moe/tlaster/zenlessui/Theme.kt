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
public enum class ZenlessTextStyle { Inherit, Title, Subtitle, Body, Caption, Number }

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
internal val LocalButtonHighlight = compositionLocalOf { 0f }
internal val LocalTextStyle = compositionLocalOf { TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Default) }

/** Read the current text/icon ink inside a button content slot, including its press feedback. */
public val zenlessContentColor: Color
    @Composable @ReadOnlyComposable get() = LocalInk.current

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
    style: ZenlessTextStyle = ZenlessTextStyle.Inherit,
    maxLines: Int = Int.MAX_VALUE,
) {
    val base = LocalTextStyle.current
    val typography = when (style) {
        ZenlessTextStyle.Title -> base.copy(fontSize = 42.sp, fontWeight = FontWeight.Bold)
        ZenlessTextStyle.Subtitle -> base.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold)
        ZenlessTextStyle.Inherit -> base
        ZenlessTextStyle.Body -> base.copy(fontSize = 24.sp)
        ZenlessTextStyle.Caption -> base.copy(fontSize = 20.sp)
        ZenlessTextStyle.Number -> base.copy(fontSize = 44.sp, fontWeight = FontWeight.Bold)
    }
    BasicText(text, modifier, typography.copy(color = if (style == ZenlessTextStyle.Caption) mix(Palette.muted,Color.Black,LocalButtonHighlight.current) else LocalInk.current),
        maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@Composable
public fun ZenlessSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.background(Palette.background)) { content() }
}
