package moe.tlaster.zenlessui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import java.io.File
import javax.imageio.ImageIO
import org.junit.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalComposeUiApi::class)
class ButtonBorderTest {
    @Test fun resourceManagementBorderAtReferenceDensity() {
        val scene = ImageComposeScene(650, 145, Density(2f)) {
            ZenlessTheme {
                Box(Modifier.size(325.dp, 72.5f.dp).background(Color.Black)) {
                    ZenlessButton({}, Modifier.offset(13.dp, 9.5f.dp).size(299.dp, 58.dp),
                        leadingIcon = {}) {}
                }
            }
        }
        try {
            scene.render(0).use { image -> image.encodeToData()!!.use {
                val file = File("../verification/button-border/actual.png")
                file.parentFile.mkdirs()
                file.writeBytes(it.bytes)
                val pixels = ImageIO.read(it.bytes.inputStream())
                for ((x, gray) in listOf(200 to 61, 350 to 61, 450 to 61, 480 to 59, 500 to 57, 530 to 51, 560 to 45)) {
                    assertTrue(abs((pixels.getRGB(x, 20) and 255) - gray) <= 2, "Reference upper bevel at $x")
                }
                assertEquals(38, pixels.getRGB(200, 26) and 255, "The rim stays #262626")
            } }
        } finally { scene.close() }
    }

    @Test fun highlightScalesWithWidthAndBlackSurroundRemainsOutsideLayout() {
        for (density in listOf(1f, 2f)) for (width in listOf(190, 299, 400)) for (leading in listOf(false, true)) {
            val scene = ImageComposeScene(((width + 8) * density).toInt(), (66 * density).toInt(), Density(density)) {
                ZenlessTheme {
                    Box(Modifier.size((width + 8).dp, 66.dp).background(Color(0xff242424))) {
                        ZenlessButton({}, Modifier.offset(4.dp, 4.dp).size(width.dp, 58.dp),
                            enabled = false, leadingIcon = if (leading) ({}) else null) {}
                    }
                }
            }
            try {
                scene.render(0).use { image -> image.encodeToData()!!.use {
                    val pixels = ImageIO.read(it.bytes.inputStream())
                    fun gray(x: Float, y: Float) = pixels.getRGB((x * density).toInt(), (y * density).toInt()) and 255
                    assertEquals(36, gray(0f, 33f), "Backdrop is retained outside the surround")
                    assertEquals(0, gray(3f, 33f), "Black surround extends beyond the layout on the left")
                    assertEquals(0, gray(width / 2f + 4, 2.5f), "Black surround above the upper bevel")
                    assertEquals(0, gray(width / 2f + 4, 63f), "Black surround below the rim")
                    assertEquals(61, gray(width * .55f + 4, 5f), "Upper highlight must not darken as width grows")
                    assertEquals(38, gray(width / 2f + 4, 60f), "Lower rim stays neutral")
                } }
            } finally { scene.close() }
        }
    }
}
