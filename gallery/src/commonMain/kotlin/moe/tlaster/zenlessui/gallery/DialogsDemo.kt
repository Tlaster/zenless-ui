package moe.tlaster.zenlessui.gallery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.tlaster.zenlessui.*

@Composable
internal fun DialogsDemo(tr: (String, String) -> String, record: (String) -> Unit, visible: Boolean, setVisible: (Boolean) -> Unit) {
    var pattern by remember { mutableStateOf("026") }
    var custom by remember { mutableStateOf(false) }
    var narrow by remember { mutableStateOf(false) }
    var longContent by remember { mutableStateOf(false) }
    val title = tr("More actions", "更多操作")
    val close = tr("Close dialog", "关闭对话框")
    val labels = listOf(tr("Change username", "修改用户名"), tr("Show birthday", "公开生日信息"),
        tr("Change signature", "修改签名"), tr("Agent showcase", "修改代理人展柜"), tr("Social settings", "社交设置"))
    Preview(tr("Dialog", "对话框")) {
        ZenlessButton({ setVisible(true) }, Modifier.testTag("open-dialog")) { ZenlessText(tr("Open dialog", "打开对话框")) }
        ZenlessSwitch(custom, { custom = it }, label = tr("Custom background slot", "自定义背景插槽"))
        ZenlessSwitch(narrow, { narrow = it }, label = tr("Narrow window", "窄窗口"))
        ZenlessSwitch(longContent, { longContent = it }, label = tr("Scrollable long content", "可滚动长内容"))
    }
    Preview(tr("Reusable animated background", "独立动态背景")) {
        ZenlessTextField(pattern, { pattern = it }, label = tr("Pattern text", "纹样文字"))
        ZenlessAnimatedBackground(Modifier.fillMaxWidth().height(240.dp).testTag("background-preview")) {
            DemoPattern(pattern)
        }
    }
    CodeExample(dialogExample(title, close, labels, pattern, custom, narrow, longContent), tr)
    CodeExample(backgroundExample(pattern), tr)
    val body: @Composable ColumnScope.() -> Unit = {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(end = 1.5.dp)) {
            val columns = if (maxWidth < 500.dp) 1 else 2
            Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                labels.chunked(columns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(24.5.dp)) {
                        row.forEach { label -> ZenlessButton({ record(label) }, Modifier.weight(1f).heightIn(min = 58.dp)) { ZenlessText(label) } }
                        if (row.size < columns) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        if (longContent) repeat(20) { ZenlessText(tr("Additional content ${it + 1}", "补充内容 ${it + 1}")) }
    }
    val window = (if (narrow) Modifier.width(400.dp) else Modifier).testTag("gallery-dialog")
    val dismiss = { setVisible(false); record(tr("Dialog closed", "对话框已关闭")) }
    if (custom) ZenlessDialog(visible, title, close, dismiss, window,
        background = { ZenlessAnimatedBackground { DemoPattern(pattern) } }, content = body)
    else ZenlessDialog(visible, title, close, dismiss, window, content = body)
}

@Composable
private fun DemoPattern(text: String) {
    Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        BasicText(text, style = TextStyle(fontSize = 120.sp, fontWeight = FontWeight.Black, color = Color.White), maxLines = 1)
        Canvas(Modifier.size(72.dp)) { drawCircle(Color.White, style = Stroke(12.dp.toPx())) }
    }
}

internal fun backgroundExample(text: String): String = """
    ZenlessAnimatedBackground(Modifier.fillMaxWidth().height(240.dp)) {
        Row(
            Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            BasicText(${quote(text)}, style = TextStyle(
                fontSize = 120.sp, fontWeight = FontWeight.Black, color = Color.White,
            ), maxLines = 1)
            Canvas(Modifier.size(72.dp)) {
                drawCircle(Color.White, style = Stroke(12.dp.toPx()))
            }
        }
    }
""".trimIndent()

internal fun dialogExample(title: String, close: String, labels: List<String>, pattern: String, custom: Boolean, narrow: Boolean, longContent: Boolean): String {
    val background = if (custom) "\n                background = {\n${backgroundExample(pattern).replace("Modifier.fillMaxWidth().height(240.dp)", "Modifier.fillMaxSize()").prependIndent("                    ")}\n                }," else ""
    val extra = if (longContent) "\n                repeat(20) { ZenlessText(\"Additional content \${it + 1}\") }" else ""
    return """
        ZenlessOverlayHost {
            var visible by remember { mutableStateOf(false) }
            ZenlessButton({ visible = true }) { ZenlessText(${quote(title)}) }
            ZenlessDialog(
                visible = visible,
                title = ${quote(title)},
                closeDescription = ${quote(close)},
                onDismissRequest = { visible = false },
                modifier = ${if (narrow) "Modifier.width(400.dp)" else "Modifier"},$background
            ) {
                BoxWithConstraints(Modifier.fillMaxWidth().padding(end = 1.5.dp)) {
                    val columns = if (maxWidth < 500.dp) 1 else 2
                    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                        listOf(${labels.joinToString(", ") { quote(it) }}).chunked(columns).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(24.5.dp)) {
                                row.forEach { label ->
                                    ZenlessButton({}, Modifier.weight(1f).heightIn(min = 58.dp)) {
                                        ZenlessText(label)
                                    }
                                }
                                if (row.size < columns) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }$extra
            }
        }
    """.trimIndent()
}
