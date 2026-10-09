package moe.tlaster.zenlessui.gallery

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages
import platform.UIKit.UIViewController

public fun MainViewController(): UIViewController = ComposeUIViewController { GalleryApp() }
internal actual fun defaultChinese(): Boolean = (NSLocale.preferredLanguages.firstOrNull() as? String)?.startsWith("zh") == true
@Composable internal actual fun GalleryBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
