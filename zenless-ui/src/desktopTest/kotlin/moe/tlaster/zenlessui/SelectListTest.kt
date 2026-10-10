package moe.tlaster.zenlessui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SelectListTest {
    @Test fun largeListStartsAtSelectionAndOnlyReadsNearbyOptions() = runDesktopComposeUiTest(width=400,height=700) {
        mainClock.autoAdvance=false
        val reads=mutableSetOf<Int>()
        val options=object:AbstractList<String>() {
            override val size=1000
            override fun get(index:Int):String { check(index in indices);reads+=index;return "Option $index" }
        }
        setContent { CompositionLocalProvider(LocalDensity provides Density(1f)) { ZenlessTheme { ZenlessOverlayHost {
            Column(Modifier.padding(20.dp)) { ZenlessSelect(options,995,{},Modifier.width(320.dp),label="Choice") }
        } } } }
        onNodeWithContentDescription("Choice").performClick();mainClock.advanceTimeBy(300)
        onNode(hasText("Option 995") and isSelected()).assertIsDisplayed()
        runOnIdle {
            assertTrue(reads.size<40,"Opening must read a bounded viewport, not all 1000 options: ${reads.size}")
            assertFalse(0 in reads,"The popup must start at the selected item instead of first composing the top")
        }
        val list=onNode(hasScrollToIndexAction())
        fun position()=list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        val initial=position()
        list.performMouseInput { enter(center);scroll(-4f) };mainClock.advanceTimeBy(400)
        assertTrue(position()<initial,"Mouse wheel scrolls toward preceding options")
        val beforeTouch=position()
        list.performTouchInput { swipeDown() };mainClock.advanceTimeBy(400)
        assertTrue(position()<beforeTouch,"Touch dragging scrolls toward preceding options")
        list.performScrollToIndex(0);mainClock.advanceTimeByFrame()
        onNodeWithText("Option 0").assertIsDisplayed()
        onRoot().performTouchInput { click(Offset(390f,20f)) };mainClock.advanceTimeBy(200)
        list.assertDoesNotExist()
        onNodeWithContentDescription("Choice").performClick();mainClock.advanceTimeBy(300)
        onNode(hasText("Option 995") and isSelected()).assertIsDisplayed()
    }

    @Test fun openListTracksExternalSelectionAndSupportsDuplicateAndShrinkingOptions() = runDesktopComposeUiTest(width=400,height=700) {
        mainClock.autoAdvance=false
        var options by mutableStateOf(List(1000) { "Option $it" })
        var selected by mutableIntStateOf(900)
        var enabled by mutableStateOf(true)
        var callbacks=0
        setContent { CompositionLocalProvider(LocalDensity provides Density(1f)) { ZenlessTheme { ZenlessOverlayHost {
            Column(Modifier.padding(20.dp)) {
                ZenlessSelect(options,selected,{selected=it;callbacks++},Modifier.width(320.dp),enabled=enabled,label="Choice")
            }
        } } } }
        onNodeWithContentDescription("Choice").performClick();mainClock.advanceTimeBy(300)
        runOnIdle { selected=500 };mainClock.advanceTimeBy(250)
        onNode(hasText("Option 500") and isSelected()).assertIsDisplayed()
        runOnIdle { options=listOf("Same","Same","Last");selected=1 };mainClock.advanceTimeBy(250)
        onNode(hasText("Same") and isSelected()).assertIsDisplayed()
        onAllNodesWithText("Same").assertCountEquals(3)
        onNode(hasText("Same") and isNotSelected()).performClick();mainClock.advanceTimeBy(200)
        runOnIdle { assertEquals(0,selected);assertEquals(1,callbacks) }
        onNodeWithContentDescription("Choice").performClick();mainClock.advanceTimeBy(300)
        onNode(isSelected()).performClick();mainClock.advanceTimeBy(200)
        runOnIdle { assertEquals(1,callbacks,"Choosing the current item does not notify again") }
        onNodeWithContentDescription("Choice").performClick();mainClock.advanceTimeBy(300)
        runOnIdle { options=listOf("Only") };mainClock.advanceTimeBy(250)
        onNode(hasText("Only") and isSelected()).assertIsDisplayed()
        val height=onNode(hasScrollToIndexAction()).fetchSemanticsNode().boundsInRoot.height
        assertTrue(height<=62f,"A one-item popup shrinks to content, not the five-row maximum: $height")
        runOnIdle { enabled=false };mainClock.advanceTimeBy(200)
        onNode(hasScrollToIndexAction()).assertDoesNotExist()
        onNodeWithContentDescription("Choice").assertIsNotEnabled()
    }

    @Test fun largeTextAndBottomAnchorKeepSelectedRowsInsideTheViewport() = runDesktopComposeUiTest(width=400,height=500) {
        mainClock.autoAdvance=false
        var options by mutableStateOf(listOf("One","Two"))
        var selected by mutableIntStateOf(0)
        setContent { CompositionLocalProvider(LocalDensity provides Density(1f,2.5f)) { ZenlessTheme { ZenlessOverlayHost {
            Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.Bottom) {
                ZenlessSelect(options,selected,{selected=it},Modifier.width(320.dp),label="Choice")
            }
        } } } }
        onNodeWithContentDescription("Choice").performClick();mainClock.advanceTimeBy(300)
        val anchor=onNodeWithContentDescription("Choice").fetchSemanticsNode().boundsInRoot
        val short=onNode(hasScrollToIndexAction()).fetchSemanticsNode().boundsInRoot
        assertTrue(short.top>=0 && short.bottom<anchor.top,"The popup flips above the bottom anchor")
        assertTrue(short.height<200,"Two large-text options still shrink to content")
        runOnIdle { options=List(1000) { "Item $it" };selected=999 };mainClock.advanceTimeBy(300)
        val item=onNode(hasText("Item 999") and isSelected()).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val popup=onNode(hasScrollToIndexAction()).fetchSemanticsNode().boundsInRoot
        assertTrue(item.top>=popup.top && item.bottom<=popup.bottom,"Variable-height selected row is fully visible")
        assertTrue(popup.top>=0 && popup.bottom<anchor.top)
    }
}
