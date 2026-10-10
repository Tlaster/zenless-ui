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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Image
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class FeedCardTest {
    @Test fun hoverPressFocusSelectionAndDisableAreIndependent() = runDesktopComposeUiTest(width = 700, height = 1000) {
        mainClock.autoAdvance = false
        val requester = FocusRequester()
        lateinit var focus: FocusManager
        var selected by mutableStateOf(false)
        var enabled by mutableStateOf(true)
        var clicks = 0
        setContent { CompositionLocalProvider(LocalDensity provides Density(2f)) { ZenlessTheme {
            focus = LocalFocusManager.current
            Fixture(Modifier.width(320.dp).focusRequester(requester).testTag("card"), selected, enabled, onClick = { clicks++ })
        } } }
        mainClock.advanceTimeBy(32)
        val card = onNodeWithTag("card")
        val bounds = card.fetchSemanticsNode().boundsInRoot
        fun rim() = card.captureToImage().toPixelMap()[3, 200]
        assertEquals(Color.Black, rim())
        card.performMouseInput { enter(center) }
        mainClock.advanceTimeBy(320)
        assertEquals(Color.Black, rim(), "Hover never highlights")
        card.performMouseInput { exit() }
        card.performTouchInput { down(center) }
        mainClock.advanceTimeBy(160)
        runOnIdle { focus.clearFocus(force = true) }
        mainClock.advanceTimeByFrame()
        card.assertIsNotFocused()
        val pressed = rim()
        assertTrue(pressed.red > .5f && pressed.green > .5f)
        mainClock.advanceTimeBy(400)
        assertNotEquals(pressed, rim(), "The rim breathes in color")
        assertEquals(bounds, card.fetchSemanticsNode().boundsInRoot, "Breathing does not change layout")
        card.performTouchInput { moveTo(androidx.compose.ui.geometry.Offset(-40f, -40f)); up() }
        mainClock.advanceTimeBy(96)
        assertEquals(Color.Black, rim(), "Cancellation does not flash")
        card.performTouchInput { click() }
        mainClock.advanceTimeBy(32)
        runOnIdle { focus.clearFocus(force = true); assertEquals(1, clicks) }
        mainClock.advanceTimeBy(32)
        card.assertIsNotSelected()
        assertEquals(Color.Black, rim(), "Click does not own selection or flash on release")
        runOnIdle { requester.requestFocus() }
        mainClock.advanceTimeBy(320)
        card.assertIsFocused()
        assertTrue(rim().red > .5f)
        card.performKeyInput { pressKey(androidx.compose.ui.input.key.Key.Enter) }
        runOnIdle { assertEquals(2, clicks, "The focused card has one keyboard action") }
        val before = rim()
        runOnIdle { selected = true; focus.clearFocus(force = true) }
        mainClock.advanceTimeBy(16)
        val after = rim()
        assertTrue(kotlin.math.abs(before.red - after.red) < .03f, "Focus-to-selection handoff keeps the breathing phase")
        card.assertIsSelected()
        runOnIdle { enabled = false }
        mainClock.advanceTimeBy(32)
        card.assertIsNotEnabled().assert(isFocused().not()).performTouchInput { click() }
        runOnIdle { assertEquals(2, clicks) }
        assertEquals(0f, rim().red, "Disabled selection does not breathe")
    }

    @Test fun scrollingCancelsTheCardsClick() = runDesktopComposeUiTest(width = 360, height = 400) {
        mainClock.autoAdvance = false
        var clicks = 0
        lateinit var scroll: androidx.compose.foundation.ScrollState
        setContent { ZenlessTheme {
            scroll = rememberScrollState()
            Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                Fixture(Modifier.width(300.dp).testTag("card"), ratio = .5f, onClick = { clicks++ })
                Spacer(Modifier.height(600.dp))
            }
        } }
        onRoot().performTouchInput { swipeUp() }
        mainClock.advanceTimeBy(400)
        runOnIdle { assertEquals(0, clicks) }
        runOnIdle { assertTrue(scroll.value > 0, "The gesture actually scrolls") }
    }

    @Test fun sizesReserveTheCoverAndAllowCallerTextToGrow() {
        for (density in listOf(1f, 2f)) runDesktopComposeUiTest(width = 900, height = 1600) {
            mainClock.autoAdvance = false
            var size by mutableStateOf(ZenlessSize.Default)
            var loaded by mutableStateOf(false)
            var long by mutableStateOf(false)
            setContent { CompositionLocalProvider(LocalDensity provides Density(density, 1.25f)) { ZenlessTheme(size = size) {
                ZenlessFeedCard({}, 1.6f,
                    cover = { Box(Modifier.fillMaxSize().testTag("cover").background(if (loaded) Color.Cyan else Color.DarkGray)) },
                    avatar = { Box(Modifier.fillMaxSize().testTag("avatar").background(Color.Magenta)) },
                    author = { ZenlessText("Author") }, title = { ZenlessText(if (long) List(20) { "Unrestricted title" }.joinToString(" ") else "Title") },
                    summary = { ZenlessText("Summary") }, modifier = Modifier.width(320.dp).testTag("card"),
                    label = { ZenlessText("Label") }, status = { ZenlessText("Read") })
            } } }
            for (preset in ZenlessSize.entries) {
                runOnIdle { size = preset; long = false }
                mainClock.advanceTimeBy(32)
                val cover = onNodeWithTag("cover", true).fetchSemanticsNode().boundsInRoot
                val avatar = onNodeWithTag("avatar", true).fetchSemanticsNode().boundsInRoot
                assertTrue(kotlin.math.abs(cover.width - (320 - 9.5f*preset.scale)*density) <= 1f)
                assertTrue(kotlin.math.abs(cover.width/cover.height - 1.6f) < .01f)
                assertTrue(kotlin.math.abs(avatar.width - 66*preset.scale*density) <= 1f)
                val before = onNodeWithTag("card").fetchSemanticsNode().boundsInRoot
                runOnIdle { loaded = !loaded }
                mainClock.advanceTimeBy(32)
                assertEquals(before, onNodeWithTag("card").fetchSemanticsNode().boundsInRoot, "Image loading must not move the layout")
                runOnIdle { long = true }
                mainClock.advanceTimeBy(32)
                assertTrue(onNodeWithTag("card").fetchSemanticsNode().boundsInRoot.height > before.height + 30*density)
            }
        }
    }

    @Test fun measuredShellClipsArtworkAndOptionalCapsules() = runDesktopComposeUiTest(width = 700, height = 1000) {
        mainClock.autoAdvance = false
        var capsules by mutableStateOf(false)
        setContent { CompositionLocalProvider(LocalDensity provides Density(2f)) { ZenlessTheme {
            Fixture(Modifier.width(320.dp).testTag("card"), label = capsules, status = capsules)
        } } }
        mainClock.advanceTimeBy(32)
        val card = onNodeWithTag("card")
        val first = card.captureToImage().toPixelMap()
        assertEquals(Color.Black, first[3, 200])
        assertEquals(Color(0xff181818), first[200, 750])
        assertEquals(Color(0xff2a2a2a), first[300, 702])
        assertEquals(Color(0xff181818), first[108, 565], "Avatar crest extends into the cover")
        assertEquals(Color.Magenta, first[108, 600])
        assertEquals(Color.Cyan, first[44, 571], "Avatar artwork stays inside its circle")
        val before = card.fetchSemanticsNode().boundsInRoot
        runOnIdle { capsules = true }
        mainClock.advanceTimeBy(32)
        val second = card.captureToImage().toPixelMap()
        assertEquals(before, card.fetchSemanticsNode().boundsInRoot)
        assertTrue(second[160, 35].blue > .9f && second[160, 35].green > .5f)
        assertTrue(second[160, 85].green < second[160, 35].green)
        save(card.captureToImage(), "geometry")
    }

    @Test fun selectedContourMatchesReferenceScanlinesWithoutScaling() = runDesktopComposeUiTest(width = 700, height = 1000) {
        mainClock.autoAdvance = false
        setContent { CompositionLocalProvider(LocalDensity provides Density(2f)) { ZenlessTheme {
            Fixture(Modifier.width(320.dp).testTag("card"), selected = true, coverColor = Color(0xff424956))
        } } }
        mainClock.advanceTimeByFrame()
        val card = onNodeWithTag("card")
        val pixels = card.captureToImage().toPixelMap()
        // Left rim spans measured in the original selected card at (893, 1167).
        for ((y, start, end) in listOf(Triple(10, 35, 61), Triple(20, 21, 36), Triple(40, 7, 17),
            Triple(60, 1, 10), Triple(100, 0, 8), Triple(200, 0, 8), Triple(910, 22, 36))) {
            val xs = (0..70).filter { x -> val c = pixels[x,y]; c.red > 180/255f && c.green > 140/255f && c.blue < 35/255f }
            assertTrue(kotlin.math.abs(xs.first()-start) <= 1, "Outer reference contour at row $y")
            assertTrue(kotlin.math.abs(xs.last()-end) <= 1, "Inner reference contour at row $y")
        }
        val before = card.fetchSemanticsNode().boundsInRoot
        mainClock.advanceTimeBy(700)
        val next = card.captureToImage().toPixelMap()
        assertEquals(before, card.fetchSemanticsNode().boundsInRoot)
        assertNotEquals(pixels[3,200], next[3,200])
        assertEquals(pixels[300,750], next[300,750], "Breathing affects only the rim")
        save(card.captureToImage(), "selected-geometry")
    }

    @Test fun invalidCoverRatiosAreRejected() {
        for (ratio in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> {
                runDesktopComposeUiTest { setContent { ZenlessTheme { Fixture(Modifier.width(320.dp), ratio = ratio) } } }
            }
        }
    }

    @Test fun captureReferenceFixturesWhenLocalArtworkIsPrepared() {
        val folder = File("../verification/feed-card")
        assumeTrue("Run tools/compare_feed_card.py --prepare with the original PNG", File(folder, "normal-cover.png").exists())
        for (name in listOf("normal", "selected", "status")) runDesktopComposeUiTest(width = 640, height = 1000) {
            mainClock.autoAdvance = false
            val cover = Image.makeFromEncoded(File(folder, "$name-cover.png").readBytes()).toComposeImageBitmap()
            val avatar = Image.makeFromEncoded(File(folder, "$name-avatar.png").readBytes()).toComposeImageBitmap()
            setContent { CompositionLocalProvider(LocalDensity provides Density(2f, 1f)) { ZenlessTheme(size = ZenlessSize.Default) {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    ZenlessFeedCard({}, if (name == "status") 621f/390f else 1f,
                        cover = { Image(cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) },
                        avatar = { Image(avatar, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) },
                        author = { Spacer(Modifier.height(24.dp)) }, title = { Spacer(Modifier.height(57.dp)) },
                        summary = { Spacer(Modifier.height(24.dp)) }, modifier = Modifier.width(320.dp).testTag("card"),
                        selected = name == "selected",
                        label = if (name == "normal") ({ Spacer(Modifier.size(130.5f.dp, 24.dp)) }) else null,
                        status = if (name == "status") ({ Spacer(Modifier.size(44.dp, 24.dp)) }) else null)
                }
            } } }
            mainClock.advanceTimeByFrame()
            save(onNodeWithTag("card").captureToImage(), "$name-actual")
        }
    }

    @Composable private fun Fixture(modifier: Modifier, selected: Boolean = false, enabled: Boolean = true, ratio: Float = 1f,
        label: Boolean = false, status: Boolean = false, coverColor: Color = Color.Cyan, onClick: () -> Unit = {}) {
        ZenlessFeedCard(onClick, ratio,
            cover = { Box(Modifier.fillMaxSize().background(coverColor)) },
            avatar = { Box(Modifier.fillMaxSize().background(Color.Magenta)) },
            author = { Spacer(Modifier.height(24.dp)) }, title = { Spacer(Modifier.height(57.dp)) }, summary = { Spacer(Modifier.height(24.dp)) },
            modifier = modifier, selected = selected, enabled = enabled,
            label = if (label) ({ ZenlessText("Featured") }) else null, status = if (status) ({ ZenlessText("Read") }) else null)
    }

    private fun save(bitmap: androidx.compose.ui.graphics.ImageBitmap, name: String) {
        val file = File("../verification/feed-card/$name.png"); file.parentFile.mkdirs()
        Image.makeFromBitmap(bitmap.asSkiaBitmap()).use { image -> image.encodeToData()?.use { file.writeBytes(it.bytes) } }
    }
}
