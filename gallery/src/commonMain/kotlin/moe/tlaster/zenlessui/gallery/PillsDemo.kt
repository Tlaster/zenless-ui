package moe.tlaster.zenlessui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import moe.tlaster.zenlessui.*

@Composable
internal fun PillsDemo(tr: (String, String) -> String, record: (String) -> Unit) {
    var selected by remember { mutableStateOf(false) }
    var enabled by remember { mutableStateOf(true) }
    var fillWidth by remember { mutableStateOf(false) }
    var longText by remember { mutableStateOf(false) }
    val label = if (longText) tr("An expanding content slot with room for a longer description.", "内容插槽可以容纳更长的说明，空间不足时自然换行并撑高容器。") else tr("Signal studio", "信号工作室")
    Preview(tr("Live preview", "实时预览")) {
        ZenlessText(tr("Hold to highlight. Click to toggle this example's selection. Only the rim color breathes.", "按住高亮，点击切换示例的选中状态；只有边框颜色呼吸。"))
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xff63462d), Color(0xff173946)))).padding(16.dp)) {
            ZenlessPillContainer(
                modifier = (if (fillWidth) Modifier.fillMaxWidth() else Modifier).testTag("pill-preview"),
                onClick = { selected = !selected; record(tr("Pill clicked", "点击胶囊")) },
                selected = selected, enabled = enabled,
            ) { ZenlessText(label) }
        }
    }
    Preview(tr("Parameters", "参数")) {
        ZenlessSwitch(selected, { selected = it }, label = tr("Selected", "选中胶囊"))
        ZenlessSwitch(enabled, { enabled = it }, label = tr("Enabled", "启用胶囊"))
        ZenlessSwitch(fillWidth, { fillWidth = it }, label = tr("Fill available width", "填满可用宽度"))
        ZenlessSwitch(longText, { longText = it }, label = tr("Long content", "长内容"))
    }
    CodeExample(pillExample(label, selected, enabled, fillWidth), tr)
    Preview(tr("Display-only content composition", "只读内容组合")) {
        Box(Modifier.padding(8.dp)) {
            ZenlessPillContainer {
                Column {
                    ZenlessText(tr("Signal studio", "信号工作室"))
                    ZenlessText("7905 / 61525", style = ZenlessTextStyle.Caption)
                }
                Spacer(Modifier.width(24.dp))
                ZenlessText("52")
            }
        }
    }
}

internal fun pillExample(label: String, selected: Boolean, enabled: Boolean, fillWidth: Boolean): String = """
    var selected by remember { mutableStateOf($selected) }
    Box(Modifier.padding(8.dp)) {
        ZenlessPillContainer(
            modifier = ${if (fillWidth) "Modifier.fillMaxWidth()" else "Modifier"},
            onClick = { selected = !selected },
            selected = selected,
            enabled = $enabled,
        ) {
            ZenlessText(${quote(label)})
        }
    }
""".trimIndent()
