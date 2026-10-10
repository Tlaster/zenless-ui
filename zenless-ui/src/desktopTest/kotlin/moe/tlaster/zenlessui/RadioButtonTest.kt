package moe.tlaster.zenlessui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.*
import org.jetbrains.skia.Image
import org.junit.Test
import java.io.File
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class RadioButtonTest {
    @Test fun capsulePressAndDotMotionKeepLabelStill() = runDesktopComposeUiTest(width = 350, height = 120) {
        mainClock.autoAdvance = false
        var selected by mutableStateOf(false)
        var clicks = 0
        setContent { CompositionLocalProvider(LocalDensity provides Density(2f)) { ZenlessTheme {
            Box(Modifier.fillMaxSize().background(Color(0xff222222))) {
                ZenlessRadioButton(selected, { selected = true; clicks++ },
                    Modifier.offset(14.dp, 1.dp).width(140.dp).testTag("radio"), label = "标准")
            }
        } } }
        mainClock.advanceTimeBy(32)
        val control = onNodeWithTag("radio")
        control.assertIsNotSelected().assertHeightIsEqualTo(52.dp)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        val bounds = control.fetchSemanticsNode().boundsInRoot
        val text = onNodeWithText("标准", useUnmergedTree = true)
        val labelBounds = text.fetchSemanticsNode().boundsInRoot
        val layouts = mutableListOf<TextLayoutResult>()
        text.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(Color(0xffc1c2c2), layouts.single().layoutInput.style.color)
        fun pixels() = onRoot().captureToImage().toPixelMap()
        fun sameGlyph(expected: PixelMap) {
            val actual = pixels()
            for (y in 27..81) for (x in 41..95) assertEquals(expected[x,y], actual[x,y], "Glyph at $x,$y")
        }
        val off = pixels()
        assertEquals(Color.Black, off[68,54])
        assertTrue(off[68,31].green > off[68,76].green)
        save("off")
        control.performMouseInput { enter(center) }; mainClock.advanceTimeBy(160)
        assertEquals(off[175,12], pixels()[175,12], "Hover is quiet")
        control.performMouseInput { exit() }
        control.performTouchInput { down(center) }; mainClock.advanceTimeBy(160)
        control.assertIsNotSelected()
        val held = pixels()
        assertTrue(held[175,12].red > .4f && held[175,12].green > .4f)
        sameGlyph(off)
        save("held")
        control.performTouchInput { up() }; mainClock.advanceTimeByFrame()
        control.assertIsSelected()
        mainClock.advanceTimeBy(80)
        val squash = greenBounds(pixels())
        assertTrue(squash.height < 36 && squash.width > 50, "Selection squashes the ring: $squash")
        assertEquals(Color.Black, pixels()[68,54], "The dot waits for the side turn")
        save("on-080")
        mainClock.advanceTimeBy(96)
        val turn = greenBounds(pixels())
        assertTrue(turn.width < 48 && turn.height > 48, "The ring turns on its side: $turn")
        val growing = centerGreen(pixels())
        assertTrue(growing > 0, "The dot grows from the center")
        save("on-176")
        mainClock.advanceTimeBy(256)
        val on = pixels()
        assertTrue(centerGreen(on) > growing, "The dot finishes growing")
        assertTrue(on[80,54].green > .5f)
        assertEquals(Color.Black, on[84,54], "Black separation remains between dot and ring")
        assertEquals(off[175,12], on[175,12], "Selection does not retain a rim")
        save("on")
        control.performTouchInput { down(center) }; mainClock.advanceTimeBy(160)
        sameGlyph(on)
        control.performTouchInput { up() }
        repeat(28) {
            mainClock.advanceTimeByFrame()
            sameGlyph(on)
            assertEquals(bounds, control.fetchSemanticsNode().boundsInRoot)
            assertEquals(labelBounds, text.fetchSemanticsNode().boundsInRoot)
        }
        assertEquals(2, clicks, "Repeat clicks retain the public callback contract")
        runOnIdle { selected = false }; mainClock.advanceTimeBy(32)
        control.assertIsNotSelected(); sameGlyph(off)
        assertEquals(2, clicks, "Caller updates never emit callbacks")
        var frame = 0
        fun frames(count: Int) = repeat(count) {
            mainClock.advanceTimeByFrame()
            assertEquals(labelBounds, text.fetchSemanticsNode().boundsInRoot)
            save("motion-${(frame++).toString().padStart(3, '0')}")
        }
        frames(12)
        control.performTouchInput { down(center) }; frames(12)
        control.performTouchInput { up() }; frames(32)
        control.performTouchInput { down(center) }; frames(12)
        control.performTouchInput { up() }; frames(16)
        runOnIdle { selected = false }; frames(12)
    }

    @Test fun groupSwitchingInitialSelectionAndInterruptedInput() = runDesktopComposeUiTest(width = 520, height = 340) {
        mainClock.autoAdvance = false
        var selected by mutableIntStateOf(0)
        var enabled by mutableStateOf(true)
        var clicks = 0
        setContent { CompositionLocalProvider(LocalDensity provides Density(1f)) { ZenlessTheme {
            Column(Modifier.padding(12.dp).selectableGroup()) {
                listOf("标准", "高级").forEachIndexed { index, label ->
                    ZenlessRadioButton(selected == index, { selected = index; clicks++ }, Modifier.testTag("radio-$index"), enabled, label)
                }
                ZenlessRadioButton(false, { clicks++ }, Modifier.testTag("controlled"), label = "Caller owned")
                ZenlessRadioButton(true, { clicks++ }, Modifier.testTag("disabled"), enabled = false, label = "Disabled")
            }
        } } }
        mainClock.advanceTimeBy(32)
        val first = onNodeWithTag("radio-0")
        val second = onNodeWithTag("radio-1")
        fun glyph(node: SemanticsNodeInteraction): List<Color> {
            val pixels = node.captureToImage().toPixelMap()
            return (12..39).flatMap { y -> (6..33).map { x -> pixels[x,y] } }
        }
        val settled = glyph(first)
        val empty = glyph(second)
        mainClock.advanceTimeBy(440)
        assertEquals(settled, glyph(first), "Initial selection is settled")
        onNodeWithText("高级").performTouchInput { click() }; mainClock.advanceTimeBy(32)
        first.assertIsNotSelected(); second.assertIsSelected()
        assertEquals(empty, glyph(first), "The old option clears immediately")
        mainClock.advanceTimeBy(64)
        first.performTouchInput { click() }; mainClock.advanceTimeBy(32)
        first.assertIsSelected(); second.assertIsNotSelected()
        assertEquals(empty, glyph(second), "Rapid reversal cancels the departing animation")
        mainClock.advanceTimeBy(440)
        assertEquals(settled, glyph(first))
        second.performTouchInput { down(center); moveTo(Offset(-100f, -100f)); up() }
        mainClock.advanceTimeBy(160)
        second.assertIsNotSelected()
        onNodeWithTag("controlled").performTouchInput { click() }; mainClock.advanceTimeBy(440)
        onNodeWithTag("controlled").assertIsNotSelected()
        assertEquals(3, clicks)
        second.performTouchInput { down(center) }; mainClock.advanceTimeBy(160)
        runOnIdle { enabled = false }; mainClock.advanceTimeBy(32)
        second.performTouchInput { up() }
        first.assertIsNotEnabled(); second.assertIsNotEnabled()
        onNodeWithTag("disabled").assertIsSelected().assertIsNotEnabled().performTouchInput { click() }
        runOnIdle { enabled = true; selected = 1 }; mainClock.advanceTimeBy(80)
        runOnIdle { enabled = false }; mainClock.advanceTimeBy(32)
        assertEquals(glyph(onNodeWithTag("disabled")), glyph(second), "Disabling mid-animation shows the muted settled dot")
        assertEquals(3, clicks)
        save("group-disabled")
    }

    @Test fun presetsRtlAndLargeTextKeepExistingSizing() {
        // Keep 900 dp of height so platform font metrics cannot squeeze the last control.
        for (density in listOf(1f, 2f)) runDesktopComposeUiTest(width = 660, height = (900 * density).toInt()) {
            var preset by mutableStateOf(ZenlessSize.Default)
            var rtl by mutableStateOf(false)
            setContent { CompositionLocalProvider(LocalDensity provides Density(density, 1.5f),
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) { ZenlessTheme(preset) {
                Column(Modifier.width(300.dp).padding(10.dp)) {
                    ZenlessRadioButton(true, {}, Modifier.testTag("empty"))
                    ZenlessRadioButton(true, {}, Modifier.testTag("short"), label = "Text")
                    ZenlessRadioButton(false, {}, Modifier.testTag("long"), label = "A much longer description that needs to wrap onto several lines")
                    ZenlessRadioButton(true, {}, Modifier.testTag("override"), size = ZenlessSize.Compact)
                }
            } } }
            for (size in ZenlessSize.entries) {
                runOnIdle { preset = size; rtl = false }
                onNodeWithTag("empty").assertHeightIsEqualTo(size.height.dp)
                onNodeWithTag("override").assertHeightIsEqualTo(40.dp)
                val before = onNodeWithTag("long").fetchSemanticsNode().boundsInRoot
                assertTrue(before.height > size.height * density)
                val text = onNodeWithText("Text", useUnmergedTree = true)
                val layouts = mutableListOf<TextLayoutResult>()
                text.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertEquals(size.fontSize.sp, layouts.single().layoutInput.style.fontSize)
                val ltr = text.fetchSemanticsNode().boundsInRoot
                val ltrRow = onNodeWithTag("short").fetchSemanticsNode().boundsInRoot
                runOnIdle { rtl = true }
                assertEquals(before.size, onNodeWithTag("long").fetchSemanticsNode().boundsInRoot.size)
                val label = text.fetchSemanticsNode().boundsInRoot
                val row = onNodeWithTag("short").fetchSemanticsNode().boundsInRoot
                assertTrue(label.left - row.left < ltr.left - ltrRow.left)
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

    private fun centerGreen(pixels: PixelMap): Int = (45..63).sumOf { y -> (59..77).count { x -> pixels[x,y].green > .3f } }

    private fun DesktopComposeUiTest.save(name: String) {
        val file = File("../verification/radio/$name-actual.png"); file.parentFile.mkdirs()
        Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image -> image.encodeToData()?.use { file.writeBytes(it.bytes) } }
    }
}
