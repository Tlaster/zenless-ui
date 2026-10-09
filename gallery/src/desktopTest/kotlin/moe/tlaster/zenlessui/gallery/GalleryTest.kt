package moe.tlaster.zenlessui.gallery

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.jetbrains.skia.Image
import java.io.File
import java.util.Locale
import org.junit.Before
import org.junit.After

class GalleryTest {
    @get:Rule val compose = createComposeRule()
    private val originalLocale = Locale.getDefault()
    @Before fun locale() { Locale.setDefault(Locale.SIMPLIFIED_CHINESE) }
    @After fun restoreLocale() { Locale.setDefault(originalLocale) }

    @Test fun galleryNavigatesAndCapturesBothLayouts() {
        compose.mainClock.autoAdvance = false
        var wide by mutableStateOf(true)
        compose.setContent { Box(Modifier.requiredSize(if (wide) 1024.dp else 390.dp, 768.dp)) { GalleryApp() } }
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithText("ZENLESS UI").assertExists()
        capture("gallery-wide")
        Demo.entries.drop(1).forEach { page ->
            compose.onAllNodesWithText(page.zh)[0].performClick()
            compose.mainClock.advanceTimeBy(400)
            capture("gallery-${page.name.lowercase()}")
        }
        compose.onAllNodesWithText("按钮")[0].performClick()
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithText("实时预览").assertExists()
        capture("gallery-buttons")
        compose.runOnIdle { wide = false }
        compose.mainClock.advanceTimeBy(400)
        capture("gallery-mobile")
    }

    private fun capture(name: String) {
        val bitmap = compose.onRoot().captureToImage()
        val file = File("../verification/$name.png")
        file.parentFile.mkdirs()
        Image.makeFromBitmap(bitmap.asSkiaBitmap()).use { image -> image.encodeToData()?.use { file.writeBytes(it.bytes) } }
    }
}
