package moe.tlaster.zenlessui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Image
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class VisualTest {
    @Test fun overlayGeometryAndDrawerScrollbar() = runDesktopComposeUiTest(width=1024,height=768) {
        mainClock.autoAdvance=false
        var alert by mutableStateOf(true)
        setContent { ZenlessTheme { ZenlessOverlayHost {
            ZenlessText("Background")
            ZenlessAlert(alert,"Apply these changes?","Apply",{}, {},"Cancel")
            ZenlessDrawer(!alert,"Preferences","Close",{},footer={ZenlessButton({}) {ZenlessText("Save")} }) {
                repeat(30) { ZenlessText("Item $it",Modifier.height(40.dp)) }
            }
        } } }
        mainClock.advanceTimeBy(400)
        save("alert-calibrated")
        val action=onNodeWithText("Apply").fetchSemanticsNode().boundsInRoot
        assertTrue(kotlin.math.abs(action.center.y-477f)<2f,"Actions straddle the band edge at center + 93")
        runOnIdle { alert=false }
        mainClock.advanceTimeBy(400)
        save("drawer-calibrated")
        val scrollbar=onNodeWithContentDescription("Scroll")
        scrollbar.performTouchInput { swipe(center,center.copy(y=center.y+100f),300) }
        mainClock.advanceTimeBy(400)
        onNodeWithText("Item 0").assertIsNotDisplayed()
        save("drawer-scrolled")
    }

    @Test fun componentGeometryAtUnitDensity() = runDesktopComposeUiTest(width=1920,height=1080) {
        mainClock.autoAdvance=false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                ZenlessTheme { ZenlessOverlayHost(Modifier.requiredSize(1920.dp,1080.dp)) { ZenlessSurface(Modifier.fillMaxSize()) {
                    val tones=listOf(ZenlessTone.Neutral,ZenlessTone.Primary,ZenlessTone.Success,ZenlessTone.Info,ZenlessTone.Warning,ZenlessTone.Danger)
                    for(row in 0..3) tones.forEachIndexed { col,tone ->
                        ZenlessButton({},Modifier.offset((64+140*col).dp,(64+60*row).dp).size(120.dp,40.dp).testTag("button-$row-$col"),tone=tone,
                            variant=when(row){1->ZenlessButtonVariant.Plain;2->ZenlessButtonVariant.Hollow;else->ZenlessButtonVariant.Filled},enabled=row!=3) {}
                    }
                    ZenlessButton({},Modifier.offset(64.dp,330.dp).size(160.dp,40.dp),round=false) {}
                    ZenlessBackButton({},"Back",Modifier.offset(260.dp,330.dp))
                    ZenlessCloseButton({},"Close",Modifier.offset(390.dp,330.dp))
                    ZenlessCard(Modifier.offset(64.dp,430.dp).size(240.dp,130.dp)) {}
                    ZenlessTextField("",{},Modifier.offset(350.dp,430.dp).width(360.dp))
                    ZenlessSelect(listOf("",""),0,{},Modifier.offset(350.dp,490.dp).width(360.dp))
                    ZenlessProgress(.65f,Modifier.offset(64.dp,610.dp).width(600.dp))
                    tones.forEachIndexed { index,tone -> ZenlessBadge("",Modifier.offset((64+index*100).dp,670.dp).size(80.dp,30.dp),tone) }
                    ZenlessTabs(listOf("","",""),0,{},Modifier.offset(64.dp,740.dp).width(450.dp))
                    ZenlessTabs(listOf("","",""),0,{},Modifier.offset(64.dp,830.dp).width(600.dp),folder=true)
                } } }
            }
        }
        mainClock.advanceTimeBy(16)
        onNodeWithTag("button-0-0").assertHeightIsEqualTo(40.dp).assertWidthIsEqualTo(120.dp)
        val pixels=onNodeWithTag("button-0-1").captureToImage().toPixelMap()
        assertTrue(pixels[60,2].blue > .9f && pixels[60,2].red < .1f,"Primary edge must retain its semantic blue")
        assertTrue(pixels[60,4].red < .02f && pixels[60,4].blue < .02f,"Enabled plate has a black separator at four units")
        save("components-calibrated")
        val resting=headerPath(Rect(0f,0f,90f,60f),true).getBounds()
        val expanded=headerPath(Rect(0f,0f,90f,60f),true,9f).getBounds()
        assertTrue(expanded.left<resting.left-8 && expanded.right>resting.right+8)
        assertTrue(expanded.top<resting.top-8 && expanded.bottom>resting.bottom+8)
        onNodeWithContentDescription("Back").performTouchInput { down(center) }
        mainClock.advanceTimeBy(160)
        save("back-pressed")
        onNodeWithContentDescription("Back").performTouchInput { up() }
    }

    private fun DesktopComposeUiTest.save(name:String) {
        val file=File("../verification/$name.png");file.parentFile.mkdirs()
        Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image -> image.encodeToData()?.use { file.writeBytes(it.bytes) } }
    }
}
