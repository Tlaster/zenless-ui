package moe.tlaster.zenlessui.gallery

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.util.Locale

public fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Zenless UI Gallery", state = rememberWindowState(width = 1280.dp, height = 900.dp)) { GalleryApp() }
}
internal actual fun defaultChinese(): Boolean = Locale.getDefault().language == "zh"
@Composable internal actual fun GalleryBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
