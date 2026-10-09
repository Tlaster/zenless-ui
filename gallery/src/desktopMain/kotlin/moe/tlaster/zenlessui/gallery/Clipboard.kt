@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
package moe.tlaster.zenlessui.gallery

import androidx.compose.ui.platform.ClipEntry
import java.awt.datatransfer.StringSelection

internal actual fun textClipEntry(text: String) = ClipEntry(StringSelection(text))
