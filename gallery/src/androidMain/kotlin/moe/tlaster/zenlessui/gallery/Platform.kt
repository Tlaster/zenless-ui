package moe.tlaster.zenlessui.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import java.util.Locale

internal actual fun defaultChinese(): Boolean = Locale.getDefault().language == "zh"
@Composable internal actual fun GalleryBackHandler(enabled: Boolean, onBack: () -> Unit) { BackHandler(enabled, onBack) }
