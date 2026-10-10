package moe.tlaster.zenlessui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.*
import org.jetbrains.skia.Image
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class DialogFrameTest {
    @Test fun nativeSilhouetteMatchesReferenceScanlinesWithoutSourceArtwork() {
        val scene = ImageComposeScene(1588, 1036, Density(2f)) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                Box(Modifier.offset(16.dp, 16.dp).size(762.dp, 482.dp).dialogFrame(1f).background(Color.Black))
            }
        }
        try { scene.render(0).use { image -> image.encodeToData()!!.use {
            val pixels = ImageIO.read(it.bytes.inputStream())
            // Native capture coordinates; include the nearly horizontal part of the top-left curve.
            for ((y, x) in listOf(599 to 1211, 600 to 1205, 602 to 1199, 605 to 1192,
                610 to 1184, 620 to 1173, 630 to 1166, 640 to 1161, 650 to 1159, 660 to 1158)) {
                val first = (0 until 160).first { column -> (pixels.getRGB(column, y - 566) and 255) < 128 }
                assertTrue(abs(first - (x - 1126)) <= 1, "Native curve at y=$y: ${first + 1126}, expected $x +/- 1")
            }
            // Partial coverage at the top edge must be applied once, even with opaque child content.
            val edge = pixels.getRGB(400, 32) and 255
            assertTrue(edge in 110..135, "Single top AA coverage over the translucent rim: $edge")
            assertEquals(0, pixels.getRGB(400, 33) and 255)
        } } } finally { scene.close() }
    }

    @Test fun frameClipsContentAndHasATranslucentRimOnEverySideAcrossSizesAndDirections() {
        for (density in listOf(1f, 2f)) for (preset in ZenlessSize.entries) for (rtl in listOf(false, true)) {
            val scale = preset.scale
            val scene = ImageComposeScene((352 * density).toInt(), (252 * density).toInt(), Density(density)) {
                CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                    Box(Modifier.fillMaxSize().background(Color(0xff4080c0))) {
                        Box(Modifier.offset(16.dp, 16.dp).size(320.dp, 220.dp).dialogFrame(scale).background(Color.White))
                    }
                }
            }
            try { scene.render(0).use { image -> image.encodeToData()!!.use {
                val pixels = ImageIO.read(it.bytes.inputStream())
                fun rgb(x: Float, y: Float) = pixels.getRGB((x * density).toInt(), (y * density).toInt()) and 0xffffff
                assertEquals(0x4080c0, rgb(if (rtl) 333f else 19f, 19f), "Rounded corner clips the white child")
                assertEquals(0, rgb(if (rtl) 18f else 334f, 18f), "Top-end stays square and black")
                assertEquals(0, rgb(176f, 16 + 2 * scale), "Opaque black band")
                assertTrue((rgb(176f, 16 + 5 * scale) and 255) in 50..250, "Inner edge has partial coverage")
                assertEquals(0xffffff, rgb(176f, 16 + 8 * scale), "Content remains sharp inside the band")
                val sides = listOf(176f to 16 - 2 * scale, 176f to 236 + 2 * scale,
                    16 - 2 * scale to 126f, 336 + 2 * scale to 126f)
                for ((x, y) in sides) {
                    val rim = rgb(x, y)
                    val expected = 0x3d648a
                    for (shift in listOf(16, 8, 0)) {
                        assertTrue(abs(((rim shr shift) and 255) - ((expected shr shift) and 255)) <= 2, "Rim at ($x,$y) transmits the colored backing")
                    }
                }
                assertEquals(0x4080c0, rgb(176f, 236 + 7 * scale), "No hard shadow beyond the rim")
            } } } finally { scene.close() }
        }
    }

    @Test fun outerRimTransmitsBothLightAndDarkBacking() {
        val scene = ImageComposeScene(472, 252, Density(1f)) {
            Box(Modifier.fillMaxSize()) {
                Canvas(Modifier.fillMaxSize()) {
                    for (x in 0 until 472 step 20) drawRect(if (x % 40 == 0) Color.Black else Color.White,
                        Offset(x.toFloat(), 0f), androidx.compose.ui.geometry.Size(20f, 252f))
                }
                Box(Modifier.offset(16.dp, 16.dp).size(440.dp, 220.dp).dialogFrame(1f))
            }
        }
        try { scene.render(0).use { image -> image.encodeToData()!!.use {
            val pixels = ImageIO.read(it.bytes.inputStream())
            for (y in listOf(14, 238)) {
                assertTrue((pixels.getRGB(90, y) and 255) in 21..25, "Rim over black")
                assertTrue((pixels.getRGB(110, y) and 255) in 174..178, "Rim over white")
            }
            val file = File("../verification/dialog-rim/transparency.png")
            file.parentFile.mkdirs(); file.writeBytes(it.bytes)
        } } } finally { scene.close() }
    }

    @Test fun capturesWholeFrameWithCleanedReferenceBacking() {
        val folder = File("../verification/dialog-rim")
        assumeTrue(File(folder,"backing.png").exists())
        runDesktopComposeUiTest(width = 1588, height = 1036) {
            mainClock.autoAdvance = false
            setContent { CompositionLocalProvider(LocalDensity provides Density(2f)) {
                val backing = remember { Image.makeFromEncoded(File(folder,"backing.png").readBytes()).toComposeImageBitmap() }
                val face = remember { Image.makeFromEncoded(File(folder,"face.png").readBytes()).toComposeImageBitmap() }
                Box(Modifier.fillMaxSize()) {
                    Canvas(Modifier.fillMaxSize()) { drawImage(backing) }
                    Canvas(Modifier.offset(16.dp,16.dp).size(762.dp,482.dp).dialogFrame(1f)) { drawImage(face,Offset(-32f,-32f)) }
                }
            } }
            mainClock.advanceTimeByFrame()
            Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image ->
                image.encodeToData()!!.use { File(folder,"actual.png").writeBytes(it.bytes) }
            }
        }
    }
}
