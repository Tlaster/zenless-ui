package moe.tlaster.zenlessui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import org.jetbrains.skia.Image
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PressedInkTest {
    @Test fun cardsAndInputsHaveNoButtonFlash() = runDesktopComposeUiTest(width=600,height=160) {
        mainClock.autoAdvance=false
        setContent { CompositionLocalProvider(LocalDensity provides Density(1f)) { ZenlessTheme {
            Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                ZenlessCard(Modifier.size(90.dp,60.dp).testTag("card"),onClick={}) {}
                ZenlessCheckbox(ToggleableState.Off,{},Modifier.testTag("checkbox"))
                ZenlessRadioButton(false,{},Modifier.testTag("radio"))
                ZenlessTextField("Text",{},Modifier.width(180.dp).testTag("field"),readOnly=true)
            }
        } } }
        fun surface(node:SemanticsNodeInteraction):List<Color> {
            val pixels=node.captureToImage().toPixelMap()
            return listOf(1,minOf(20,pixels.width/2),pixels.width/2).map { pixels[it,pixels.height/2] }
        }
        mainClock.advanceTimeBy(200)
        for(tag in listOf("card","checkbox","radio","field")) {
            val control=onNodeWithTag(tag)
            if(tag=="field") { control.performTouchInput { click() };mainClock.advanceTimeBy(240) }
            val resting=surface(control)
            control.performTouchInput { down(center) };mainClock.advanceTimeBy(200)
            assertEquals(resting,surface(control),"$tag must not gain a button highlight while held")
            control.performTouchInput { up() }
            repeat(12) {
                mainClock.advanceTimeByFrame()
                assertEquals(resting,surface(control),"$tag must not flash after release")
            }
        }
    }

    @Test fun allButtonTonesAndVariantsUseBlackTextAndCallerIconsWhileHeld() = runDesktopComposeUiTest(width=1120,height=400) {
        mainClock.autoAdvance=false
        var leadingVariant by mutableStateOf(ZenlessButtonVariant.Filled)
        var leadingRound by mutableStateOf(true)
        var direction by mutableStateOf(LayoutDirection.Ltr)
        setContent { CompositionLocalProvider(LocalDensity provides Density(1f),LocalLayoutDirection provides direction) { ZenlessTheme {
            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                ZenlessButtonVariant.entries.forEach { variant -> Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    ZenlessTone.entries.forEach { tone ->
                        val key="$variant-$tone"
                        ZenlessButton({},Modifier.size(140.dp,70.dp).testTag(key),tone=tone,variant=variant) {
                            Column {
                                ZenlessText("Aa",Modifier.testTag("$key-text"))
                                ZenlessText("Ab",Modifier.testTag("$key-caption"),style=ZenlessTextStyle.Caption)
                            }
                            val ink=zenlessContentColor
                            Canvas(Modifier.size(8.dp).testTag("$key-icon")) { drawRect(ink) }
                        }
                    }
                } }
                Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    ZenlessIconButton({},"Caller icon",Modifier.testTag("icon-button")) {
                        val ink=zenlessContentColor
                        Canvas(Modifier.size(20.dp)) { drawRect(ink) }
                    }
                    ZenlessButton({error("Disabled button activated")},Modifier.testTag("disabled"),enabled=false) { ZenlessText("Disabled",style=ZenlessTextStyle.Caption) }
                    ZenlessButton({},Modifier.width(260.dp).testTag("leading-button"),size=ZenlessSize.Default,variant=leadingVariant,round=leadingRound,leadingIcon={
                        val ink=zenlessContentColor
                        Canvas(Modifier.size(12.dp).testTag("leading-glyph")) { drawRect(ink) }
                    }) { ZenlessText("Leading") }
                }
            }
        } } }
        mainClock.advanceTimeBy(16)
        for(variant in ZenlessButtonVariant.entries) for(tone in ZenlessTone.entries) {
            val key="$variant-$tone"
            val button=onNodeWithTag(key)
            val text=onNodeWithTag("$key-text",useUnmergedTree=true)
            val caption=onNodeWithTag("$key-caption",useUnmergedTree=true)
            val icon=onNodeWithTag("$key-icon",useUnmergedTree=true)
            val restingInk=text.ink();val restingCaption=caption.ink();val restingIcon=icon.centerPixel()
            assertEquals(Color.White,restingInk,"$key shares the enabled text color")
            button.performTouchInput { down(center) };mainClock.advanceTimeBy(800)
            assertEquals(Color.Black,text.ink(),"$key text")
            assertEquals(Color.Black,caption.ink(),"$key caption")
            assertEquals(Color.Black,icon.centerPixel(),"$key caller icon")
            button.save("held-$key")
            button.performTouchInput { up() };mainClock.advanceTimeBy(240)
            assertEquals(restingInk,text.ink(),"$key released text")
            assertEquals(restingCaption,caption.ink(),"$key released caption")
            assertEquals(restingIcon,icon.centerPixel(),"$key released icon")
        }
        val iconButton=onNodeWithTag("icon-button")
        assertEquals(Color.White,iconButton.centerPixel())
        iconButton.performTouchInput { down(center) };mainClock.advanceTimeBy(800)
        assertEquals(Color.Black,iconButton.centerPixel())
        iconButton.save("held-icon-button")
        iconButton.performTouchInput { up() };mainClock.advanceTimeBy(240)
        assertEquals(Color.White,iconButton.centerPixel())
        val disabled=onNodeWithText("Disabled")
        val disabledInk=disabled.ink()
        assertEquals(Color(0xff565657),disabledInk,"Disabled captions inherit the button's dim ink")
        disabled.performTouchInput { down(center) };mainClock.advanceTimeBy(800)
        assertEquals(disabledInk,disabled.ink())
        disabled.performTouchInput { up() }
        val leading=onNodeWithTag("leading-button")
        val leadingText=mutableListOf<TextLayoutResult>()
        onNodeWithText("Leading").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(leadingText) }
        assertTrue(leadingText.single().layoutInput.style.fontStyle!=FontStyle.Italic,"Buttons do not force italic text")
        val glyph=onNodeWithTag("leading-glyph",useUnmergedTree=true)
        leading.performTouchInput { down(center) };mainClock.advanceTimeBy(800)
        assertEquals(Color.Black,onNodeWithText("Leading").ink())
        var plate=leading.captureToImage().toPixelMap()
        val first=glyph.centerPixel()
        assertEquals(plate[plate.width-12,plate.height/2],first,"Leading glyph matches the breathing plate")
        leading.save("held-leading-button")
        onRoot().save("held-leading-context")
        mainClock.advanceTimeBy(320)
        plate=leading.captureToImage().toPixelMap()
        assertTrue(first!=glyph.centerPixel(),"Leading glyph must keep breathing")
        assertEquals(plate[plate.width-12,plate.height/2],glyph.centerPixel(),"Leading glyph stays in phase")
        leading.performTouchInput { up() };mainClock.advanceTimeBy(240)
        assertEquals(Color.Black,glyph.centerPixel())
        for(variant in listOf(ZenlessButtonVariant.Plain,ZenlessButtonVariant.Hollow)) {
            runOnIdle { leadingVariant=variant };mainClock.advanceTimeBy(16)
            val pixels=leading.captureToImage().toPixelMap()
            assertTrue(pixels[22,8].red<=9/255f,"$variant preserves the dark leading disc")
        }
        runOnIdle { leadingVariant=ZenlessButtonVariant.Filled;leadingRound=false };mainClock.advanceTimeBy(16)
        assertTrue(leading.captureToImage().toPixelMap()[6,6].red<=9/255f,"Square buttons retain their face outside the leading disc")
        runOnIdle { leadingRound=true;direction=LayoutDirection.Rtl };mainClock.advanceTimeBy(16)
        val rtl=leading.captureToImage().toPixelMap()
        assertEquals(Color(0xff262626),rtl[rtl.width-50,26],"The joined ring stays with the leading icon in RTL")
    }

    @Test fun navigationAndSelectKeepTheirInkUntilSelectionChanges() = runDesktopComposeUiTest(width=1000,height=760) {
        mainClock.autoAdvance=false
        setContent { ZenlessTheme { ZenlessOverlayHost {
            Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
                ZenlessTabs(listOf("Tab selected","Tab idle"),0,{},Modifier.width(600.dp))
                ZenlessTabs(listOf("Color selected","Color idle"),0,{},Modifier.width(600.dp),expand=false)
                ZenlessTabs(listOf("Folder selected","Folder idle"),0,{},Modifier.width(600.dp),folder=true)
                ZenlessNavigation(listOf("Navigation selected","Navigation idle"),0,{},Modifier.width(320.dp))
                ZenlessCollapse("Collapse",false,{},Modifier.width(400.dp)) {}
                ZenlessSelect(listOf("Current","Other"),0,{},Modifier.width(320.dp),label="Choice")
            }
        } } }
        mainClock.advanceTimeBy(200)
        for(label in listOf("Tab idle","Color idle","Folder idle","Navigation idle","Collapse")) {
            val button=onNodeWithText(label)
            val resting=button.ink()
            button.performTouchInput { down(center) };mainClock.advanceTimeBy(800)
            assertEquals(if(label=="Collapse")Color.Black else resting,button.ink(),"Only the collapse action uses button press feedback")
            button.save("held-${label.replace(' ','-')}")
            button.performTouchInput { up() }
            repeat(12) {
                mainClock.advanceTimeByFrame()
                if(label!="Collapse")assertEquals(resting,button.ink(),"$label must not flash after release")
            }
            mainClock.advanceTimeBy(240)
            assertEquals(resting,button.ink(),"$label released text")
        }
        val select=onNodeWithContentDescription("Choice")
        select.performTouchInput { down(center) };mainClock.advanceTimeBy(800)
        assertEquals(Color.White,onNodeWithText("Current").ink())
        val pixels=select.captureToImage().toPixelMap()
        assertEquals(Color.White,pixels[pixels.width-26,pixels.height/2],"Select keeps its white arrow while pressed")
        select.save("held-select")
        select.performTouchInput { up() };mainClock.advanceTimeBy(300)
        val other=onNodeWithText("Other")
        assertEquals(Color.White,other.ink())
        other.performTouchInput { down(center) };mainClock.advanceTimeBy(800)
        assertEquals(Color.White,other.ink(),"Unselected options keep their resting ink before activation")
        other.save("held-option")
        other.performTouchInput { up() };mainClock.advanceTimeBy(240)
        onNodeWithText("Other").assertDoesNotExist()
        assertEquals(Color.White,onNodeWithText("Current").ink(),"Closed select restores its white ink")
    }

    @Test fun alertLeadingGlyphsBreatheInsideBlackDiscs() = runDesktopComposeUiTest(width=1024,height=768) {
        mainClock.autoAdvance=false
        var clicks=0
        setContent { ZenlessTheme { ZenlessOverlayHost {
            ZenlessAlert(true,"Confirm your selection","Apply",{clicks++},{clicks++},"Cancel")
        } } }
        mainClock.advanceTimeBy(400)
        for(label in listOf("Apply","Cancel")) {
            val button=onNodeWithText(label)
            button.performTouchInput { down(center) };mainClock.advanceTimeBy(800)
            assertEquals(Color.Black,button.ink(),"$label held text")
            val pixels=button.captureToImage().toPixelMap()
            val first=if(label=="Apply")pixels[25,33] else pixels[27,28]
            assertEquals(pixels[pixels.width-14,pixels.height/2],first,"$label leading glyph matches the plate")
            assertEquals(Color.Black,pixels[27,40],"$label leading glyph has a black disc")
            button.save("held-alert-$label")
            onRoot().save("held-alert-$label-context")
            mainClock.advanceTimeBy(320)
            val later=button.captureToImage().toPixelMap()
            val next=if(label=="Apply")later[25,33] else later[27,28]
            assertTrue(first!=next,"$label leading glyph keeps breathing")
            assertEquals(later[later.width-14,later.height/2],next,"$label glyph stays in phase")
            button.performTouchInput { moveTo(Offset(-1000f,-1000f));up() };mainClock.advanceTimeBy(240)
            assertEquals(Color.White,button.ink(),"$label cancelled press")
        }
        assertEquals(0,clicks,"Dragging out of the actions must not activate them")
    }

    private fun SemanticsNodeInteraction.ink():Color {
        val results=mutableListOf<TextLayoutResult>()
        performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        return results.single().layoutInput.style.color
    }
    private fun SemanticsNodeInteraction.centerPixel():Color {
        val pixels=captureToImage().toPixelMap()
        return pixels[pixels.width/2,pixels.height/2]
    }
    private fun SemanticsNodeInteraction.save(name:String) {
        val file=File("../verification/$name.png");file.parentFile.mkdirs()
        Image.makeFromBitmap(captureToImage().asSkiaBitmap()).use { image -> image.encodeToData()?.use { file.writeBytes(it.bytes) } }
    }
}
