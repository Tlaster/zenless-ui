package moe.tlaster.zenlessui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun quickTapFlashesEvenWhenPressAndReleaseShareAFrame() {
        val source = MutableInteractionSource()
        lateinit var feedback: Feedback
        compose.mainClock.autoAdvance = false
        compose.setContent { feedback = rememberFeedback(source, selected = false, enabled = true, buttonFeedback = true) }
        compose.mainClock.advanceTimeByFrame()
        val press = PressInteraction.Press(Offset.Zero)
        compose.runOnIdle { source.tryEmit(press); source.tryEmit(PressInteraction.Release(press)) }
        var peak = 0f
        repeat(12) {
            compose.mainClock.advanceTimeByFrame()
            compose.runOnIdle { peak = maxOf(peak, feedback.highlight) }
        }
        assertTrue(peak > .9f, "A quick tap must retain the release flash")
        compose.mainClock.advanceTimeBy(200)
        compose.runOnIdle { assertEquals(0f, feedback.highlight) }
        compose.runOnIdle { source.tryEmit(press); source.tryEmit(PressInteraction.Cancel(press)) }
        compose.mainClock.advanceTimeBy(64)
        compose.runOnIdle { assertEquals(0f, feedback.highlight, "Cancelled gestures must not flash") }
        val nextPress=PressInteraction.Press(Offset.Zero)
        compose.runOnIdle {
            source.tryEmit(press); source.tryEmit(PressInteraction.Release(press))
            source.tryEmit(nextPress); source.tryEmit(PressInteraction.Cancel(nextPress))
        }
        repeat(12) {
            compose.mainClock.advanceTimeByFrame()
            compose.runOnIdle { assertEquals(0f,feedback.highlight,"A newer cancellation must discard an older release in the same frame") }
        }
    }

    @Test fun selectionFeedbackDoesNotPulseOrFlashFromPointerEvents() {
        val source=MutableInteractionSource()
        lateinit var idle:Feedback
        lateinit var selected:Feedback
        compose.mainClock.autoAdvance=false
        compose.setContent {
            idle=rememberFeedback(source,selected=false,enabled=true)
            selected=rememberFeedback(source,selected=true,enabled=true)
        }
        compose.mainClock.advanceTimeBy(200)
        val press=PressInteraction.Press(Offset.Zero)
        compose.runOnIdle { source.tryEmit(press) }
        compose.mainClock.advanceTimeBy(800)
        compose.runOnIdle { assertEquals(0f,idle.highlight);assertEquals(1f,selected.highlight) }
        compose.runOnIdle { source.tryEmit(PressInteraction.Release(press)) }
        repeat(12) {
            compose.mainClock.advanceTimeByFrame()
            compose.runOnIdle {
                assertEquals(0f,idle.highlight,"An unselected control must not flash after release")
                assertEquals(1f,selected.highlight,"Selection must not blink off after release")
            }
        }
        compose.runOnIdle { source.tryEmit(press);source.tryEmit(PressInteraction.Release(press)) }
        repeat(12) {
            compose.mainClock.advanceTimeByFrame()
            compose.runOnIdle { assertEquals(0f,idle.highlight);assertEquals(1f,selected.highlight) }
        }
    }

    @Test fun controlledInputsAndDisabledButton() {
        var state = ToggleableState.Indeterminate
        var radio = 0
        var switched = false
        var clicks = 0
        compose.mainClock.autoAdvance = false
        compose.setContent {
            ZenlessTheme {
                Column {
                    ZenlessCheckbox(state, { state = it }, Modifier.testTag("mixed"), label = "Select all")
                    ZenlessRadioButton(false, { radio = 1 }, Modifier.testTag("radio"), label = "Second")
                    ZenlessSwitch(false, { switched = it }, Modifier.testTag("switch"), label = "Switch")
                    ZenlessButton({ clicks++ }, Modifier.testTag("disabled"), enabled = false) { ZenlessText("Disabled") }
                    ZenlessButton({ clicks++ }, Modifier.testTag("loading"), loading = true) { ZenlessText("Loading") }
                }
            }
        }
        compose.onNodeWithTag("mixed").performTouchInput { click() }
        compose.onNodeWithTag("radio").performTouchInput { click() }
        compose.onNodeWithTag("switch").performTouchInput { click() }
        compose.onNodeWithTag("disabled").assertIsNotEnabled().performTouchInput { click() }
        compose.onNodeWithTag("loading").assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(ToggleableState.On, state); assertEquals(1, radio); assertEquals(true, switched); assertEquals(0, clicks) }
    }

    @Test fun numericSliderAcceptsIntermediateTextAndHonorsSteps() {
        var value by mutableFloatStateOf(0f)
        compose.mainClock.autoAdvance=false
        compose.setContent { ZenlessTheme { ZenlessSlider(value,{value=it},Modifier.width(300.dp),steps=3) } }
        val number=compose.onNode(hasSetTextAction())
        number.performClick().performTextClearance()
        number.performTextInput("0.")
        number.performTextInput("6")
        compose.runOnIdle { assertEquals(.5f,value) }
        number.performTextReplacement("NaN")
        compose.runOnIdle { assertEquals(.5f,value) }
    }

    @Test fun longSelectFitsBetweenAnchorAndViewportEdge() {
        compose.mainClock.autoAdvance=false
        compose.setContent { ZenlessTheme { ZenlessOverlayHost(Modifier.size(400.dp,320.dp)) {
            Box(Modifier.padding(top=120.dp)) { ZenlessSelect((1..20).map { "Option $it" },0,{},Modifier.width(240.dp),label="Long list") }
        } } }
        compose.onNodeWithContentDescription("Long list").performTouchInput { click() }
        compose.mainClock.advanceTimeBy(300)
        val options=compose.onAllNodesWithText("Option 1").fetchSemanticsNodes()
        assertEquals(2,options.size)
        val root=compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue(options.all { it.boundsInRoot.top>=root.top && it.boundsInRoot.bottom<=root.bottom })
        compose.onNodeWithText("Option 2").performTouchInput { click() }
        compose.mainClock.advanceTimeBy(200)
    }

    @Test fun alertConfirmsOnceAfterExitAndBlocksUnderlyingButton() {
        var count = 0
        compose.mainClock.autoAdvance = false
        compose.setContent {
            var open by remember { mutableStateOf(true) }
            ZenlessTheme { ZenlessOverlayHost(Modifier.size(800.dp, 600.dp)) {
                ZenlessButton({ count += 100 }) { ZenlessText("Underlying") }
                ZenlessAlert(open, "Apply?", "Confirm", { count++; open = false }, { open = false }, "Cancel")
            } }
        }
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithText("Underlying").assertDoesNotExist()
        compose.onRoot().performTouchInput { click(androidx.compose.ui.geometry.Offset(10f, 10f)) }
        compose.runOnIdle { assertEquals(0, count) }
        compose.onNodeWithText("Confirm").performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, count) }
        compose.mainClock.advanceTimeBy(300)
        compose.runOnIdle { assertEquals(1, count) }
        compose.onNodeWithText("Apply?").assertDoesNotExist()
    }

    @Test fun loadingPreservesButtonSize() {
        var loading by mutableStateOf(false)
        compose.mainClock.autoAdvance = false
        compose.setContent { ZenlessTheme {
            ZenlessButton({}, Modifier.testTag("button"), loading = loading) { ZenlessText("Continue") }
        } }
        val size = compose.onNodeWithTag("button").fetchSemanticsNode().size
        compose.runOnIdle { loading = true }
        compose.mainClock.advanceTimeByFrame()
        assertEquals(size, compose.onNodeWithTag("button").fetchSemanticsNode().size)
        compose.onNodeWithTag("button").assertIsNotEnabled()
    }

    @Test fun selectChangesOnceAndCloses() {
        var selected by mutableIntStateOf(0)
        var count = 0
        compose.mainClock.autoAdvance = false
        compose.setContent { ZenlessTheme { ZenlessOverlayHost(Modifier.size(600.dp, 600.dp)) {
            ZenlessSelect(listOf("One", "Two"), selected, { selected = it; count++ }, Modifier.width(240.dp), label = "Choice")
        } } }
        compose.onNodeWithContentDescription("Choice").performTouchInput { click() }
        compose.mainClock.advanceTimeBy(250)
        compose.onNodeWithText("Two").performTouchInput { click() }
        compose.mainClock.advanceTimeBy(200)
        compose.runOnIdle { assertEquals(1, selected); assertEquals(1, count) }
        compose.onAllNodesWithText("Two").assertCountEquals(1)
        compose.onNodeWithText("One").assertDoesNotExist()
    }

    @Test fun tooltipLongPressDoesNotActivateAnchorAndOutsideTapDismisses() {
        var clicks = 0
        compose.mainClock.autoAdvance = false
        compose.setContent { ZenlessTheme { ZenlessOverlayHost(Modifier.size(600.dp, 600.dp)) {
            ZenlessTooltip("Helpful context", Modifier.padding(100.dp)) {
                ZenlessButton({ clicks++ }, Modifier.testTag("anchor")) { ZenlessText("Details") }
            }
        } } }
        compose.onNodeWithTag("anchor").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(800)
        compose.onNodeWithText("Helpful context").assertExists()
        compose.onNodeWithTag("anchor").performTouchInput { up() }
        compose.runOnIdle { assertEquals(0, clicks) }
        compose.onRoot().performTouchInput { click(androidx.compose.ui.geometry.Offset(10f, 10f)) }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Helpful context").assertDoesNotExist()
    }
}
