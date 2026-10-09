package moe.tlaster.zenlessui.gallery

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry

internal actual fun textClipEntry(text: String) = ClipEntry(ClipData.newPlainText("Kotlin", text))
