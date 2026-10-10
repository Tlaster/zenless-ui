package moe.tlaster.zenlessui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize

@Composable
internal fun Modifier.buttonClick(enabled: Boolean, source: MutableInteractionSource, onClick: () -> Unit): Modifier =
    then(ButtonPressElement(source, enabled && LocalWindowInfo.current.isWindowFocused))
        .pointerClick(enabled, remember { MutableInteractionSource() }, onClick = onClick)

private data class ButtonPressElement(val source: MutableInteractionSource, val enabled: Boolean) : ModifierNodeElement<ButtonPressNode>() {
    override fun create() = ButtonPressNode(source, enabled)
    override fun update(node: ButtonPressNode) {
        if (node.source !== source || node.enabled != enabled) node.interrupt()
        node.source = source
        node.enabled = enabled
    }
    override fun InspectorInfo.inspectableProperties() { name = "buttonPress" }
}

// Leave movement unconsumed: scrolling may cancel a click, but only release ends its visual hold.
internal class ButtonPressNode(var source: MutableInteractionSource, var enabled: Boolean) : Modifier.Node(), PointerInputModifierNode {
    private var pointer: PointerId? = null
    private var press: PressInteraction.Press? = null
    private var interrupted: PointerId? = null

    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        if (pass != PointerEventPass.Initial) return
        if (interrupted != null) {
            if (pointerEvent.type == PointerEventType.Press && pointerEvent.changes.none { it.previousPressed }) interrupted = null
            else {
                val change = pointerEvent.changes.firstOrNull { it.id == interrupted }
                if (change != null && !change.pressed) { change.consume(); interrupted = null }
                return
            }
        }
        if (!enabled) return
        val held = press
        if (held == null) {
            if (pointerEvent.type != PointerEventType.Press || pointerEvent.changes.any { it.previousPressed }) return
            val down = pointerEvent.changes.firstOrNull { it.changedToDownIgnoreConsumed() } ?: return
            if (down.type == PointerType.Mouse && !pointerEvent.buttons.isPrimaryPressed) return
            if (down.position.x !in 0f..<bounds.width.toFloat() || down.position.y !in 0f..<bounds.height.toFloat()) return
            pointer = down.id
            press = PressInteraction.Press(down.position).also { source.tryEmit(it) }
        } else {
            val change = pointerEvent.changes.firstOrNull { it.id == pointer } ?: return
            val released = if (change.type == PointerType.Mouse) !pointerEvent.buttons.isPrimaryPressed else !change.pressed
            if (released) {
                if (pointerEvent.type == PointerEventType.Release) source.tryEmit(PressInteraction.Release(held))
                else source.tryEmit(PressInteraction.Cancel(held))
                pointer = null
                press = null
            }
        }
    }

    override fun onCancelPointerInput() {
        press?.let { source.tryEmit(PressInteraction.Cancel(it)) }
        pointer = null
        press = null
        interrupted = null
    }
    fun interrupt() {
        val heldPointer = pointer ?: interrupted
        onCancelPointerInput()
        // Re-enabling before up must not revive the native clickable's interrupted gesture.
        interrupted = heldPointer
    }
    override fun onDetach() = onCancelPointerInput()
    override fun onReset() = onCancelPointerInput()
}
