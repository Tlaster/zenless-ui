@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
package moe.tlaster.zenlessui.gallery

import androidx.compose.ui.platform.ClipEntry

internal actual fun textClipEntry(text: String) = ClipEntry.withPlainText(text)
