package moe.tlaster.zenlessui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Image
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SwitchTest {
    @Test fun referenceStatesAndStagedMotion() = runDesktopComposeUiTest(width = 284, height = 124) {
        mainClock.autoAdvance = false
        var checked by mutableStateOf(false)
        setContent { CompositionLocalProvider(LocalControlSize provides ZenlessSize.Comfortable, LocalDensity provides Density(2f)) {
            Box(Modifier.fillMaxSize().background(Color.Black)) { ZenlessSwitch(checked, { checked = it }, Modifier.testTag("switch")) }
        } }
        mainClock.advanceTimeByFrame()
        val control = onNodeWithTag("switch")
        control.assertIsOff().assertWidthIsEqualTo(142.dp).assertHeightIsEqualTo(62.dp)
        assertEquals(Color(0xff272727), control.captureToImage().toPixelMap()[130,35])
        val leftHighlight = highlightX(control.captureToImage().toPixelMap())
        save("off")
        control.performTouchInput { down(center) }
        mainClock.advanceTimeBy(300)
        control.assertIsOff()
        assertEquals(Color(0xff272727), control.captureToImage().toPixelMap()[130,35], "Holding does not turn the switch yellow")
        control.performTouchInput { up() }
        mainClock.advanceTimeByFrame()
        control.assertIsOn()
        repeat(12) { index ->
            mainClock.advanceTimeByFrame()
            save("on-${(index + 1) * 16}")
            if (index == 2) {
                val pixel = control.captureToImage().toPixelMap()[130,35]
                assertEquals(pixel.red, pixel.green, "Green waits for the thumb to leave")
                val travel = (highlightX(control.captureToImage().toPixelMap()) - leftHighlight) / 173f
                assertTrue(travel in .36f.. .5f, "Opening moves about 43% of the travel in the first 32 ms")
            }
        }
        assertEquals(Color(0xff36a100), control.captureToImage().toPixelMap()[130,35])
        val on = control.captureToImage().toPixelMap()
        assertTrue(kotlin.math.abs(highlightX(on) - leftHighlight - 173f) <= 1f, "Reference thumb travel is 173 pixels")
        assertEquals(Color.Black, on[118,55], "The ON artwork remains black")
        for ((y, left, right) in listOf(Triple(32,47,199), Triple(38,39,194), Triple(50,31,188), Triple(62,29,186), Triple(80,34,191), Triple(94,51,202))) {
            val greenPixels = (0 until on.width).filter { on[it,y].green > 100 / 255f && on[it,y].red < 80 / 255f }
            assertTrue(kotlin.math.abs(greenPixels.first() - left) <= 1 && kotlin.math.abs(greenPixels.last() - right) <= 1, "Reference track contour at row $y")
        }
        save("on")
        control.performTouchInput { click() }
        mainClock.advanceTimeByFrame()
        repeat(12) { index ->
            mainClock.advanceTimeByFrame(); save("off-${(index + 1) * 16}")
            if (index == 2) {
                val pixels = control.captureToImage().toPixelMap()
                assertTrue(kotlin.math.abs(highlightX(pixels) - highlightX(on)) <= 1f, "Closing fades green before moving the thumb")
                assertTrue(pixels[130,35].green < .17f, "Closing has faded to gray after 32 ms")
            }
        }
        control.assertIsOff()
        assertEquals(Color(0xff272727), control.captureToImage().toPixelMap()[130,35])
    }

    private fun highlightX(pixels: PixelMap): Float {
        val highlights = (0 until pixels.width).filter { pixels[it,44].red > .55f }
        check(highlights.isNotEmpty())
        return highlights.average().toFloat()
    }

    @Test fun longLabelsWrapAtNarrowWidthsAndLargeFontScale() = runDesktopComposeUiTest(width = 320, height = 260) {
        mainClock.autoAdvance = false
        setContent { CompositionLocalProvider(LocalControlSize provides ZenlessSize.Comfortable, LocalDensity provides Density(1f, 1.5f)) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                ZenlessSwitch(true, {}, Modifier.testTag("switch"), label = "Controls enabled")
            }
        } }
        mainClock.advanceTimeByFrame()
        val control = onNodeWithTag("switch").fetchSemanticsNode().boundsInRoot
        val label = onNodeWithText("Controls enabled", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(control.right <= 320f && control.bottom <= 260f)
        assertTrue(label.right <= control.right - 142f && label.bottom <= control.bottom)
        save("narrow-large-text")
    }

    @Test fun controlledDisabledAndInterruptedSwitches() = runDesktopComposeUiTest(width = 620, height = 360) {
        mainClock.autoAdvance = false
        var checked by mutableStateOf(false)
        var changes = 0
        setContent { CompositionLocalProvider(LocalControlSize provides ZenlessSize.Comfortable, LocalDensity provides Density(2f)) {
            Column(Modifier.fillMaxSize().background(Color.Black)) {
                ZenlessSwitch(checked, { checked = it; changes++ }, Modifier.testTag("switch"), label = "屏蔽邀请")
                ZenlessSwitch(false, { changes++ }, Modifier.testTag("disabled"), enabled = false, label = "屏蔽邀请")
            }
        } }
        mainClock.advanceTimeByFrame()
        save("labeled")
        val control = onNodeWithTag("switch")
        val label = onAllNodesWithText("屏蔽邀请", useUnmergedTree = true)[0].fetchSemanticsNode().boundsInRoot
        assertTrue(label.right < control.fetchSemanticsNode().boundsInRoot.right - 284, "The label precedes the track")
        onNodeWithTag("disabled").assertIsNotEnabled().performTouchInput { click() }
        assertEquals(0, changes)
        control.performTouchInput { click() }
        mainClock.advanceTimeBy(64)
        control.performTouchInput { click() }
        mainClock.advanceTimeBy(240)
        control.assertIsOff()
        assertEquals(2, changes)
        runOnIdle { checked = true }
        mainClock.advanceTimeBy(240)
        control.assertIsOn()
        assertEquals(2, changes, "Caller updates must not emit callbacks")
        save("labeled-on")
        control.performTouchInput { down(center); moveTo(center.copy(x = -100f)); up() }
        mainClock.advanceTimeBy(240)
        assertEquals(2, changes, "Cancelled gestures do not toggle")
    }

    private fun DesktopComposeUiTest.save(name: String) {
        val file = File("../verification/toggle/render-$name.png"); file.parentFile.mkdirs()
        Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image -> image.encodeToData()?.use { file.writeBytes(it.bytes) } }
    }
}
