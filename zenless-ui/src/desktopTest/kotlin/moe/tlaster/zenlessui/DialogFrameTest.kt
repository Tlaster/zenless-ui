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
            assertTrue(edge in 180..200, "Top AA coverage on white: $edge")
            assertEquals(0, pixels.getRGB(400, 33) and 255)
        } } } finally { scene.close() }
    }

    @Test fun frameClipsContentSoftensInnerJoinAndKeepsBevelTranslucentAcrossSizesAndDirections() {
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
                val bevel = rgb(176f, 236 + 2 * scale)
                for ((shift, expected) in listOf(16 to 59, 8 to 93, 0 to 128)) {
                    assertTrue(abs(((bevel shr shift) and 255) - expected) <= 2, "Bevel transmits the colored backing")
                }
                assertEquals(0x4080c0, rgb(176f, 236 + 7 * scale), "No hard shadow beyond the bevel")
            } } } finally { scene.close() }
        }
    }

    @Test fun capturesWholeFrameWithCleanedReferenceBacking() {
        val folder = File("../verification/dialog-border")
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
