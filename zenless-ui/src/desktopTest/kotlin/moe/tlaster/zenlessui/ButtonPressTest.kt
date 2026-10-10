package moe.tlaster.zenlessui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class ButtonPressTest {
    @Test fun mouseTracksTheOriginalPressOutsideAndFlashesOnEitherRelease() = runDesktopComposeUiTest(width=500,height=400) {
        mainClock.autoAdvance=false
        lateinit var feedback: Feedback
        var clicks=0
        setContent { Box(Modifier.fillMaxSize().padding(100.dp)) { Probe(onFeedback={feedback=it},onClick={clicks++}) } }
        val button=onNodeWithTag("button")
        button.performMouseInput { enter(center); press() }
        mainClock.advanceTimeBy(240)
        val start=feedback.seconds
        button.performMouseInput { moveTo(Offset(-50f,-50f)) }
        mainClock.advanceTimeBy(320)
        assertEquals(1f,feedback.highlight)
        assertTrue(feedback.seconds>start,"Leaving must keep the animation's phase")
        val outside=feedback.seconds
        button.performMouseInput { moveTo(center) }
        mainClock.advanceTimeBy(80)
        assertTrue(feedback.seconds>outside,"Reentering must not restart the animation")
        button.performMouseInput { moveTo(Offset(-50f,-50f)); release() }
        assertFlash(feedback)
        assertEquals(0,clicks,"An outside release must not activate the button")
        button.performMouseInput { moveTo(center); press(); release() }
        assertFlash(feedback)
        assertEquals(1,clicks)
        for(mouseButton in listOf(MouseButton.Secondary,MouseButton.Tertiary)) {
            button.performMouseInput { press(mouseButton) }
            mainClock.advanceTimeBy(80)
            assertEquals(0f,feedback.highlight)
            button.performMouseInput { release(mouseButton) }
            mainClock.advanceTimeBy(240)
            assertEquals(0f,feedback.highlight)
        }
        onRoot().performMouseInput { moveTo(Offset(10f,10f)); press() }
        button.performMouseInput { moveTo(center) }
        mainClock.advanceTimeBy(80)
        assertEquals(0f,feedback.highlight,"Dragging in cannot start a hold")
        button.performMouseInput { release() }
        mainClock.advanceTimeBy(240)
        assertEquals(1,clicks)
    }

    @Test fun scrollingKeepsTouchFeedbackWithoutActivatingTheButton() = runDesktopComposeUiTest(width=500,height=500) {
        mainClock.autoAdvance=false
        lateinit var scroll: ScrollState
        var ink=Color.White
        var clicks=0
        setContent { ZenlessTheme {
            scroll=rememberScrollState()
            Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                Spacer(Modifier.height(100.dp))
                ZenlessButton({clicks++},Modifier.size(300.dp,160.dp).testTag("button")) {
                    val color=zenlessContentColor
                    SideEffect { ink=color }
                    ZenlessText("Scroll from here")
                }
                Spacer(Modifier.height(1000.dp))
            }
        } }
        val button=onNodeWithTag("button")
        button.performTouchInput { down(center) }
        mainClock.advanceTimeBy(200)
        onRoot().performTouchInput { moveBy(Offset(0f,-40f)); moveBy(Offset(0f,-30f)) }
        mainClock.advanceTimeBy(160)
        assertTrue(scroll.value>0,"The same touch must scroll the parent")
        assertEquals(Color.Black,ink,"Scrolling must not cancel the visual hold")
        onRoot().performTouchInput { up() }
        var flashed=false
        repeat(12) { mainClock.advanceTimeByFrame(); flashed=flashed || ink==Color.Black }
        assertTrue(flashed,"A scroll release still flashes")
        mainClock.advanceTimeBy(240)
        assertEquals(Color.White,ink)
        assertEquals(0,clicks)
    }

    @Test fun secondFingerDoesNotTakeOverOrExtendTheFirstHold() = runDesktopComposeUiTest(width=400,height=300) {
        mainClock.autoAdvance=false
        lateinit var feedback: Feedback
        var clicks=0
        setContent { Probe(onFeedback={feedback=it},onClick={clicks++}) }
        val button=onNodeWithTag("button")
        button.performTouchInput { down(0,center) }
        mainClock.advanceTimeBy(200)
        button.performTouchInput { down(1,center+Offset(10f,0f)); moveTo(0,Offset(-60f,-60f)) }
        mainClock.advanceTimeBy(200)
        assertEquals(1f,feedback.highlight)
        button.performTouchInput { up(0) }
        assertFlash(feedback)
        button.performTouchInput { moveBy(1,Offset(10f,0f)) }
        mainClock.advanceTimeBy(200)
        assertEquals(0f,feedback.highlight)
        button.performTouchInput { up(1) }
        mainClock.advanceTimeBy(200)
        assertEquals(0f,feedback.highlight)
        assertEquals(0,clicks)
    }

    @Test fun newPressInterruptsTheTail() = runDesktopComposeUiTest(width=400,height=300) {
        mainClock.autoAdvance=false
        lateinit var feedback: Feedback
        setContent { Probe(onFeedback={feedback=it}) }
        val button=onNodeWithTag("button")
        button.performTouchInput { down(center); up() }
        mainClock.advanceTimeBy(64)
        assertTrue(feedback.release>=0f)
        button.performTouchInput { down(center) }
        mainClock.advanceTimeByFrame()
        assertEquals(-1f,feedback.release,"A new press immediately interrupts the tail")
        assertEquals(1f,feedback.highlight)
        button.performTouchInput { up() }
        assertFlash(feedback)
        button.performTouchInput { down(center); up(); down(center) }
        mainClock.advanceTimeByFrame()
        assertEquals(-1f,feedback.release,"A release and new press in one frame must not queue a tail")
        assertEquals(1f,feedback.highlight)
        button.performTouchInput { up() }
        assertFlash(feedback)
    }

    @Test fun systemCancellationHasNoTail() = runDesktopComposeUiTest(width=400,height=300) {
        mainClock.autoAdvance=false
        val source=MutableInteractionSource()
        val node=ButtonPressNode(source,true)
        lateinit var feedback: Feedback
        setContent { feedback=rememberFeedback(source,false,true,buttonFeedback=true) }
        mainClock.advanceTimeByFrame()
        runOnIdle {
            node.onPointerEvent(PointerEvent(listOf(PointerInputChange(
                id=PointerId(0),uptimeMillis=0,position=Offset(20f,20f),pressed=true,
                previousUptimeMillis=0,previousPosition=Offset(20f,20f),previousPressed=false,type=PointerType.Touch,
                pressure=1f,isInitiallyConsumed=false,
            ))),PointerEventPass.Initial,IntSize(200,100))
        }
        mainClock.advanceTimeBy(160)
        assertEquals(1f,feedback.highlight)
        // Desktop's TouchInjectionScope.cancel() is a no-op; exercise the platform callback directly.
        runOnIdle { node.onCancelPointerInput() }
        repeat(12) { mainClock.advanceTimeByFrame(); assertEquals(0f,feedback.highlight,"System cancellation never flashes") }
    }

    @Test fun disablingLosingFocusAndRemovalClearTheHoldAndTail() = runDesktopComposeUiTest(width=400,height=300) {
        mainClock.autoAdvance=false
        val window=object : WindowInfo { override var isWindowFocused by mutableStateOf(true) }
        var enabled by mutableStateOf(true)
        var shown by mutableStateOf(true)
        var clicks=0
        lateinit var feedback: Feedback
        setContent { CompositionLocalProvider(LocalWindowInfo provides window) {
            Box(Modifier.fillMaxSize()) { if(shown) Probe(enabled,onFeedback={feedback=it},onClick={clicks++}) }
        } }
        val button=onNodeWithTag("button")
        for(interruption in listOf("disabled","focus")) {
            button.performTouchInput { down(center) }
            mainClock.advanceTimeBy(160)
            runOnIdle { if(interruption=="disabled") enabled=false else window.isWindowFocused=false }
            mainClock.advanceTimeBy(32)
            assertEquals(0f,feedback.highlight)
            runOnIdle { enabled=true; window.isWindowFocused=true }
            mainClock.advanceTimeBy(32)
            assertEquals(0f,feedback.highlight,"Resuming must require a new press")
            button.performTouchInput { up() }
            repeat(12) { mainClock.advanceTimeByFrame(); assertEquals(0f,feedback.highlight) }
            assertEquals(0,clicks,"Interrupted gestures cannot activate after resuming")
        }
        button.performTouchInput { click() }
        mainClock.advanceTimeBy(48)
        runOnIdle { window.isWindowFocused=false }
        mainClock.advanceTimeBy(32)
        assertEquals(0f,feedback.highlight,"Focus loss also stops an existing tail")
        runOnIdle { window.isWindowFocused=true }
        mainClock.advanceTimeBy(32)
        button.performTouchInput { down(center) }
        mainClock.advanceTimeBy(160)
        runOnIdle { shown=false }
        mainClock.advanceTimeBy(32)
        onRoot().performTouchInput { up() }
        runOnIdle { shown=true }
        mainClock.advanceTimeBy(200)
        assertEquals(0f,feedback.highlight,"A replacement must not inherit the removed button's hold")
    }

    @Test fun loadingInterruptsTheActualButtonAndDoesNotResumeOnRelease() = runDesktopComposeUiTest(width=400,height=300) {
        mainClock.autoAdvance=false
        var loading by mutableStateOf(false)
        var ink=Color.White
        var clicks=0
        setContent { ZenlessTheme { ZenlessButton({clicks++},Modifier.testTag("button"),loading=loading) {
            val color=zenlessContentColor
            SideEffect { ink=color }
            ZenlessText("Action")
        } } }
        val button=onNodeWithTag("button")
        button.performTouchInput { down(center) }
        mainClock.advanceTimeBy(160)
        assertEquals(Color.Black,ink)
        runOnIdle { loading=true }
        mainClock.advanceTimeBy(32)
        assertEquals(Color.White,ink)
        runOnIdle { loading=false }
        mainClock.advanceTimeBy(32)
        button.performTouchInput { up() }
        repeat(12) { mainClock.advanceTimeByFrame(); assertEquals(Color.White,ink) }
        assertEquals(0,clicks)
    }

    @Test fun tooltipLongPressAllowsScrollingAndStillFlashesWithoutClicking() = runDesktopComposeUiTest(width=500,height=500) {
        mainClock.autoAdvance=false
        lateinit var feedback: Feedback
        lateinit var scroll: ScrollState
        var clicks=0
        setContent { ZenlessTheme { ZenlessOverlayHost {
            scroll=rememberScrollState()
            Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                Spacer(Modifier.height(100.dp))
                ZenlessTooltip("Helpful context") { Probe(onFeedback={feedback=it},onClick={clicks++}) }
                Spacer(Modifier.height(1000.dp))
            }
        } } }
        val button=onNodeWithTag("button")
        button.performTouchInput { down(center) }
        mainClock.advanceTimeBy(800)
        onNodeWithText("Helpful context").assertExists()
        assertEquals(1f,feedback.highlight)
        onRoot().performTouchInput { moveBy(Offset(0f,-30f)); moveBy(Offset(0f,-30f)) }
        mainClock.advanceTimeBy(80)
        onNodeWithText("Helpful context").assertDoesNotExist()
        assertTrue(scroll.value>0,"A visible tooltip must not block scrolling")
        assertEquals(1f,feedback.highlight)
        onRoot().performTouchInput { up() }
        assertFlash(feedback)
        assertEquals(0,clicks)
        button.performTouchInput { down(center) }
        mainClock.advanceTimeBy(800)
        onNodeWithText("Helpful context").assertExists()
        button.performTouchInput { up() }
        assertFlash(feedback)
        assertEquals(0,clicks,"A stationary long press must also suppress activation")
    }

    @Composable
    private fun Probe(enabled: Boolean=true, onFeedback: (Feedback)->Unit, onClick: ()->Unit={}) {
        val source=remember { MutableInteractionSource() }
        val feedback=rememberFeedback(source,false,enabled,buttonFeedback=true)
        SideEffect { onFeedback(feedback) }
        Box(Modifier.size(200.dp,100.dp).testTag("button").plate(feedback).buttonClick(enabled,source,onClick))
    }

    private fun ComposeUiTest.assertFlash(feedback: Feedback) {
        var peak=0f
        repeat(12) { mainClock.advanceTimeByFrame(); peak=maxOf(peak,feedback.highlight) }
        assertTrue(peak>.9f,"An actual release must play the existing flash")
        mainClock.advanceTimeBy(64)
        assertEquals(0f,feedback.highlight,"The release flash must finish")
    }
}
