package moe.tlaster.zenlessui

import androidx.compose.foundation.background
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
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TabsTest {
    @Test fun colorOnlyTabsKeepCircularOuterEndsAndSkewedInnerEdges() {
        for(density in listOf(1,2)) runDesktopComposeUiTest(width=680,height=512) {
            mainClock.autoAdvance=false
            setContent { CompositionLocalProvider(LocalDensity provides Density(density.toFloat())) { ZenlessTheme {
                Column(Modifier.background(Color.Black)) {
                    for(index in 0..3) Box(Modifier.size(340.dp,64.dp).testTag("tabs-$index")) {
                        ZenlessTabs(List(if(index==3)1 else 3) { "" },if(index==3)0 else index,{},
                            Modifier.offset(20.dp,12.dp).width(300.dp),expand=false)
                    }
                }
            } } }
            mainClock.advanceTimeBy(32)
            val first=(0..3).map { onNodeWithTag("tabs-$it").captureToImage().toPixelMap() }
            for(index in listOf(0,2,3)) {
                val pixels=first[index]
                for(y in listOf(13,22,32,41,50).map { it*density }) {
                    val radius=21f*density
                    val dy=y+.5f-32*density
                    val dx=sqrt(radius*radius-dy*dy)
                    val ink=pixels.inkAt(y)
                    if(index!=2) assertTrue(abs(ink.first()+.5f-(40*density-dx))<1.5f,"Circular left cap at density $density, row $y")
                    if(index!=0) assertTrue(abs(ink.last()+.5f-(300*density+dx))<1.5f,"Circular right cap at density $density, row $y")
                }
            }
            for(index in 0..2) {
                val upper=first[index].inkAt(22*density);val lower=first[index].inkAt(41*density)
                if(index>0) assertTrue(upper.first()-lower.first()>4*density,"Interior left edge remains slanted")
                if(index<2) assertTrue(upper.last()-lower.last()>4*density,"Interior right edge remains slanted")
            }
            mainClock.advanceTimeBy(352)
            for(index in 0..3) {
                val next=onNodeWithTag("tabs-$index").captureToImage().toPixelMap()
                val x=(if(index==3)170 else 70+100*index)*density
                assertTrue(first[index][x,32*density]!=next[x,32*density],"Color continues breathing")
                for(y in 13*density..50*density) {
                    val before=first[index].inkAt(y);val after=next.inkAt(y)
                    assertTrue(abs(before.first()-after.first())<=1 && abs(before.last()-after.last())<=1,"Color-only geometry stays fixed")
                }
            }
            val file=File("../verification/tabs-color-only-density-$density.png");file.parentFile.mkdirs()
            Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image -> image.encodeToData()?.use { file.writeBytes(it.bytes) } }
        }
    }

    @Test fun defaultPulseExpandsAndColorOnlySelectionStillWorks() = runDesktopComposeUiTest(width=400,height=160) {
        mainClock.autoAdvance=false
        var expand by mutableStateOf(true)
        var selected by mutableIntStateOf(0)
        var clicks=0
        setContent { CompositionLocalProvider(LocalDensity provides Density(1f)) { ZenlessTheme {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                if(expand) ZenlessTabs(listOf("Left","Middle","Right"),selected,{selected=it;clicks++},Modifier.offset(30.dp,50.dp).width(330.dp))
                else ZenlessTabs(listOf("Left","Middle","Right"),selected,{selected=it;clicks++},Modifier.offset(30.dp,50.dp).width(330.dp),expand=false)
            }
        } } }
        mainClock.advanceTimeBy(32)
        val resting=onRoot().captureToImage().toPixelMap().inkAt(70).first()
        mainClock.advanceTimeBy(144)
        val peak=onRoot().captureToImage().toPixelMap().inkAt(70).first()
        assertTrue(resting-peak>=4,"The default still expands beyond the plate")
        runOnIdle { expand=false }
        mainClock.advanceTimeBy(32)
        onNodeWithText("Right").performTouchInput { click() }
        mainClock.advanceTimeBy(240)
        onNodeWithText("Right").assertIsSelected()
        onNodeWithText("Left").assertIsNotSelected()
        runOnIdle { assertEquals(2,selected);assertEquals(1,clicks) }
        onNodeWithText("Right").performTouchInput { click() }
        mainClock.advanceTimeBy(240)
        runOnIdle { assertEquals(1,clicks) }
    }

    @Test fun selectionSlidesMorphsAndRetargetsWithoutRestartingBreathing() {
        for(expand in listOf(false,true)) runDesktopComposeUiTest(width=400,height=160) {
            mainClock.autoAdvance=false
            var selected by mutableIntStateOf(0)
            setContent { CompositionLocalProvider(LocalDensity provides Density(1f)) { ZenlessTheme {
                Column(Modifier.background(Color.Black)) {
                    for(row in 0..1) Box(Modifier.size(400.dp,80.dp).testTag("motion-$row")) {
                        ZenlessTabs(listOf("","",""),if(row==0)selected else 0,{},Modifier.offset(30.dp,20.dp).width(330.dp),expand=expand)
                    }
                }
            } } }
            fun frame():PixelMap {
                val moving=onNodeWithTag("motion-0").captureToImage().toPixelMap()
                val control=onNodeWithTag("motion-1").captureToImage().toPixelMap()
                val ink=moving.inkAt(40)
                assertTrue(ink.zipWithNext().all { (a,b) -> b==a+1 },"Only one continuous indicator is drawn")
                assertEquals(control[85,40],moving[(ink.first()+ink.last())/2,40],"Selection keeps the existing color phase")
                val movingRows=(0 until moving.height).filter { moving.inkAt(it).isNotEmpty() }
                val controlRows=(0 until control.height).filter { control.inkAt(it).isNotEmpty() }
                assertEquals(controlRows,movingRows,"Selection keeps the existing scale phase")
                return moving
            }
            fun PixelMap.centerX():Float { val ink=inkAt(40);return (ink.first()+ink.last()+1)/2f }
            mainClock.advanceTimeBy(320)
            val initial=frame().centerX()
            runOnIdle { selected=1 }
            mainClock.advanceTimeByFrame()
            assertTrue(abs(initial-frame().centerX())<1f,"Selection does not jump to its destination")
            mainClock.advanceTimeBy(48)
            val middle=frame()
            assertTrue(middle.centerX() in initial+10..initial+105,"The indicator travels through intermediate positions")
            val slant=middle.inkAt(29).first()-middle.inkAt(50).first()
            assertTrue(slant in 1..6,"The circular cap morphs gradually into a slanted edge")
            mainClock.advanceTimeBy(300)
            assertTrue(abs(frame().centerX()-195)<1f,"The indicator settles on the middle tab: ${frame().centerX()}, expand=$expand")
            runOnIdle { selected=2 }
            mainClock.advanceTimeBy(64)
            val turning=frame().centerX()
            runOnIdle { selected=0 }
            mainClock.advanceTimeByFrame()
            assertTrue(abs(frame().centerX()-turning)<20f,"Rapid reversal starts from the current position")
            mainClock.advanceTimeBy(64)
            assertTrue(frame().centerX()<turning,"The moving indicator turns toward the latest selection")
            mainClock.advanceTimeBy(300)
            assertTrue(abs(frame().centerX()-initial)<1f,"The latest selection wins")
        }
    }

    @Test fun movingIndicatorClipsTextInkAndCapturesBothStyles() = runDesktopComposeUiTest(width=800,height=320) {
        mainClock.autoAdvance=false
        var selected by mutableIntStateOf(0)
        setContent { CompositionLocalProvider(LocalDensity provides Density(2f)) { ZenlessTheme {
            Column(Modifier.background(Color.Black)) {
                for(expand in listOf(true,false)) Box(Modifier.size(400.dp,80.dp)) {
                    ZenlessTabs(if(expand)listOf("Overview","Details","History") else listOf("我的好友","添加好友","寻找好友"),selected,{},
                        Modifier.offset(30.dp,20.dp).width(330.dp),expand=expand)
                }
            }
        } } }
        val whiteCounts=mutableListOf<Int>()
        for(frame in 0 until 72) {
            if(frame in listOf(18,36,54)) runOnIdle { selected=when(frame) {18->1;36->2;else->0} }
            mainClock.advanceTimeBy(32)
            val text=onNodeWithText("Overview",useUnmergedTree=true).captureToImage().toPixelMap()
            whiteCounts.add((0 until text.height).sumOf { y -> (0 until text.width).count { x -> text[x,y].let { it.red>.9f && it.green>.9f && it.blue>.9f } } })
            val file=File("../verification/tabs-motion/frame-${frame.toString().padStart(3,'0')}.png");file.parentFile.mkdirs()
            Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image -> image.encodeToData()?.use { file.writeBytes(it.bytes) } }
        }
        assertEquals(0,whiteCounts.first(),"Selected text is black")
        assertTrue(whiteCounts.max()>20,"Uncovered text is white")
        assertTrue(whiteCounts.any { it in 1 until whiteCounts.max() },"The moving edge recolors partial glyphs")
    }

    @Test fun rightToLeftAndItemCountChangesKeepTheIndicatorInsideTheRail() = runDesktopComposeUiTest(width=390,height=80) {
        mainClock.autoAdvance=false
        var selected by mutableIntStateOf(0)
        var count by mutableIntStateOf(3)
        setContent { CompositionLocalProvider(LocalDensity provides Density(1f),LocalLayoutDirection provides LayoutDirection.Rtl) { ZenlessTheme {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                ZenlessTabs(List(count) { "" },selected,{},Modifier.fillMaxWidth().padding(horizontal=30.dp,vertical=20.dp),expand=false)
            }
        } } }
        mainClock.advanceTimeBy(32)
        assertTrue(onRoot().captureToImage().toPixelMap().inkAt(40).first()>240,"The first RTL tab is on the right")
        runOnIdle { selected=2 }
        mainClock.advanceTimeBy(350)
        assertTrue(onRoot().captureToImage().toPixelMap().inkAt(40).last()<145,"Selection reaches the left end")
        runOnIdle { selected=0;count=1 }
        mainClock.advanceTimeBy(32)
        val ink=onRoot().captureToImage().toPixelMap().inkAt(40)
        assertTrue(abs(ink.first()-29)<=1 && abs(ink.last()-360)<=1,"A changed item count snaps to the new rail geometry")
    }

    private fun PixelMap.inkAt(y:Int)=(0 until width).filter { x -> this[x,y].let { it.red>.4f && it.green>.65f && it.blue<.1f } }
}
