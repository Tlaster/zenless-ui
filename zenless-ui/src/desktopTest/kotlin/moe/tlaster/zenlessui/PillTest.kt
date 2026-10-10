package moe.tlaster.zenlessui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.*
import org.jetbrains.skia.Image
import org.junit.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class PillTest {
    @Test fun pressSelectionFocusAndDisableShareOnlyTheColorCycle() = runDesktopComposeUiTest(width = 660, height = 180) {
        mainClock.autoAdvance = false
        val requester = FocusRequester()
        lateinit var focus: FocusManager
        var selected by mutableStateOf(false)
        var enabled by mutableStateOf(true)
        var clicks = 0
        setContent { CompositionLocalProvider(LocalDensity provides Density(2f)) { ZenlessTheme {
            focus = LocalFocusManager.current
            Box(Modifier.fillMaxSize().background(Color.Black).padding(10.dp)) {
                ZenlessPillContainer(Modifier.width(300.dp).focusRequester(requester).testTag("pill"),
                    { clicks++ }, selected, enabled) { Spacer(Modifier.size(20.dp)) }
            }
        } } }
        mainClock.advanceTimeBy(32)
        val pill = onNodeWithTag("pill")
        val bounds = pill.fetchSemanticsNode().boundsInRoot
        fun snapshot() = onRoot().captureToImage().toPixelMap()
        fun rim() = snapshot()[300, 16]
        val normal = snapshot()
        pill.performMouseInput { enter(center) }
        mainClock.advanceTimeBy(300)
        assertEquals(Color.Black, rim(), "Hover is quiet")
        pill.performMouseInput { exit() }
        pill.performTouchInput { down(center) }
        mainClock.advanceTimeBy(320)
        runOnIdle { focus.clearFocus(force = true) }
        mainClock.advanceTimeByFrame()
        val pressed = rim()
        assertTrue(pressed.red > .5f && pressed.green > .5f)
        mainClock.advanceTimeBy(400)
        assertNotEquals(pressed, rim())
        val held = snapshot()
        assertEquals(bounds, pill.fetchSemanticsNode().boundsInRoot)
        for (y in 21..122) for (x in 75..564) assertEquals(normal[x,y], held[x,y], "Shell/content never scale or change ink")
        val before = rim()
        runOnIdle { selected = true }
        pill.performTouchInput { up() }
        mainClock.advanceTimeBy(16)
        runOnIdle { focus.clearFocus(force = true); assertEquals(1, clicks) }
        assertTrue(abs(before.red-rim().red) < .04f, "Press-to-selection preserves phase")
        runOnIdle { selected = false }
        mainClock.advanceTimeBy(32)
        pill.assertIsNotSelected()
        assertEquals(Color.Black, rim(), "Release has no flash and never owns selection")
        pill.performTouchInput { down(center); moveTo(Offset(-30f,-30f)); up() }
        runOnIdle { focus.clearFocus(force = true) }
        mainClock.advanceTimeBy(64)
        assertEquals(Color.Black, rim())
        runOnIdle { assertEquals(1, clicks); requester.requestFocus() }
        mainClock.advanceTimeBy(300)
        pill.assertIsFocused()
        assertTrue(rim().red > .5f)
        pill.performKeyInput { pressKey(Key.Enter) }
        runOnIdle { assertEquals(2, clicks); selected = true; enabled = false }
        mainClock.advanceTimeBy(32)
        pill.assertIsNotEnabled().performTouchInput { click() }
        assertEquals(Color.Black, rim())
        runOnIdle { assertEquals(2, clicks) }
        val disabled = snapshot()
        for (y in 21..122) for (x in 75..564) assertEquals(normal[x,y], disabled[x,y], "Disabled retains the shell")
    }

    @Test fun presetsNaturalWidthOverridesGrowthAndRtl() {
        for (density in listOf(1f, 2f)) runDesktopComposeUiTest(width = 800, height = 900) {
            var size by mutableStateOf(ZenlessSize.Default)
            var tall by mutableStateOf(false)
            var rtl by mutableStateOf(false)
            setContent { CompositionLocalProvider(LocalDensity provides Density(density, 1.5f),
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) { ZenlessTheme(size) {
                Column(Modifier.padding(10.dp)) {
                    ZenlessPillContainer(Modifier.testTag("natural")) { Spacer(Modifier.size(60.dp, 10.dp)) }
                    ZenlessPillContainer(Modifier.width(220.dp).testTag("grow")) {
                        Column { ZenlessText(if (tall) List(10) { "Long content" }.joinToString(" ") else "Text") }
                    }
                    ZenlessPillContainer(Modifier.width(140.dp).testTag("override"), size = ZenlessSize.Compact) { Spacer(Modifier.height(10.dp)) }
                }
            } } }
            for (preset in ZenlessSize.entries) {
                runOnIdle { size = preset; tall = false }
                onNodeWithTag("natural").assertHeightIsEqualTo(preset.height.dp)
                    .assertWidthIsEqualTo((60+2*(18*preset.scale*density).roundToInt()/density).dp)
                onNodeWithTag("natural").assertHasNoClickAction()
                onNodeWithTag("override").assertHeightIsEqualTo(40.dp)
                val before = onNodeWithTag("grow").fetchSemanticsNode().boundsInRoot
                runOnIdle { tall = true }
                val after = onNodeWithTag("grow").fetchSemanticsNode().boundsInRoot
                assertEquals(before.width, after.width)
                assertTrue(after.height > before.height + 20*density)
                val ltr = onNodeWithTag("natural").captureToImage().toPixelMap()
                runOnIdle { rtl = !rtl }
                val other = onNodeWithTag("natural").captureToImage().toPixelMap()
                for (y in 0 until ltr.height) for (x in 0 until ltr.width) {
                    assertTrue(abs(ltr[x,y].red-other[x,y].red) <= 1.01f/255, "RTL preserves geometry; raster dithering can differ by one level")
                }
            }
        }
    }

    @Test fun clippingStaysInsideTheShellAndScrollCancelsActivation() = runDesktopComposeUiTest(width = 360, height = 400) {
        var clicks = 0
        lateinit var scroll: androidx.compose.foundation.ScrollState
        setContent { ZenlessTheme {
            scroll = rememberScrollState()
            Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(scroll).padding(12.dp)) {
                ZenlessPillContainer(Modifier.size(200.dp, 100.dp).testTag("pill"), onClick = { clicks++ }) {
                    Box(Modifier.requiredSize(500.dp).background(Color.Magenta))
                }
                Spacer(Modifier.height(800.dp))
            }
        } }
        val image = onNodeWithTag("pill").captureToImage().toPixelMap()
        assertEquals(Color.White, image[0,0], "Oversized caller content is clipped to the pill")
        assertEquals(Color.Magenta, image[100,50])
        onNodeWithTag("pill").performTouchInput { swipe(center, Offset(center.x,-150f), 300) }
        runOnIdle { assertEquals(0, clicks); assertTrue(scroll.value > 0) }
    }

    @Test fun captureReferenceShellsAndLockContourGeometry() {
        for (name in listOf("normal", "selected", "background")) {
            val backing = File("../verification/pill/backing.png")
            if (name == "background" && !backing.exists()) continue
            runDesktopComposeUiTest(width = 660, height = 156) {
                mainClock.autoAdvance = false
                setContent { CompositionLocalProvider(LocalDensity provides Density(2f,1f)) { ZenlessTheme {
                    Box(Modifier.fillMaxSize().background(Color.Black)) {
                        if (name == "background") {
                            val image = remember { Image.makeFromEncoded(backing.readBytes()).toComposeImageBitmap() }
                            Image(image, null, Modifier.fillMaxSize())
                        }
                        ZenlessPillContainer(Modifier.offset(7.5f.dp, 8.dp).size(314.5f.dp, 62.5f.dp), selected = name == "selected") {}
                    }
                } } }
                mainClock.advanceTimeBy(736)
                val bitmap = onRoot().captureToImage()
                val pixels = bitmap.toPixelMap()
                save(bitmap, "$name-actual")
                for ((y, gray) in listOf(22 to 45, 36 to 23, 60 to 20, 84 to 14, 108 to 13, 132 to 14)) {
                    assertTrue(abs(pixels[510,y].red-gray/255f) <= 2.01f/255, "Reference shading at row $y")
                }
                if (name == "selected") {
                    for ((y, first, last) in listOf(Triple(10,64,594), Triple(16,46,612), Triple(36,22,636), Triple(79,9,649), Triple(146,64,594))) {
                        val xs = (0 until 660).filter { x -> val c = pixels[x,y]; c.red > 60/255f && c.green > 75/255f && c.blue < 50/255f }
                        assertTrue(abs(xs.first()-first) <= 1 && abs(xs.last()-last) <= 1, "Measured highlight contour row $y: ${xs.first()}..${xs.last()}")
                    }
                }
            }
        }
    }

    private fun save(bitmap: ImageBitmap, name: String) {
        val file = File("../verification/pill/$name.png"); file.parentFile.mkdirs()
        Image.makeFromBitmap(bitmap.asSkiaBitmap()).use { image -> image.encodeToData()?.use { file.writeBytes(it.bytes) } }
    }
}
