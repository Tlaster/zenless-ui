package moe.tlaster.zenlessui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalWindowInfo
import kotlin.math.abs

/** Shared timing curves; attribution is recorded in THIRD_PARTY_NOTICES.md. */
internal object Motion {
    val smoothEasing=Easing { smooth(it) }
    private fun smooth(value: Float): Float { val x = value.coerceIn(0f, 1f); return x * x * (3 - 2 * x) }
    fun modal(progress: Float, closing: Boolean): ModalFrame {
        val t = if (closing) (1 - progress) * .22f else progress * .25f
        return if (closing) ModalFrame(1 - smooth((t - .045f) / .15f), 1 - smooth((t - .035f) / .11f), 1 - smooth(t / .08f), 1f, 0f)
        else ModalFrame(smooth(t / .25f), smooth(t / .12f), smooth((t - .055f) / .09f), smooth((t - .06f) / .1f), 1 - smooth((t - .04f) / .12f))
    }
    fun bezier(x: Float, x1: Float, x2: Float): Float {
        if (x <= 0f || x >= 1f) return x.coerceIn(0f, 1f)
        var low = 0f; var high = 1f; var t = .5f
        repeat(14) {
            t = (low + high) / 2
            val u = 1 - t
            val at = 3 * u * u * t * x1 + 3 * u * t * t * x2 + t * t * t
            if (at < x) low = t else high = t
        }
        return t * t * (3 - 2 * t)
    }
    fun signalColor(seconds: Float): Float = bezier(1 - abs(seconds % 2f - 1), .42f, .58f)
    fun color(seconds: Float): Float = bezier(1 - abs((seconds % 1.5f) / .75f - 1), .42f, .58f)
    fun pulse(seconds: Float): Float {
        if (seconds < .15f) return bezier(seconds / .15f, .42f, 1f)
        val t = (seconds - .15f) % .64f
        return when { t < .15f -> (1 - t / .15f) * (1 - t / .15f); t < .4f -> 0f; else -> bezier((t - .4f) / .24f, .42f, 1f) }
    }
    fun release(seconds: Float): Float {
        if (seconds !in 0f..<.15f) return 0f
        fun smooth(v: Float): Float { val x = v.coerceIn(0f, 1f); return x * x * (3 - 2 * x) }
        val t = seconds % .07f
        return smooth((t - .025f) / .01f) * (1 - smooth((t - .06f) / .01f))
    }
}

internal data class ModalFrame(val effect: Float, val band: Float, val ink: Float, val artwork: Float, val flash: Float)

internal class Feedback {
    var seconds by mutableFloatStateOf(0f)
    var release by mutableFloatStateOf(-1f)
    var active by mutableStateOf(false)
    var visible by mutableStateOf(true)
    val highlight: Float get() = if (release >= 0f) Motion.release(release) else if (active) 1f else 0f
}

@Composable
internal fun rememberFeedback(source: MutableInteractionSource, selected: Boolean, enabled: Boolean, ambient: Boolean = false, buttonFeedback: Boolean = false): Feedback {
    val focused = !buttonFeedback || LocalWindowInfo.current.isWindowFocused
    val state = remember { Feedback() }
    var interaction by remember { mutableStateOf<PressInteraction?>(null) }
    LaunchedEffect(source, buttonFeedback, enabled, focused) {
        interaction=null
        if (!buttonFeedback || !enabled || !focused) return@LaunchedEffect
        source.interactions.collect { event -> if (event is PressInteraction) interaction=event }
    }
    var processedInteraction by remember { mutableStateOf<PressInteraction?>(null) }
    LaunchedEffect(interaction, selected, enabled, focused, ambient, state.visible) {
        val changed=interaction!==processedInteraction
        processedInteraction=interaction
        if (!enabled || !focused || !state.visible) { state.active = false; state.release = -1f; state.seconds = 0f; return@LaunchedEffect }
        if (changed && interaction is PressInteraction.Release) {
            val start = withFrameNanos { it }
            do { state.release = withFrameNanos { (it - start) / 1_000_000_000f } } while (state.release < .15f)
        }
        state.release = -1f
        val active = interaction is PressInteraction.Press || selected
        if (active || ambient) {
            if (!state.active || (changed && interaction is PressInteraction.Press)) state.seconds = 0f
            state.active = active
            var last = withFrameNanos { it }
            while (true) { withFrameNanos { now -> state.seconds += (now - last) / 1_000_000_000f; last = now } }
        } else { state.active = false; state.seconds = 0f }
    }
    return state
}
