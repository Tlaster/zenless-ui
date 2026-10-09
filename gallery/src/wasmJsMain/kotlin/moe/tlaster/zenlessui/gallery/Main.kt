@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
package moe.tlaster.zenlessui.gallery

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window

public fun main() { ComposeViewport(document.body!!) { GalleryApp() } }
internal actual fun defaultChinese(): Boolean = window.navigator.language.startsWith("zh")
@Composable internal actual fun GalleryBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
