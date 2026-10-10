package moe.tlaster.zenlessui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.*
import org.jetbrains.skia.Image
import org.junit.Test
import java.io.File
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class CheckboxTest {
    @Test fun referenceGlyphsPressRimAndAsymmetricMotion() = runDesktopComposeUiTest(width = 350, height = 120) {
        mainClock.autoAdvance = false
        var state by mutableStateOf(ToggleableState.Off)
        var label by mutableStateOf("")
        var changes = 0
        setContent { CompositionLocalProvider(LocalDensity provides Density(2f)) { ZenlessTheme {
            Box(Modifier.fillMaxSize().background(Color(0xff222222))) {
                ZenlessCheckbox(state, { state = it; changes++ }, Modifier.offset(14.dp, 1.dp)
                    .then(if (label.isEmpty()) Modifier.width(140.dp) else Modifier).testTag("check"), label = label)
            }
        } } }
        mainClock.advanceTimeBy(32)
        val control = onNodeWithTag("check")
        val bounds = control.fetchSemanticsNode().boundsInRoot
        control.assertIsOff().assertHeightIsEqualTo(52.dp)
        fun pixels() = onRoot().captureToImage().toPixelMap()
        val off = pixels()
        assertEquals(Color.Black, off[68,54])
        assertTrue(off[68,31].green > off[68,76].green, "The ring shades from bright to dark green")
        save("off")
        control.performMouseInput { enter(center) }
        mainClock.advanceTimeBy(200)
        assertEquals(off[175,12], pixels()[175,12], "Hover has no rim")
        control.performMouseInput { exit() }
        control.performTouchInput { down(center) }
        mainClock.advanceTimeBy(160)
        control.assertIsOff()
        val held = pixels()
        assertTrue(held[175,12].red > .4f && held[175,12].green > .4f, "Only the external rim lights up")
        for (y in 27..81) for (x in 41..95) assertEquals(off[x,y], held[x,y], "Pressing preserves the green glyph")
        save("held")
        control.performTouchInput { up() }
        mainClock.advanceTimeByFrame()
        control.assertIsOn()
        mainClock.advanceTimeBy(80)
        val squash = greenBounds(pixels())
        assertTrue(squash.height < 36 && squash.width > 50, "Checking first flattens and widens the ring: $squash")
        save("on-080")
        mainClock.advanceTimeBy(96)
        val turn = greenBounds(pixels())
        assertTrue(turn.width < 48 && turn.height > 48, "The arriving check turns on its side: $turn")
        save("on-176")
        mainClock.advanceTimeBy(256)
        assertEquals(bounds, control.fetchSemanticsNode().boundsInRoot, "Animation cannot move the label or click target")
        assertTrue(pixels()[66,67].green > .5f, "The check is green on black")
        assertEquals(off[175,12], pixels()[175,12], "Selection does not retain a rim")
        save("on")
        control.performTouchInput { click() }
        mainClock.advanceTimeBy(32)
        control.assertIsOff()
        val cancelled = pixels()
        for (y in 27..81) for (x in 41..95) assertEquals(off[x,y], cancelled[x,y], "Unchecking restores the ring without a reverse flip")
        assertEquals(2, changes)
        runOnIdle { label = "不再提示" }
        mainClock.advanceTimeBy(160)
        val labelBounds = onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        var frame = 0
        fun frames(count: Int) = repeat(count) {
            mainClock.advanceTimeByFrame()
            assertEquals(labelBounds, onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot)
            save("motion-${(frame++).toString().padStart(3, '0')}")
        }
        frames(12)
        control.performTouchInput { down(center) }; frames(12)
        control.performTouchInput { up() }; frames(40)
        control.performTouchInput { down(center) }; frames(12)
        control.performTouchInput { up() }; frames(20)
        control.assertIsOff()
        assertEquals(4, changes)
    }

    @Test fun controlledMixedDisabledAndInterruptedInput() = runDesktopComposeUiTest(width = 420, height = 340) {
        mainClock.autoAdvance = false
        var state by mutableStateOf(ToggleableState.Indeterminate)
        var enabled by mutableStateOf(true)
        var changes = 0
        setContent { ZenlessTheme {
            Column(Modifier.padding(12.dp)) {
                ZenlessCheckbox(state, { state = it; changes++ }, Modifier.testTag("check"), enabled, "不再提示")
                ZenlessCheckbox(ToggleableState.Off, { changes++ }, Modifier.testTag("controlled"), label = "Caller owned")
                ZenlessCheckbox(ToggleableState.On, { changes++ }, Modifier.testTag("disabled"), enabled = false, label = "Disabled")
            }
        } }
        mainClock.advanceTimeBy(32)
        val control = onNodeWithTag("check")
        save("mixed-and-disabled")
        onNodeWithText("不再提示").performTouchInput { click() }
        mainClock.advanceTimeBy(80)
        control.assertIsOn()
        control.performTouchInput { click() }
        mainClock.advanceTimeBy(32)
        control.assertIsOff()
        runOnIdle { assertEquals(2, changes); state = ToggleableState.On }
        mainClock.advanceTimeBy(80)
        runOnIdle { enabled = false }
        mainClock.advanceTimeBy(32)
        control.assertIsNotEnabled().performTouchInput { click() }
        onNodeWithTag("disabled").assertIsNotEnabled().performTouchInput { click() }
        runOnIdle { assertEquals(2, changes); enabled = true }
        control.performTouchInput { down(center); moveTo(Offset(-100f, -100f)); up() }
        mainClock.advanceTimeBy(160)
        runOnIdle { assertEquals(2, changes) }
        onNodeWithTag("controlled").performTouchInput { click() }
        mainClock.advanceTimeBy(440)
        onNodeWithTag("controlled").assertIsOff()
        assertEquals(3, changes, "Only user activations emit callbacks")
        save("labeled")
    }

    @Test fun presetsRtlAndLongTextKeepTheirLayout() {
        for (density in listOf(1f, 2f)) runDesktopComposeUiTest(width = 660, height = 600) {
            var preset by mutableStateOf(ZenlessSize.Default)
            var rtl by mutableStateOf(false)
            setContent { CompositionLocalProvider(LocalDensity provides Density(density, 1.5f),
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) { ZenlessTheme(preset) {
                Column(Modifier.width(300.dp).padding(10.dp)) {
                    ZenlessCheckbox(ToggleableState.On, {}, Modifier.testTag("empty"))
                    ZenlessCheckbox(ToggleableState.On, {}, Modifier.testTag("short"), label = "Text")
                    ZenlessCheckbox(ToggleableState.Off, {}, Modifier.testTag("long"), label = "A much longer description that needs to wrap onto several lines")
                }
            } } }
            for (size in ZenlessSize.entries) {
                runOnIdle { preset = size; rtl = false }
                onNodeWithTag("empty").assertHeightIsEqualTo(size.height.dp)
                assertTrue(onNodeWithTag("short").fetchSemanticsNode().boundsInRoot.height >= size.height * density)
                val before = onNodeWithTag("long").fetchSemanticsNode().boundsInRoot
                assertTrue(before.height > size.height * density)
                val ltr = onNodeWithText("Text", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                val ltrRow = onNodeWithTag("short").fetchSemanticsNode().boundsInRoot
                runOnIdle { rtl = true }
                val after = onNodeWithTag("long").fetchSemanticsNode().boundsInRoot
                assertEquals(before.size, after.size)
                val label = onNodeWithText("Text", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                val row = onNodeWithTag("short").fetchSemanticsNode().boundsInRoot
                assertTrue(label.left - row.left < ltr.left - ltrRow.left, "RTL moves the indicator to the right")
                assertTrue(label.left >= row.left && label.right <= row.right)
            }
            save("presets-rtl-$density")
        }
    }

    private fun greenBounds(pixels: PixelMap): IntRect {
        val points = (23..85).flatMap { y -> (30..108).filter { x ->
            val c = pixels[x,y]; c.green > .3f && c.red < .15f && c.blue < .15f
        }.map { x -> IntOffset(x,y) } }
        return IntRect(points.minOf { it.x }, points.minOf { it.y }, points.maxOf { it.x } + 1, points.maxOf { it.y } + 1)
    }

    private fun DesktopComposeUiTest.save(name: String) {
        val file = File("../verification/checkbox/$name-actual.png"); file.parentFile.mkdirs()
        Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image -> image.encodeToData()?.use { file.writeBytes(it.bytes) } }
    }
}
