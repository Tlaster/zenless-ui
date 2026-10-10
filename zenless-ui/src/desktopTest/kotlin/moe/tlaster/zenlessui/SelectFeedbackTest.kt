package moe.tlaster.zenlessui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Test
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class SelectFeedbackTest {
    @Test fun mouseHoldSurvivesLeavingAndOutsideReleaseOnlyFlashesTheRim() = runDesktopComposeUiTest(width=600,height=600) {
        mainClock.autoAdvance=false
        var changes=0
        setContent { Host { Box(Modifier.padding(100.dp)) { Select(onSelected={changes++}) } } }
        val select=onNodeWithContentDescription("Choice")
        mainClock.advanceTimeBy(32)
        val resting=select.captureToImage().toPixelMap()
        assertEquals(Color.Black,rim())
        select.performMouseInput { enter(center); press() }
        mainClock.advanceTimeBy(480)
        val held=rim()
        assertTrue(held.green>.75f)
        assertEquals(Color.White,onNodeWithText("Option 0").ink())
        val pixels=select.captureToImage().toPixelMap()
        assertEquals(Color.White,pixels[pixels.width-26,pixels.height/2],"Arrow stays white")
        assertEquals(resting[12,pixels.height/2],pixels[12,pixels.height/2],"The plate is not filled by press feedback")
        onNode(hasScrollToIndexAction()).assertDoesNotExist()
        select.performMouseInput { moveTo(Offset(-40f,-40f)) }
        mainClock.advanceTimeByFrame()
        assertTrue(abs(rim().red-held.red)<.08f,"Leaving must not reset the color phase")
        mainClock.advanceTimeBy(160)
        val outside=rim()
        select.performMouseInput { moveTo(center) }
        mainClock.advanceTimeByFrame()
        assertTrue(abs(rim().red-outside.red)<.08f,"Reentering must not reset the phase")
        select.performMouseInput { moveTo(Offset(-40f,-40f)); release() }
        assertTail()
        onNode(hasScrollToIndexAction()).assertDoesNotExist()
        assertEquals(0,changes)
        for(button in listOf(MouseButton.Secondary,MouseButton.Tertiary)) {
            select.performMouseInput { moveTo(center); press(button) }
            mainClock.advanceTimeBy(160)
            assertEquals(Color.Black,rim())
            select.performMouseInput { release(button) }
            mainClock.advanceTimeBy(200)
            assertEquals(Color.Black,rim())
        }
        onRoot().performMouseInput { moveTo(Offset(10f,10f)); press() }
        select.performMouseInput { moveTo(center) }
        mainClock.advanceTimeBy(160)
        assertEquals(Color.Black,rim(),"An outside press cannot acquire the rim")
        select.performMouseInput { release() }
        mainClock.advanceTimeBy(200)
        onNode(hasScrollToIndexAction()).assertDoesNotExist()
    }

    @Test fun openingKeepsThePhaseAndClosingFadesWithTheMenu() = runDesktopComposeUiTest(width=600,height=600) {
        mainClock.autoAdvance=false
        var selected by mutableIntStateOf(0)
        var changes=0
        setContent { Host { Box(Modifier.padding(100.dp)) { Select(selected=selected,onSelected={selected=it;changes++}) } } }
        val select=onNodeWithContentDescription("Choice")
        select.performMouseInput { enter(center); press() }
        mainClock.advanceTimeBy(480)
        val before=rim()
        select.performMouseInput { release() }
        mainClock.advanceTimeByFrame()
        assertTrue(abs(rim().red-before.red)<.08f,"Opening continues the held color phase")
        repeat(12) {
            mainClock.advanceTimeByFrame()
            assertTrue(rim().green>.75f,"Successful opening must not insert a release flash")
        }
        val list=onNode(hasScrollToIndexAction()).assertIsDisplayed()
        list.performMouseInput { moveTo(center); scroll(3f) }
        mainClock.advanceTimeBy(200)
        assertTrue(rim().green>.75f,"Scrolling an open list keeps the rim")
        list.performScrollToIndex(1)
        mainClock.advanceTimeBy(100)
        list.performMouseInput { exit() }
        // A still-settling wheel gesture can cancel a touch; activate explicitly to measure closing.
        onNodeWithText("Option 1").performClick()
        assertEquals(1,selected)
        val closing=List(12) { mainClock.advanceTimeByFrame(); rim().green }
        assertTrue(closing.any { it in .05f..0.7f },"Closing includes a visible fade: $closing")
        assertTrue(closing.zipWithNext().all { (before,after) -> after<=before+.02f },"Closing never flashes or rebounds: $closing")
        assertEquals(Color.Black,rim())
        list.assertDoesNotExist()
        assertEquals(1,selected)
        assertEquals(1,changes)
        select.performTouchInput { click() }
        val opening=List(12) { mainClock.advanceTimeByFrame(); rim().green }
        val firstVisible=opening.indexOfFirst { it>.75f }
        assertTrue(firstVisible in 0..2 && opening.drop(firstVisible).all { it>.75f },"A quick tap opens without flashing after the overlay mounts: $opening")
        onRoot().performTouchInput { click(Offset(20f,20f)) }
        mainClock.advanceTimeBy(160)
        assertEquals(Color.Black,rim())
        list.assertDoesNotExist()
        assertEquals(1,changes,"Outside dismissal does not change selection")
    }

    @Test fun touchCanScrollTheParentWhileHoldingAndReleaseOutsideFlashes() = runDesktopComposeUiTest(width=600,height=600) {
        mainClock.autoAdvance=false
        lateinit var scroll: ScrollState
        var changes=0
        setContent { Host {
            scroll=rememberScrollState()
            Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal=100.dp)) {
                Spacer(Modifier.height(180.dp))
                Select(onSelected={changes++})
                Spacer(Modifier.height(1000.dp))
            }
        } }
        val select=onNodeWithContentDescription("Choice")
        select.performTouchInput { down(center) }
        mainClock.advanceTimeBy(240)
        onRoot().performTouchInput { moveBy(Offset(0f,-35f)); moveBy(Offset(0f,-35f)) }
        mainClock.advanceTimeBy(160)
        assertTrue(scroll.value>0,"The rim-only overlay must not intercept scrolling")
        assertTrue(rim().green>.75f,"Scrolling preserves the held rim")
        onNode(hasScrollToIndexAction()).assertDoesNotExist()
        onRoot().performTouchInput { up() }
        assertTail()
        onNode(hasScrollToIndexAction()).assertDoesNotExist()
        assertEquals(0,changes)
    }

    @Test fun firstFingerOwnsTheHoldAndNewPressInterruptsTheTail() = runDesktopComposeUiTest(width=600,height=600) {
        mainClock.autoAdvance=false
        setContent { Host { Box(Modifier.padding(100.dp)) { Select() } } }
        val select=onNodeWithContentDescription("Choice")
        select.performTouchInput { down(0,center) }
        mainClock.advanceTimeBy(160)
        select.performTouchInput { down(1,center+Offset(10f,0f)); moveTo(0,Offset(-40f,-40f)) }
        mainClock.advanceTimeBy(160)
        assertTrue(rim().green>.75f)
        select.performTouchInput { up(0) }
        assertTail()
        select.performTouchInput { moveBy(1,Offset(10f,0f)); up(1) }
        mainClock.advanceTimeBy(200)
        assertEquals(Color.Black,rim())
        onNode(hasScrollToIndexAction()).assertDoesNotExist()
        select.performTouchInput { down(center); moveTo(Offset(-40f,-40f)); up() }
        mainClock.advanceTimeBy(48)
        select.performTouchInput { down(center) }
        repeat(12) { mainClock.advanceTimeByFrame(); assertTrue(rim().green>.75f,"A new press interrupts the old tail") }
        select.performTouchInput { up() }
        mainClock.advanceTimeBy(250)
        onNode(hasScrollToIndexAction()).assertExists()
    }

    @Test fun focusDisableAndRemovalClearFeedbackWithoutReopening() = runDesktopComposeUiTest(width=600,height=600) {
        mainClock.autoAdvance=false
        val window=object: WindowInfo { override var isWindowFocused by mutableStateOf(true) }
        var enabled by mutableStateOf(true)
        var shown by mutableStateOf(true)
        setContent { CompositionLocalProvider(LocalWindowInfo provides window) { Host {
            Box(Modifier.padding(100.dp)) { if(shown) Select(enabled=enabled) }
        } } }
        val select=onNodeWithContentDescription("Choice")
        for(reason in listOf("focus","disabled")) {
            select.performTouchInput { down(center) }
            mainClock.advanceTimeBy(160)
            runOnIdle { if(reason=="focus") window.isWindowFocused=false else enabled=false }
            mainClock.advanceTimeBy(32)
            assertEquals(Color.Black,rim())
            runOnIdle { window.isWindowFocused=true; enabled=true }
            mainClock.advanceTimeBy(32)
            select.performTouchInput { up() }
            mainClock.advanceTimeBy(200)
            assertEquals(Color.Black,rim())
            onNode(hasScrollToIndexAction()).assertDoesNotExist()
            select.performTouchInput { click() }
            mainClock.advanceTimeBy(250)
            onNode(hasScrollToIndexAction()).assertExists()
            runOnIdle { if(reason=="focus") window.isWindowFocused=false else enabled=false }
            mainClock.advanceTimeBy(32)
            assertEquals(Color.Black,rim())
            onNode(hasScrollToIndexAction()).assertDoesNotExist()
            runOnIdle { window.isWindowFocused=true; enabled=true }
            repeat(12) { mainClock.advanceTimeByFrame(); assertEquals(Color.Black,rim(),"Interrupted menus must not revive on recovery") }
        }
        select.performTouchInput { down(center) }
        mainClock.advanceTimeBy(160)
        val bounds=select.fetchSemanticsNode().boundsInRoot
        runOnIdle { shown=false }
        mainClock.advanceTimeBy(32)
        val pixels=onRoot().captureToImage().toPixelMap()
        assertEquals(Color.Black,pixels[bounds.center.x.roundToInt(),bounds.top.roundToInt()-1],"Removing the select removes its rim overlay")
        onRoot().performTouchInput { up() }
        runOnIdle { shown=true }
        mainClock.advanceTimeBy(200)
        assertEquals(Color.Black,rim())
    }

    @Test fun tooltipLongPressEndsWithARimFlashWithoutOpeningTheMenu() = runDesktopComposeUiTest(width=600,height=600) {
        mainClock.autoAdvance=false
        setContent { Host { Box(Modifier.padding(100.dp)) { ZenlessTooltip("Help") { Select() } } } }
        val select=onNodeWithContentDescription("Choice")
        select.performTouchInput { down(center) }
        mainClock.advanceTimeBy(800)
        onNodeWithText("Help").assertExists()
        assertTrue(rim().green>.75f)
        select.performTouchInput { up() }
        assertTail()
        onNode(hasScrollToIndexAction()).assertDoesNotExist()
    }

    @Composable private fun Host(content: @Composable ()->Unit) {
        CompositionLocalProvider(LocalDensity provides Density(1f)) {
            ZenlessTheme { ZenlessOverlayHost(Modifier.fillMaxSize().background(Color.Black),content) }
        }
    }
    @Composable private fun Select(enabled: Boolean=true,selected: Int=0,onSelected: (Int)->Unit={}) {
        ZenlessSelect(List(20) { "Option $it" },selected,onSelected,Modifier.width(300.dp),enabled=enabled,label="Choice")
    }
    private fun ComposeUiTest.rim(): Color {
        val bounds=onNodeWithContentDescription("Choice").fetchSemanticsNode().boundsInRoot
        return onRoot().captureToImage().toPixelMap()[bounds.center.x.roundToInt(),bounds.top.roundToInt()-1]
    }
    private fun ComposeUiTest.assertTail() {
        val amounts=List(12) { mainClock.advanceTimeByFrame(); rim().green }
        assertTrue(amounts.max()>.75f,"An outside or consumed release must flash the rim")
        assertTrue(amounts.min()<.1f,"The tail must contain its existing dark intervals")
        mainClock.advanceTimeBy(64)
        assertEquals(Color.Black,rim(),"The release tail must finish")
    }
    private fun SemanticsNodeInteraction.ink(): Color {
        val layouts=mutableListOf<TextLayoutResult>()
        performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        return layouts.single().layoutInput.style.color
    }
}
