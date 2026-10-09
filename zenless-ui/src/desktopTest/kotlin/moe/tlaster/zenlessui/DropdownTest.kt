package moe.tlaster.zenlessui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Image
import org.junit.Test
import java.io.File
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DropdownTest {
    @Test fun arrowMatchesTheRoundedReferenceAndDisabledInk() = runDesktopComposeUiTest(width=768,height=124) {
        mainClock.autoAdvance=false
        var enabled by mutableStateOf(true)
        setContent { CompositionLocalProvider(LocalDensity provides Density(2f)) { ZenlessTheme(size=ZenlessSize.Comfortable) {
            ZenlessSelect(listOf(""),0,{},Modifier.width(384.dp).testTag("select"),enabled=enabled)
        } } }
        mainClock.advanceTimeByFrame()
        val select=onNodeWithTag("select")
        select.assertHeightIsEqualTo(62.dp)
        val pixels=select.captureToImage().toPixelMap()
        fun PixelMap.whiteAt(y:Int)=(width-100 until width).filter { this[it,y].red>.7f }
        val rows=(0 until pixels.height).filter { pixels.whiteAt(it).isNotEmpty() }
        assertTrue(abs(rows.first()-49)<=1 && abs(rows.last()-74)<=1,"The arrow is 26 reference pixels high")
        for ((y,halfWidth) in listOf(49 to 12,52 to 15,55 to 14,61 to 10,67 to 6,73 to 2)) {
            val ink=pixels.whiteAt(y)
            assertTrue(abs(ink.first()-(706-halfWidth))<=2 && abs(ink.last()-(706+halfWidth))<=2,"Rounded contour at row $y: ${ink.first()}..${ink.last()}")
        }
        assertTrue(pixels.whiteAt(49).size<pixels.whiteAt(52).size,"The top corners are rounded")
        save(select,"enabled")
        runOnIdle { enabled=false }; mainClock.advanceTimeByFrame()
        select.onChild().assertIsNotEnabled()
        assertEquals(Color(0xff787879),select.captureToImage().toPixelMap()[706,62])
        save(select,"disabled")
    }

    @Test fun arrowScalesWithTheControlAndFollowsTheTrailingSide() = runDesktopComposeUiTest(width=480,height=100) {
        mainClock.autoAdvance=false
        var size by mutableStateOf(ZenlessSize.Default)
        var direction by mutableStateOf(LayoutDirection.Ltr)
        setContent { CompositionLocalProvider(LocalDensity provides Density(1f),LocalLayoutDirection provides direction) {
            ZenlessTheme(size) { ZenlessSelect(listOf(""),0,{},Modifier.width(480.dp).testTag("select")) }
        } }
        for (preset in ZenlessSize.entries) for (layout in LayoutDirection.entries) {
            runOnIdle { size=preset;direction=layout }; mainClock.advanceTimeByFrame()
            val pixels=onNodeWithTag("select").captureToImage().toPixelMap()
            val points=(0 until pixels.height).flatMap { y -> (0 until pixels.width).filter { x -> pixels[x,y].red>.7f }.map { x -> x to y } }
            val left=points.minOf { it.first }; val right=points.maxOf { it.first }
            val top=points.minOf { it.second }; val bottom=points.maxOf { it.second }
            val center=if(layout==LayoutDirection.Ltr)pixels.width-preset.height/2f else preset.height/2f
            assertTrue(abs((left+right+1)/2f-center)<=1,"Arrow follows the trailing semicircle center")
            assertTrue(abs((bottom-top+1)-13f*preset.height/62)<=1,"Proportional arrow height")
            assertTrue(abs((right-left+1)-16f*preset.height/62)<=1,"Proportional arrow width")
        }
    }

    private fun save(node:SemanticsNodeInteraction,name:String) {
        val file=File("../verification/dropdown/render-$name.png"); file.parentFile.mkdirs()
        Image.makeFromBitmap(node.captureToImage().asSkiaBitmap()).use { image -> image.encodeToData()?.use { file.writeBytes(it.bytes) } }
    }
}
