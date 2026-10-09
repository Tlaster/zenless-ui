package moe.tlaster.zenlessui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.TextLayoutResult
import org.jetbrains.skia.Image
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class VisualTest {
    @Test fun leadingButtonsShareTypographyAndCenterInTheRemainingBody() = runDesktopComposeUiTest(width=640,height=160) {
        mainClock.autoAdvance=false
        var leading by mutableStateOf(false)
        var fixedWidth by mutableStateOf(true)
        var size by mutableStateOf(ZenlessSize.Default)
        var direction by mutableStateOf(LayoutDirection.Ltr)
        setContent { CompositionLocalProvider(LocalDensity provides Density(2f),LocalLayoutDirection provides direction) { ZenlessTheme {
            Box(Modifier.fillMaxSize()) {
                ZenlessButton({},Modifier.height(58.dp).then(if(fixedWidth)Modifier.width(244.dp) else Modifier).testTag("button"),size=size,
                    leadingIcon=if(leading)({Box(Modifier.size(12.dp))}) else null) {
                    ZenlessText("Action",Modifier.testTag("label"))
                }
            }
        } } }
        fun textLayout():TextLayoutResult {
            val results=mutableListOf<TextLayoutResult>()
            onNodeWithTag("label",useUnmergedTree=true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            return results.single()
        }
        for(value in ZenlessSize.entries)for(layoutDirection in LayoutDirection.entries)for(fixed in listOf(true,false)) {
            runOnIdle { size=value;direction=layoutDirection;fixedWidth=fixed;leading=false };mainClock.advanceTimeByFrame()
            val plain=textLayout().layoutInput.style
            runOnIdle { leading=true };mainClock.advanceTimeByFrame()
            assertEquals(plain,textLayout().layoutInput.style,"A leading icon does not change typography at $value")
            val button=onNodeWithTag("button").fetchSemanticsNode().boundsInRoot
            val label=onNodeWithTag("label",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
            val diameter=button.height*58.5f/58f
            val leadingCenter=if(direction==LayoutDirection.Ltr)button.left+diameter/2 else button.right-diameter/2
            val trailingCenter=if(direction==LayoutDirection.Ltr)button.right-button.height/2 else button.left+button.height/2
            assertTrue(kotlin.math.abs(label.center.x-(leadingCenter+trailingCenter)/2)<=1,"Center between the leading circle and trailing semicircle centers")
            assertTrue(if(direction==LayoutDirection.Ltr)label.left>=button.left+diameter-1 else label.right<=button.right-diameter+1,"Text stays outside the icon circle even at natural width")
        }
    }

    @Test fun closeButtonAtReferenceDensity() = runDesktopComposeUiTest(width=188,height=128) {
        mainClock.autoAdvance=false
        var clicks=0
        var enabled by mutableStateOf(true)
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                Box(Modifier.requiredSize(94.dp,64.dp).background(Color.Black)) {
                    ZenlessCloseButton({clicks++},"Close",Modifier.offset(2.dp,2.dp),enabled)
                }
            }
        }
        mainClock.advanceTimeBy(16)
        val close=onNodeWithContentDescription("Close")
        close.assertWidthIsEqualTo(90.dp).assertHeightIsEqualTo(60.dp)
        val pixels=onRoot().captureToImage().toPixelMap()
        assertEquals(Color(0xffc50600),pixels[80,12],"Reference rim red")
        assertEquals(Color(0xffc50600),pixels[40,62],"Dark red checker cell")
        assertEquals(Color(0xffcb1100),pixels[130,45],"Light red checker cell")
        assertTrue(pixels[80,6].red>.95f && pixels[80,21].red>.9f,"Outer and inner upper bevels")
        assertTrue(pixels[80,17].red<.03f,"Black separator between rim and face")
        assertEquals(Color.Black,pixels[84,63],"The cross is black")
        // Native reference scanlines, allowing one pixel for renderer antialiasing.
        for((y,left,right) in listOf(Triple(10,37,172),Triple(20,23,179),Triple(35,11,181),Triple(65,5,169),Triple(95,15,152),Triple(110,30,137),Triple(118,47,120))) {
            val ink=(0 until pixels.width).filter { pixels[it,y].red>100/255f }
            assertTrue(kotlin.math.abs(ink.first()-left)<=1 && kotlin.math.abs(ink.last()-right)<=1,"Reference close contour at row $y")
        }
        for((y,left,right) in listOf(Triple(41,61,106),Triple(55,67,100),Triple(63,74,93),Triple(80,60,107),Triple(85,62,105))) {
            val ink=(55..113).filter { pixels[it,y].red<100/255f }
            assertTrue(kotlin.math.abs(ink.first()-left)<=1 && kotlin.math.abs(ink.last()-right)<=1,"Reference cross at row $y")
        }
        save("close-reference-density")
        close.performTouchInput { down(center) }
        mainClock.advanceTimeBy(800)
        val held=onRoot().captureToImage().toPixelMap()
        assertTrue(held[130,45].green>.5f,"Holding keeps the navigation highlight")
        assertEquals(Color.Black,held[84,63],"The cross stays black while held")
        save("close-held-reference-density")
        close.performTouchInput { up() }
        runOnIdle { assertEquals(0,clicks,"Activation waits for the release flash") }
        mainClock.advanceTimeBy(240)
        runOnIdle { assertEquals(1,clicks) }
        assertEquals(pixels[130,45],onRoot().captureToImage().toPixelMap()[130,45],"Release restores the checker face")
        close.performTouchInput { down(center) }
        mainClock.advanceTimeBy(800)
        close.performTouchInput { moveTo(androidx.compose.ui.geometry.Offset(-100f,-100f));up() }
        mainClock.advanceTimeBy(240)
        runOnIdle { assertEquals(1,clicks);enabled=false }
        mainClock.advanceTimeByFrame()
        close.assertIsNotEnabled().performTouchInput { click() }
        mainClock.advanceTimeBy(240)
        runOnIdle { assertEquals(1,clicks) }
        assertEquals(pixels[130,45],onRoot().captureToImage().toPixelMap()[130,45],"Cancelled and disabled presses restore the resting face")
    }

    @Test fun enabledAndDisabledLeadingButtonsAtReferenceDensity() = runDesktopComposeUiTest(width=1080,height=160) {
        mainClock.autoAdvance=false
        setContent { CompositionLocalProvider(LocalDensity provides Density(2f)) { ZenlessTheme {
            Row(Modifier.fillMaxSize().background(Color.Black).padding(10.dp),horizontalArrangement=Arrangement.spacedBy(26.dp)) {
                for(enabled in listOf(false,true)) ZenlessButton(
                    {check(enabled)},Modifier.size(244.dp,58.dp).testTag(if(enabled)"enabled-leading" else "disabled-leading"),
                    tone=if(enabled)ZenlessTone.Danger else ZenlessTone.Warning,size=ZenlessSize.Extra,enabled=enabled,
                    leadingIcon={val ink=zenlessContentColor;Canvas(Modifier.size(16.dp)) { drawRect(ink) }},
                ) { ZenlessText(if(enabled)"继续操作" else "暂不可用") }
            }
        } } }
        mainClock.advanceTimeBy(16)
        save("button-states-calibrated")
        val disabled=onNodeWithTag("disabled-leading");val enabled=onNodeWithTag("enabled-leading")
        val label=onNodeWithText("继续操作",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
        val button=enabled.fetchSemanticsNode().boundsInRoot
        val leadingCenter=button.left+button.height*58.5f/58f/2
        val trailingCenter=button.right-button.height/2
        assertTrue(kotlin.math.abs(label.center.x-(leadingCenter+trailingCenter)/2)<=1,"The label centers between the two circular centers")
        val off=disabled.captureToImage().toPixelMap();val on=enabled.captureToImage().toPixelMap()
        assertEquals(Color(0xff262626),on[300,7]);assertEquals(on[300,7],off[300,7],"Disabling preserves the shell")
        assertEquals(Color(0xff3d3d3d),on[300,2],"The upper bevel keeps the reference brightness")
        assertEquals(Color(0xff262626),on[90,15],"The interior arc has no second bevel")
        for(y in (5..9)+(107..112))for(x in 70..104) {
            assertEquals(Color(0xff262626),on[x,y],"The circular and outer borders form one solid join at $x,$y")
            assertEquals(on[x,y],off[x,y],"Disabled junctions use the same silhouette")
        }
        for((x,value) in listOf(114 to 38,115 to 33,116 to 13))assertTrue(kotlin.math.abs(on[x,58].red*255-value)<=1,"Soft inner-ring edge at $x")
        for((y,left,right) in listOf(Triple(11,24,462),Triple(21,13,472),Triple(41,3,483),Triple(58,1,486),Triple(91,11,475),Triple(111,35,452))) {
            val edge=(0 until on.width).filter { on[it,y].red>15/255f }
            assertTrue(kotlin.math.abs(edge.first()-left)<=1 && kotlin.math.abs(edge.last()-right)<=1,"Reference shell contour at row $y")
        }
        assertEquals(Color(0xffff2b00),on[58,32],"Enabled badge red")
        assertEquals(Color(0xff5a3e00),off[58,32],"Disabled badge keeps its dim amber")
        for(y in 14..24)for(x in 270..360)assertEquals(on[x,y],off[x,y],"The same texture remains visible")
        disabled.performTouchInput { down(center) };mainClock.advanceTimeBy(800)
        assertEquals(off[58,32],disabled.captureToImage().toPixelMap()[58,32],"A disabled badge never pulses")
        disabled.performTouchInput { up() }
    }

    @Test fun backButtonAtReferenceDensity() = runDesktopComposeUiTest(width=188,height=128) {
        mainClock.autoAdvance=false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                Box(Modifier.requiredSize(94.dp,64.dp).background(Color.Black)) {
                    ZenlessBackButton({},"Back",Modifier.offset(2.dp,2.dp))
                }
            }
        }
        mainClock.advanceTimeBy(16)
        val pixels=onRoot().captureToImage().toPixelMap()
        val red=Color(0xffc50600)
        for((x,y) in listOf(100 to 45,100 to 85,80 to 12)) assertEquals(red,pixels[x,y],"Reference red at $x,$y")
        assertTrue(pixels[50,8].red>.97f && pixels[50,8].green in .08f.. .14f,"Upper rim has a bright bevel")
        assertEquals(Color(0xff090909),pixels[30,21],"Light checker cell")
        assertEquals(Color.Black,pixels[35,21],"Dark checker cell")
        // Native reference scanlines, allowing one pixel for renderer antialiasing.
        for((y,left,right) in listOf(Triple(12,13,151),Triple(30,5,171),Triple(65,18,182),Triple(100,38,170),Triple(120,72,136))) {
            val ink=(0 until pixels.width).filter { pixels[it,y].red>100/255f && pixels[it,y].green<50/255f }
            assertTrue(kotlin.math.abs(ink.first()-left)<=1 && kotlin.math.abs(ink.last()-right)<=1,"Reference contour at row $y")
        }
        save("back-reference-density")
        val back=onNodeWithContentDescription("Back")
        back.performTouchInput { down(center) }
        mainClock.advanceTimeBy(800)
        assertEquals(Color.Black,onRoot().captureToImage().toPixelMap()[100,45],"Held arrow matches the black reference ink")
        save("back-held-reference-density")
        back.performTouchInput { up() }
        mainClock.advanceTimeBy(240)
        assertEquals(red,onRoot().captureToImage().toPixelMap()[100,45],"Resting red returns after the release flash")
        back.performTouchInput { down(center) }
        mainClock.advanceTimeBy(800)
        back.performTouchInput { moveTo(androidx.compose.ui.geometry.Offset(-100f,-100f)) }
        mainClock.advanceTimeBy(64)
        assertEquals(red,onRoot().captureToImage().toPixelMap()[100,45],"Cancelled presses restore resting ink")
        back.performTouchInput { up() }
    }

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
                    ZenlessSelect(listOf("",""),0,{},Modifier.offset(350.dp,490.dp).width(360.dp).testTag("select"))
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
        assertTrue(kotlin.math.abs(pixels[60,2].blue-pixels[60,2].red)<.01f,"Every tone shares the gray shell")
        val neutral=onNodeWithTag("button-0-0").captureToImage().toPixelMap()
        for(row in 0..3)for(column in 0..5) {
            val button=onNodeWithTag("button-$row-$column").captureToImage().toPixelMap()
            for((x,y) in listOf(60 to 1,1 to 20,60 to 39))assertEquals(neutral[x,y],button[x,y],"Tone, variant and disabled state preserve the common rim")
        }
        val select=onNodeWithTag("select").captureToImage().toPixelMap()
        assertTrue(neutral[60,20].red<=9/255f,"Neutral button uses the reference's subtle checker")
        assertEquals(select[180,31],select[186,31],"The texture repeats every six units")
        save("components-calibrated")
        // Compare sampled contours; cubic getBounds also includes control points outside the curve.
        for(back in listOf(true,false)) {
            val resting=headerPath(Rect(0f,0f,90f,60f),back,.001f).getBounds()
            val expanded=headerPath(Rect(0f,0f,90f,60f),back,9f).getBounds()
            assertTrue(expanded.left<resting.left-8 && expanded.right>resting.right+8)
            assertTrue(expanded.top<resting.top-8 && expanded.bottom>resting.bottom+8)
        }
        onNodeWithContentDescription("Back").performTouchInput { down(center) }
        mainClock.advanceTimeBy(160)
        save("back-pressed")
        mainClock.advanceTimeBy(640)
        save("back-held")
        onNodeWithContentDescription("Back").performTouchInput { up() }
    }

    private fun DesktopComposeUiTest.save(name:String) {
        val file=File("../verification/$name.png");file.parentFile.mkdirs()
        Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image -> image.encodeToData()?.use { file.writeBytes(it.bytes) } }
    }
}
