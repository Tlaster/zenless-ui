package moe.tlaster.zenlessui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.skia.Image
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SizingTest {
    @Test fun everyControlInheritsTheSameSizeAndCanChangeAtRuntime() {
        for (density in listOf(1f, 2f)) runDesktopComposeUiTest(width=760,height=2400) {
            mainClock.autoAdvance=false
            var size by mutableStateOf(ZenlessSize.Default)
            setContent { CompositionLocalProvider(LocalDensity provides Density(density)) {
                ZenlessTheme(size) { ZenlessOverlayHost {
                    Column(Modifier.width(360.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        ZenlessButton({},Modifier.testTag("button")) { ZenlessText("Action",Modifier.testTag("button-text")) }
                        ZenlessButton({},Modifier.testTag("leading"),leadingIcon={ Canvas(Modifier.fillMaxSize().testTag("glyph")) { drawCircle(Color.Black) } }) { ZenlessText("Action") }
                        ZenlessIconButton({},"Icon",Modifier.testTag("icon")) { Canvas(Modifier.fillMaxSize()) { drawCircle(Color.White) } }
                        ZenlessTextField("",{},Modifier.testTag("field"),placeholder="Placeholder")
                        ZenlessSelect(listOf("Selected","Other"),0,{},Modifier.testTag("select"))
                        ZenlessSwitch(true,{},Modifier.testTag("switch"),label="Switch")
                        ZenlessCheckbox(ToggleableState.On,{},Modifier.testTag("checkbox"),label="Checkbox")
                        ZenlessRadioButton(true,{},Modifier.testTag("radio"),label="Radio")
                        ZenlessSlider(.5f,{},Modifier.testTag("slider"))
                        ZenlessInfoRow("Name","Value",Modifier.testTag("info"))
                        ZenlessCollapse("Collapse",false,{},Modifier.testTag("collapse")) {}
                        ZenlessTabs(listOf("One","Two"),0,{},Modifier.testTag("tabs"))
                        ZenlessBackButton({},"Back",Modifier.testTag("back"))
                        ZenlessCloseButton({},"Close",Modifier.testTag("close"))
                        ZenlessTabs(listOf("Files","Details"),0,{},Modifier.testTag("folder"),folder=true)
                    }
                } }
            } }
            for ((value,height,font) in listOf(Triple(ZenlessSize.Compact,40,14),Triple(ZenlessSize.Default,52,20),Triple(ZenlessSize.Comfortable,62,24))) {
                runOnIdle { size=value }; mainClock.advanceTimeBy(32)
                for (tag in listOf("button","leading","icon","field","select","switch","checkbox","radio","slider","info","collapse","tabs","back","close","folder")) {
                    onNodeWithTag(tag).assertHeightIsEqualTo(height.dp)
                }
                onNodeWithTag("icon").assertWidthIsEqualTo(height.dp)
                for (tag in listOf("back","close")) onNodeWithTag(tag).assertWidthIsEqualTo((height*1.5f).dp)
                for (node in listOf(onNodeWithTag("button-text",true),onNodeWithText("Placeholder",useUnmergedTree=true),onNodeWithText("Selected",useUnmergedTree=true),onNodeWithText("Checkbox",useUnmergedTree=true),onNodeWithText("Files",useUnmergedTree=true))) {
                    val layouts=mutableListOf<TextLayoutResult>()
                    node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                    assertEquals(font.sp,layouts.single().layoutInput.style.fontSize)
                }
            }
        }
    }

    @Test fun sizeOverridesAndLargeTextKeepTheirContentInside() = runDesktopComposeUiTest(width=320,height=800) {
        mainClock.autoAdvance=false
        setContent { CompositionLocalProvider(LocalDensity provides Density(1f,1.5f)) {
            ZenlessTheme(size=ZenlessSize.Compact) {
                Column(Modifier.width(320.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    ZenlessSwitch(true,{},Modifier.testTag("switch"),label="Controls enabled",size=ZenlessSize.Comfortable)
                    ZenlessTextField("",{},Modifier.testTag("field"),placeholder="Placeholder",size=ZenlessSize.Default)
                    ZenlessCheckbox(ToggleableState.On,{},Modifier.testTag("check"),label="A longer checkbox description that wraps",size=ZenlessSize.Comfortable)
                    ZenlessButton({},Modifier.testTag("button"),size=ZenlessSize.Default) { ZenlessText("Action") }
                    ZenlessBackButton({},"Back",Modifier.testTag("back"),size=ZenlessSize.Comfortable)
                    ZenlessTabs(listOf("One","Two"),0,{},Modifier.testTag("folder"),folder=true,size=ZenlessSize.Default)
                }
            }
        } }
        mainClock.advanceTimeBy(32)
        for (text in listOf("Controls enabled","Placeholder","A longer checkbox description that wraps","Action")) {
            val results=mutableListOf<TextLayoutResult>()
            val node=onNodeWithText(text,useUnmergedTree=true)
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            val layout=results.single()
            for (line in 0 until layout.lineCount) {
                assertTrue(layout.getLineLeft(line)>=-1 && layout.getLineRight(line)<=layout.size.width+1,text)
                assertTrue(layout.getLineBottom(line)<=layout.size.height+1 && !layout.isLineEllipsized(line),text)
            }
            val bounds=node.fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.left>=0 && bounds.right<=320,text)
        }
        onNodeWithTag("back").assertWidthIsEqualTo(93.dp).assertHeightIsEqualTo(62.dp)
        onNodeWithTag("folder").assertHeightIsEqualTo(52.dp)
    }

    @Test fun selectOptionsUseTheChosenSizeAndScrollToTheSelection() {
        for (size in ZenlessSize.entries) runDesktopComposeUiTest(width=400,height=700) {
            mainClock.autoAdvance=false
            var selected by mutableIntStateOf(6)
            setContent { CompositionLocalProvider(LocalDensity provides Density(1f)) { ZenlessTheme(size) {
                ZenlessOverlayHost { Column(Modifier.padding(20.dp)) {
                    ZenlessSelect(List(10) { "Choice $it" },selected,{selected=it},Modifier.width(320.dp),label="Choice")
                } }
            } } }
            mainClock.advanceTimeByFrame()
            onNodeWithContentDescription("Choice").performClick(); mainClock.advanceTimeBy(300)
            val option=onNodeWithText("Choice 7")
            option.assertIsDisplayed().assertHeightIsEqualTo((size.height+2).dp).performClick()
            mainClock.advanceTimeBy(300)
            assertEquals(7,selected)
        }
    }

    @Test fun auxiliaryComponentsAndOverlayOverridesFollowTheSize() = runDesktopComposeUiTest(width=800,height=900) {
        mainClock.autoAdvance=false
        var size by mutableStateOf(ZenlessSize.Default)
        var alert by mutableStateOf(false)
        var drawer by mutableStateOf(false)
        setContent { CompositionLocalProvider(LocalDensity provides Density(1f)) { ZenlessTheme { ZenlessOverlayHost {
            ZenlessCard(Modifier.width(400.dp),size=size) {
                ZenlessButton({},Modifier.testTag("nested")) { ZenlessText("Nested") }
                ZenlessSpinner(Modifier.testTag("spinner"))
                ZenlessBadge("Badge",Modifier.testTag("badge"))
                ZenlessNotice("Notice",Modifier.testTag("notice"))
                ZenlessProgress(.5f,Modifier.testTag("progress"))
                ZenlessMetric("024",unit="%")
                ZenlessTooltip("Help",size=size) { ZenlessText("Hover",Modifier.testTag("anchor")) }
            }
            ZenlessAlert(alert,"Apply?","Apply",{}, {},"Cancel",size=size)
            ZenlessDrawer(drawer,"Preferences","Close",{},size=size,footer={
                ZenlessButton({},Modifier.testTag("footer")) { ZenlessText("Save") }
            }) { ZenlessButton({},Modifier.testTag("drawer-action")) { ZenlessText("Inside") } }
        } } } }
        for (value in ZenlessSize.entries) {
            runOnIdle { size=value }; mainClock.advanceTimeBy(32)
            onNodeWithTag("nested").assertHeightIsEqualTo(value.height.dp)
            for ((tag,height) in listOf("spinner" to 24,"badge" to 30,"notice" to 34,"progress" to 10)) {
                onNodeWithTag(tag).assertHeightIsEqualTo((height*value.scale).dp)
            }
            onNodeWithTag("anchor").performMouseInput { enter(center) };mainClock.advanceTimeBy(700)
            val help=mutableListOf<TextLayoutResult>()
            onNodeWithText("Help").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(help) }
            assertEquals(value.fontSize.sp,help.single().layoutInput.style.fontSize)
            onNodeWithTag("anchor").performMouseInput { exit() };mainClock.advanceTimeByFrame()
            runOnIdle { alert=true }; mainClock.advanceTimeBy(400)
            for (label in listOf("Apply","Cancel")) onNodeWithText(label).assertHeightIsEqualTo(value.height.dp)
            runOnIdle { alert=false;drawer=true }; mainClock.advanceTimeBy(400)
            for (tag in listOf("footer","drawer-action")) onNodeWithTag(tag).assertHeightIsEqualTo(value.height.dp)
            onNodeWithContentDescription("Close").assertHeightIsEqualTo(value.height.dp).assertWidthIsEqualTo((1.5f*value.height).dp)
            runOnIdle { drawer=false }; mainClock.advanceTimeBy(400)
        }
    }

    @Test fun narrowOverlaysKeepActionsReadableAtEverySize() {
        for (size in ZenlessSize.entries) runDesktopComposeUiTest(width=390,height=768) {
            mainClock.autoAdvance=false
            var alert by mutableStateOf(true)
            val labelWidths=mutableMapOf<String,Int>()
            setContent { CompositionLocalProvider(LocalDensity provides Density(1f)) { ZenlessTheme(size) { ZenlessOverlayHost {
                val measurer=rememberTextMeasurer()
                SideEffect { for(label in listOf("Apply","Cancel")) labelWidths[label]=measurer.measure(label,TextStyle(fontSize=size.fontSize.sp,letterSpacing=1.sp)).size.width }
                ZenlessAlert(alert,"Apply these changes?","Apply",{}, {},"Cancel")
                ZenlessDrawer(!alert,"Preferences","Close",{},footer={ZenlessButton({}) { ZenlessText("Save") }}) {
                    ZenlessButton({}) { ZenlessText("Action") }
                    ZenlessTabs(listOf("Files","Details"),0,{},folder=true)
                }
            } } } }
            mainClock.advanceTimeBy(400)
            fun capture(name:String) {
                val file=File("../verification/$name-${size.name.lowercase()}.png");file.parentFile.mkdirs()
                Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image -> image.encodeToData()?.use { file.writeBytes(it.bytes) } }
            }
            capture("sizing-alert-mobile")
            for (text in listOf("Apply","Cancel")) {
                val node=onNodeWithText(text,useUnmergedTree=true)
                val bounds=node.fetchSemanticsNode().boundsInRoot
                assertTrue(labelWidths.getValue(text)<=bounds.width+1,"$size: $text must fit its measured text area")
            }
            runOnIdle { alert=false };mainClock.advanceTimeBy(400)
            onNodeWithContentDescription("Close").assertIsDisplayed()
            onNodeWithText("Save").assertIsDisplayed()
            capture("sizing-drawer-mobile")
        }
    }

    @Test fun captureTheThreeControlGroups() = runDesktopComposeUiTest(width=1140,height=1200) {
        mainClock.autoAdvance=false
        setContent { CompositionLocalProvider(LocalDensity provides Density(1f)) { ZenlessTheme { ZenlessOverlayHost { ZenlessSurface {
            Row(Modifier.fillMaxSize().padding(20.dp),horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                for (size in ZenlessSize.entries) ZenlessTheme(size) {
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                        ZenlessText("${size.name} · ${size.height} / ${size.fontSize}")
                        ZenlessButton({},Modifier.fillMaxWidth()) { ZenlessText("Continue / 继续") }
                        ZenlessButton({},Modifier.fillMaxWidth(),leadingIcon={Canvas(Modifier.fillMaxSize().padding(5.dp)) { drawCircle(Color.Black) }}) { ZenlessText("Continue / 继续") }
                        ZenlessTextField("",{},placeholder="Name / 名称")
                        ZenlessSelect(listOf("Balanced / 均衡"),0,{})
                        ZenlessSwitch(true,{},label="Notify / 通知")
                        ZenlessCheckbox(ToggleableState.Indeterminate,{},label="Select all / 全选")
                        ZenlessRadioButton(true,{},label="Default / 默认")
                        ZenlessSlider(.5f,{})
                        ZenlessInfoRow("Status / 状态","Ready")
                        ZenlessCollapse("Details / 详情",false,{}) {}
                        ZenlessTabs(listOf("One","Two"),0,{})
                        Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                            ZenlessBackButton({},"Back"); ZenlessCloseButton({},"Close"); ZenlessSpinner()
                        }
                        ZenlessTabs(listOf("Files / 文件","Details / 详情"),0,{},folder=true)
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { ZenlessBadge("Ready / 就绪"); ZenlessNotice("Saved / 已保存") }
                        ZenlessProgress(.65f)
                    }
                }
            }
        } } } } }
        mainClock.advanceTimeBy(300)
        val file=File("../verification/control-sizes.png"); file.parentFile.mkdirs()
        Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image -> image.encodeToData()?.use { file.writeBytes(it.bytes) } }
    }
}
