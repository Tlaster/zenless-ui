package moe.tlaster.zenlessui.gallery

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.jetbrains.skia.Image
import java.io.File
import java.util.Locale
import org.junit.Before
import org.junit.After
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GalleryTest {
    @get:Rule val compose = createComposeRule()
    private val originalLocale = Locale.getDefault()
    @Before fun locale() { Locale.setDefault(Locale.SIMPLIFIED_CHINESE) }
    @After fun restoreLocale() { Locale.setDefault(originalLocale) }

    @Test fun checkboxKeepsThreeStatesAndRadioSelectionIndependent() {
        compose.mainClock.autoAdvance = false
        compose.setContent { Box(Modifier.requiredSize(1024.dp,768.dp)) { GalleryApp() } }
        compose.onAllNodesWithText("选择控件")[0].performClick()
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithText("标准").assertIsSelected()
        compose.onNodeWithText("全选").performClick()
        compose.mainClock.advanceTimeBy(440)
        compose.onNodeWithText("全选").assertIsOn()
        compose.onNodeWithText("ToggleableState.On", substring = true).assertExists()
        capture("gallery-checkbox-on")
        compose.onNodeWithText("全选").performClick()
        compose.mainClock.advanceTimeBy(32)
        compose.onNodeWithText("全选").assertIsOff()
        compose.onNodeWithText("高级").performClick()
        compose.mainClock.advanceTimeBy(200)
        compose.onNodeWithText("高级").assertIsSelected()
        compose.onNodeWithText("全选").assertIsOff()
        compose.onNodeWithText("启用控件").performClick()
        compose.mainClock.advanceTimeBy(200)
        compose.onNodeWithText("全选").assertIsNotEnabled()
        compose.onNodeWithText("enabled = false", substring = true).assertExists()
    }

    @Test fun dialogShowsCallerActionsCustomBackgroundAndMatchingCode() {
        compose.mainClock.autoAdvance = false
        compose.setContent { Box(Modifier.requiredSize(1024.dp, 768.dp)) { GalleryApp() } }
        compose.onAllNodesWithText("对话框与动态背景")[0].performClick()
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithText("自定义背景插槽").performClick()
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("background = {", substring = true).assertExists()
        compose.onNodeWithTag("open-dialog").performClick()
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithText("修改用户名").assertIsDisplayed()
        compose.onNodeWithText("公开生日信息").assertIsDisplayed()
        val first = compose.onNodeWithText("修改用户名").fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithText("公开生日信息").fetchSemanticsNode().boundsInRoot
        assertEquals(first.top, second.top)
        assertTrue(second.left > first.right)
        capture("gallery-dialog-open")
        compose.onNodeWithContentDescription("关闭对话框").performClick()
        compose.mainClock.advanceTimeBy(250)
        compose.onNodeWithTag("gallery-dialog").assertDoesNotExist()
        compose.onNodeWithText("窄窗口").performClick()
        compose.onNodeWithText("可滚动长内容").performClick()
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("modifier = Modifier.width(400.dp)", substring = true).assertExists()
        compose.onNodeWithTag("open-dialog").performClick()
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithTag("gallery-dialog").assertWidthIsEqualTo(400.dp)
        compose.onNodeWithText("社交设置").assertExists()
        val narrowFirst = compose.onNodeWithText("修改用户名").fetchSemanticsNode().boundsInRoot
        val narrowSecond = compose.onNodeWithText("公开生日信息").fetchSemanticsNode().boundsInRoot
        assertTrue(narrowSecond.top > narrowFirst.bottom)
        capture("gallery-dialog-narrow")
    }

    @Test fun pillsSelectAndUpdateParametersAndCopyableCode() {
        compose.mainClock.autoAdvance = false
        compose.setContent { Box(Modifier.requiredSize(1024.dp,768.dp)) { GalleryApp() } }
        compose.onAllNodesWithText("胶囊容器")[0].performClick()
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithTag("pill-preview").performClick()
        compose.mainClock.advanceTimeBy(32)
        compose.onNodeWithTag("pill-preview").assertIsSelected()
        compose.mainClock.advanceTimeBy(720)
        capture("gallery-pill-selected")
        compose.onNode(hasScrollAction() and hasAnyDescendant(hasText("启用胶囊")))
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy) { it(0f, 450f) }
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithText("启用胶囊").performClick()
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("pill-preview").assertIsNotEnabled()
        compose.onNodeWithText("enabled = false", substring = true).assertExists()
        compose.onNodeWithText("填满可用宽度").performClick()
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("modifier = Modifier.fillMaxWidth()", substring = true).assertExists()
        compose.onNodeWithText("长内容").performClick()
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("ZenlessText(\"内容插槽可以容纳更长的说明", substring = true).assertExists()
    }

    @Test fun tabsExposeColorOnlyStyleAndMatchingExample() {
        compose.mainClock.autoAdvance=false
        compose.setContent { Box(Modifier.requiredSize(1024.dp,768.dp)) { GalleryApp() } }
        compose.onAllNodesWithText("导航与页签")[0].performClick()
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithText("缩放呼吸").assertDoesNotExist()
        compose.onNodeWithText("文件夹样式").performClick()
        compose.mainClock.advanceTimeBy(200)
        compose.onNodeWithText("缩放呼吸").assertIsOn().performClick()
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithText("缩放呼吸").assertIsOff()
        compose.onNodeWithText("folder = false,\n        expand = false,",substring=true).assertExists()
        capture("gallery-tabs-color-only")
        compose.onNodeWithText("文件夹样式").performClick()
        compose.mainClock.advanceTimeBy(200)
        compose.onNodeWithText("缩放呼吸").assertDoesNotExist()
    }

    @Test fun changingSizeUpdatesControlsAndCopyableCode() {
        compose.mainClock.autoAdvance=false
        compose.setContent { Box(Modifier.requiredSize(1024.dp,768.dp)) { GalleryApp() } }
        compose.onAllNodesWithText("选择器与滑条")[0].performClick()
        compose.mainClock.advanceTimeBy(300)
        compose.onNodeWithContentDescription("控件尺寸").performClick()
        compose.mainClock.advanceTimeBy(300)
        compose.onNodeWithText("宽松 · 62 dp / 24 sp").performClick()
        compose.mainClock.advanceTimeBy(300)
        compose.onNodeWithContentDescription("布局").assertHeightIsEqualTo(62.dp)
        compose.onNodeWithContentDescription("等级").assertHeightIsEqualTo(62.dp)
        compose.onNodeWithText("ZenlessTheme(size = ZenlessSize.Comfortable)",substring=true).assertExists()
        capture("gallery-comfortable")
    }

    @Test fun feedCardsSelectAndUpdateOptionalSlotsAndExample() {
        compose.mainClock.autoAdvance = false
        compose.setContent { Box(Modifier.requiredSize(1024.dp, 768.dp)) { GalleryApp() } }
        compose.onAllNodesWithText("信息流卡片")[0].performClick()
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithTag("feed-card-0").performClick()
        compose.mainClock.advanceTimeBy(320)
        compose.onNodeWithTag("feed-card-0").assertIsSelected()
        compose.onNodeWithTag("feed-card-1").assertIsNotSelected()
        capture("gallery-feed-cards")
        compose.onNode(hasScrollAction() and hasAnyDescendant(hasText("启用卡片")))
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy) { it(0f, 850f) }
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithText("状态胶囊").performClick()
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("status = null", substring = true).assertExists()
        compose.onNodeWithText("分类胶囊").performClick()
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("label = null", substring = true).assertExists()
        compose.onNodeWithText("启用卡片").performClick()
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("enabled = false", substring = true).assertExists()
        compose.onNodeWithTag("feed-card-0").assertIsNotEnabled()
    }

    @Test fun galleryNavigatesAndCapturesBothLayouts() {
        compose.mainClock.autoAdvance = false
        var wide by mutableStateOf(true)
        compose.setContent { Box(Modifier.requiredSize(if (wide) 1024.dp else 390.dp, 768.dp)) { GalleryApp() } }
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithText("ZENLESS UI").assertExists()
        capture("gallery-wide")
        Demo.entries.drop(1).forEach { page ->
            compose.onAllNodesWithText(page.zh)[0].performClick()
            compose.mainClock.advanceTimeBy(400)
            capture("gallery-${page.name.lowercase()}")
        }
        compose.onAllNodesWithText("按钮")[0].performClick()
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithText("实时预览").assertExists()
        capture("gallery-buttons")
        compose.runOnIdle { wide = false }
        compose.mainClock.advanceTimeBy(400)
        capture("gallery-mobile")
        compose.onNodeWithText("English").performClick()
        compose.mainClock.advanceTimeBy(300)
        compose.onNodeWithContentDescription("Control size").assertIsDisplayed()
        capture("gallery-mobile-english")
    }

    private fun capture(name: String) {
        val bitmap = compose.onRoot().captureToImage()
        val file = File("../verification/$name.png")
        file.parentFile.mkdirs()
        Image.makeFromBitmap(bitmap.asSkiaBitmap()).use { image -> image.encodeToData()?.use { file.writeBytes(it.bytes) } }
    }
}
