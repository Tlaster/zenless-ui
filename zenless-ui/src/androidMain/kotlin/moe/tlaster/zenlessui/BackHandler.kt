package moe.tlaster.zenlessui

import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler

@Composable
internal actual fun ModalBackHandler(enabled: Boolean, onBack: () -> Unit) = BackHandler(enabled, onBack)
