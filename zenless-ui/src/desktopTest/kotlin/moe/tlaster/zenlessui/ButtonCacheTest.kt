package moe.tlaster.zenlessui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import javax.imageio.ImageIO
import org.junit.Test
import kotlin.math.abs
import kotlin.test.assertTrue

@OptIn(ExperimentalComposeUiApi::class)
class ButtonCacheTest {
    private data class Appearance(
        val width:Int=244,val height:Int=58,val fill:Color=Color.Black,val round:Boolean=true,
        val pattern:Boolean=true,val direction:LayoutDirection=LayoutDirection.Ltr,
    )

    @Test fun nativeSizeCacheMatchesAcceptedVectorAndRefreshesWhenAppearanceChanges() {
        val cases=listOf(Appearance(),Appearance(190,40),Appearance(191,40),Appearance(height=52),Appearance(height=62),
            Appearance(round=false),Appearance(fill=Color(0xff999999)),Appearance(direction=LayoutDirection.Rtl),
            Appearance(fill=Color(0xff999999),direction=LayoutDirection.Rtl),Appearance(pattern=false))
        for(density in listOf(1f,1.5f,2f,3f)) {
            val current=mutableStateOf(cases.first())
            val scenes=listOf(ButtonPrototype.Direct,ButtonPrototype.CachedDirect).map { mode ->
                ImageComposeScene((260*density).toInt(),(80*density).toInt(),Density(density)) {
                    val appearance=current.value
                    CompositionLocalProvider(LocalLayoutDirection provides appearance.direction) {
                        Box(Modifier.size(appearance.width.dp,appearance.height.dp).background(Color.Black).drawWithCache {
                            val drawing=prototypeShell(mode,appearance.fill,Color(0xff262626),appearance.round,appearance.pattern)
                            onDrawBehind { drawing() }
                        })
                    }
                }
            }
            try {
                cases.forEachIndexed { index,appearance ->
                    current.value=appearance
                    val images=scenes.map { scene -> scene.render(index*32_000_000L).use { image ->
                        image.encodeToData()!!.use { ImageIO.read(it.bytes.inputStream()) }
                    } }
                    var maximum=0
                    for(y in 0 until images[0].height)for(x in 0 until images[0].width) {
                        val a=images[0].getRGB(x,y);val b=images[1].getRGB(x,y)
                        for(shift in 0..24 step 8)maximum=maxOf(maximum,abs((a ushr shift and 255)-(b ushr shift and 255)))
                    }
                    assertTrue(maximum<=1,"Cache changed the accepted vector beyond one rounding level: density=$density, $appearance, error=$maximum")
                }
            } finally { scenes.forEach { it.close() } }
        }
    }
}
