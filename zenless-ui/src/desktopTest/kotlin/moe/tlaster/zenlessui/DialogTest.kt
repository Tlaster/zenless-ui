package moe.tlaster.zenlessui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.*
import org.jetbrains.skia.Image
import org.junit.Test
import java.io.File
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DialogTest {
    @Test fun dismissesOnceAfterExitBlocksOutsideAndRestoresFocus() = runDesktopComposeUiTest(width = 1100, height = 800) {
        mainClock.autoAdvance = false
        var visible by mutableStateOf(false)
        var dismissals = 0
        var backgroundClicks = 0
        val launcher = FocusRequester()
        setContent { ZenlessTheme { ZenlessOverlayHost {
            BasicTextField("Launcher", {}, Modifier.focusRequester(launcher).testTag("launcher"))
            Box(Modifier.padding(top = 80.dp).size(80.dp).clickable { backgroundClicks++ })
            ZenlessDialog(visible, "More actions", "Close dialog", { dismissals++; visible = false }, Modifier.testTag("dialog")) {
                ZenlessText("Dialog content")
            }
        } } }
        runOnIdle { launcher.requestFocus() }
        onNodeWithTag("launcher").assertIsFocused()
        runOnIdle { visible = true }
        mainClock.advanceTimeBy(350)
        onNodeWithTag("launcher").assertDoesNotExist()
        onRoot().performTouchInput { click(Offset(20f, 100f)) }
        runOnIdle { assertEquals(0, backgroundClicks); assertEquals(0, dismissals) }
        onNodeWithContentDescription("Close dialog").performTouchInput { click() }
        mainClock.advanceTimeBy(48)
        onNodeWithText("Dialog content").assertExists()
        onRoot().performKeyInput { pressKey(Key.Escape) }
        runOnIdle { assertEquals(0, dismissals) }
        mainClock.advanceTimeBy(250)
        runOnIdle { assertEquals(1, dismissals) }
        onNodeWithTag("dialog").assertDoesNotExist()
        onNodeWithTag("launcher").assertIsFocused()
        runOnIdle { visible = true }
        mainClock.advanceTimeBy(300)
        onRoot().performKeyInput { pressKey(Key.Escape) }
        mainClock.advanceTimeBy(250)
        runOnIdle { assertEquals(2, dismissals) }
    }

    @Test fun narrowWindowScrollsOnlyBodyAndResetsOnReopen() = runDesktopComposeUiTest(width = 390, height = 650) {
        mainClock.autoAdvance = false
        var visible by mutableStateOf(true)
        setContent { CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) { ZenlessTheme { ZenlessOverlayHost {
            ZenlessDialog(visible, "A long title that must leave the close button visible", "Close", { visible = false }, Modifier.testTag("dialog")) {
                repeat(30) { ZenlessText("Content row $it", Modifier.heightIn(min = 48.dp)) }
            }
        } } } }
        mainClock.advanceTimeBy(350)
        val header = onNodeWithContentDescription("Close").fetchSemanticsNode().boundsInRoot
        val window = onNodeWithTag("dialog").fetchSemanticsNode().boundsInRoot
        assertTrue(window.left >= 15 && window.right <= 375 && window.top >= 15 && window.bottom <= 635)
        onNode(hasScrollAction()).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy) { it(0f, 10000f) }
        mainClock.advanceTimeBy(500)
        onNodeWithText("Content row 29").assertIsDisplayed()
        assertEquals(header, onNodeWithContentDescription("Close").fetchSemanticsNode().boundsInRoot)
        save("dialog-narrow")
        runOnIdle { visible = false }
        mainClock.advanceTimeBy(250)
        runOnIdle { visible = true }
        mainClock.advanceTimeBy(350)
        onNodeWithText("Content row 0").assertIsDisplayed()
    }

    @Test fun interruptedExitReversesWithoutJumpOrDismissCallback() = runDesktopComposeUiTest(width = 1100, height = 800) {
        mainClock.autoAdvance = false
        var visible by mutableStateOf(true)
        var dismissals = 0
        setContent { ZenlessTheme { ZenlessOverlayHost {
            ZenlessDialog(visible, "Interrupt", "Close", { dismissals++; visible = false }, Modifier.testTag("dialog")) { ZenlessText("Body") }
        } } }
        mainClock.advanceTimeBy(350)
        val settled = onNodeWithContentDescription("Close").fetchSemanticsNode().boundsInRoot.left
        runOnIdle { visible = false }
        mainClock.advanceTimeBy(80)
        val departing = onNodeWithContentDescription("Close").fetchSemanticsNode().boundsInRoot.left
        assertTrue(departing < settled, "$departing must be left of $settled")
        runOnIdle { visible = true }
        mainClock.advanceTimeByFrame()
        val turning = onNodeWithContentDescription("Close").fetchSemanticsNode().boundsInRoot.left
        assertTrue(kotlin.math.abs(turning - departing) < 15f)
        mainClock.advanceTimeBy(300)
        assertEquals(settled, onNodeWithContentDescription("Close").fetchSemanticsNode().boundsInRoot.left, .5f)
        runOnIdle { assertEquals(0, dismissals) }
    }

    @Test fun referenceWindowAndCustomBackgroundAtNativeDensity() = runDesktopComposeUiTest(width = 3840, height = 2160) {
        mainClock.autoAdvance = false
        setContent { CompositionLocalProvider(LocalDensity provides Density(2f)) { ZenlessTheme { ZenlessOverlayHost {
            Box(Modifier.fillMaxSize().background(Color(0xff464646)))
            ZenlessDialog(true, "更多操作", "关闭", {}, Modifier.testTag("dialog")) {
                repeat(3) { row -> Row(Modifier.padding(end = 1.5.dp), horizontalArrangement = Arrangement.spacedBy(24.5.dp)) {
                    ZenlessButton({}, Modifier.weight(1f).height(58.dp)) { ZenlessText(listOf("修改用户名", "修改签名", "社交设置")[row]) }
                    if (row < 2) ZenlessButton({}, Modifier.weight(1f).height(58.dp)) { ZenlessText(if (row == 0) "公开生日信息" else "修改代理人展柜") }
                    else Spacer(Modifier.weight(1f))
                } }
            }
        } } } }
        mainClock.advanceTimeBy(400)
        onNodeWithTag("dialog").assertWidthIsEqualTo(762.dp).assertHeightIsEqualTo(482.dp)
        save("dialog-reference")
    }

    @Test fun backgroundComposesOneTileHidesSemanticsAndMovesWithoutRecomposition() = runDesktopComposeUiTest(width = 640, height = 400) {
        mainClock.autoAdvance = false
        var mounts = 0
        var compositions = 0
        var clicks = 0
        var tall by mutableStateOf(false)
        setContent { ZenlessTheme {
            ZenlessAnimatedBackground(Modifier.size(640.dp, 400.dp).testTag("background")) {
                DisposableEffect(Unit) { mounts++; onDispose { mounts-- } }
                SideEffect { compositions++ }
                Box(Modifier.size(140.dp, if (tall) 100.dp else 70.dp).clickable { clicks++ }.testTag("tile")) {
                    Box(Modifier.padding(12.dp).size(70.dp, 36.dp).background(Color.White))
                    ZenlessText("Hidden decorative text")
                }
            }
        } }
        mainClock.advanceTimeBy(32)
        val first = onRoot().captureToImage().toPixelMap()
        val count = compositions
        mainClock.advanceTimeBy(1600)
        val second = onRoot().captureToImage().toPixelMap()
        assertTrue((20 until 620 step 8).sumOf { x -> (20 until 380 step 8).count { y -> first[x,y] != second[x,y] } } > 50)
        runOnIdle { assertEquals(1, mounts); assertEquals(count, compositions) }
        onNodeWithText("Hidden decorative text").assertDoesNotExist()
        onRoot().performTouchInput { click(Offset(30f, 30f)) }
        runOnIdle { assertEquals(0, clicks); tall = true }
        mainClock.advanceTimeBy(100)
        runOnIdle { assertEquals(1, mounts) }
        save("animated-background")
    }

    @Test fun zeroSizeTileIsStationaryAndSafe() = runDesktopComposeUiTest(width = 320, height = 180) {
        mainClock.autoAdvance = false
        setContent { ZenlessAnimatedBackground { Spacer(Modifier.size(0.dp)) } }
        mainClock.advanceTimeBy(32)
        val first = onRoot().captureToImage().toPixelMap()
        mainClock.advanceTimeBy(500)
        val second = onRoot().captureToImage().toPixelMap()
        for (y in 0 until 180 step 7) for (x in 0 until 320 step 7) assertEquals(first[x,y], second[x,y])
    }

    @Test fun tinyTilesAreBoundedAndCustomDialogDecorationCannotTakeInput() = runDesktopComposeUiTest(width = 1100, height = 800) {
        mainClock.autoAdvance = false
        var decorationClicks = 0
        var behindFocused = false
        val field = FocusRequester()
        setContent { ZenlessTheme { ZenlessOverlayHost {
            BasicTextField("Behind", {}, Modifier.onFocusChanged { behindFocused = it.isFocused })
            ZenlessDialog(true, "Decoration", "Close", {}, background = {
                Box(Modifier.fillMaxSize().clickable { decorationClicks++ }) {
                    ZenlessAnimatedBackground { Spacer(Modifier.size(1.dp).background(Color.White)) }
                    ZenlessText("Hidden background label")
                }
            }) {
                BasicTextField("Inside", {}, Modifier.focusRequester(field).testTag("inside"))
            }
        } } }
        mainClock.advanceTimeBy(350)
        onNodeWithText("Hidden background label").assertDoesNotExist()
        runOnIdle { field.requestFocus() }
        onNodeWithTag("inside").assertIsFocused()
        repeat(4) { onRoot().performKeyInput { pressKey(Key.Tab) }; mainClock.advanceTimeByFrame() }
        runOnIdle { assertFalse(behindFocused) }
        onRoot().performTouchInput { click(Offset(200f, 430f)) }
        mainClock.advanceTimeBy(64)
        runOnIdle { assertEquals(0, decorationClicks) }
    }

    private fun ComposeUiTest.save(name: String) {
        val file = File("../verification/$name.png")
        file.parentFile.mkdirs()
        Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image -> image.encodeToData()!!.use { file.writeBytes(it.bytes) } }
    }
}
