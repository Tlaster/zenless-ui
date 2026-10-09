package moe.tlaster.zenlessui

import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlin.test.*

class BehaviorTest {
    @Test fun artworkColorsUseChannelInterpolation() {
        val tint=mix(androidx.compose.ui.graphics.Color.Black,androidx.compose.ui.graphics.Color.White,.06f)
        assertEquals(.06f,tint.red,.002f)
        assertEquals(.06f,tint.green,.002f)
        assertEquals(.06f,tint.blue,.002f)
    }
    @Test fun mixedCheckboxSelectsAllInsteadOfCyclingToOff() {
        assertEquals(ToggleableState.On, nextCheckState(ToggleableState.Indeterminate))
        assertEquals(ToggleableState.Off, nextCheckState(ToggleableState.On))
        assertEquals(ToggleableState.On, nextCheckState(ToggleableState.Off))
    }
    @Test fun sliderClampsAndSnapsToActualRange() {
        assertEquals(-10f, sliderValue(-1f, -10f..10f, 3))
        assertEquals(10f, sliderValue(2f, -10f..10f, 3))
        assertEquals(0f, sliderValue(.53f, -10f..10f, 3))
        assertEquals(2.5f, sliderValue(.625f, -10f..10f, 0))
    }
    @Test fun popupFlipsAboveAndStaysInsideRightEdge() {
        val result = popupOffset(IntRect(260, 380, 310, 420), IntSize(180, 160), IntSize(320, 480), 8)
        assertEquals(132, result.x)
        assertEquals(212, result.y)
    }
    @Test fun breathingHasEarlyPeakPauseAndStablePeriod() {
        assertEquals(0f, Motion.pulse(0f), .001f)
        assertEquals(1f, Motion.pulse(.15f), .001f)
        assertEquals(0f, Motion.pulse(.4f), .001f)
        assertEquals(Motion.pulse(.62f), Motion.pulse(.62f + .64f), .001f)
        assertEquals(1f, Motion.color(.75f), .001f)
        assertEquals(0f, Motion.color(1.5f), .001f)
    }
    @Test fun releaseFlashEndsAndNeverLeavesVisualResidue() {
        assertEquals(0f, Motion.release(0f))
        assertTrue(Motion.release(.04f) > .9f)
        assertTrue(Motion.release(.11f) > .9f)
        assertEquals(0f, Motion.release(.15f))
        assertEquals(0f, Motion.release(1f))
    }
    @Test fun alertClosesTextThenBandThenBackground() {
        val afterText = Motion.modal(1 - .08f / .22f, true)
        assertEquals(0f, afterText.ink, .001f)
        assertTrue(afterText.band > 0f && afterText.effect > afterText.band)
        val closed = Motion.modal(0f, true)
        assertEquals(0f, closed.band); assertEquals(0f, closed.effect)
        val open = Motion.modal(1f, false)
        assertEquals(1f, open.ink); assertEquals(1f, open.band); assertEquals(0f, open.flash)
    }
}
