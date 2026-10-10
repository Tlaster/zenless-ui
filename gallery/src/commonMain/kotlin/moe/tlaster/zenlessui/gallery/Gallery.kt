package moe.tlaster.zenlessui.gallery

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import moe.tlaster.zenlessui.*
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

internal expect fun defaultChinese(): Boolean
internal expect fun textClipEntry(text: String): ClipEntry
@Composable internal expect fun GalleryBackHandler(enabled: Boolean, onBack: () -> Unit)
private val LocalExampleSize = compositionLocalOf { ZenlessSize.Default }

internal fun sizedExample(code: String, size: ZenlessSize): String =
    "ZenlessTheme(size = ZenlessSize.${size.name}) {\n${code.prependIndent("    ")}\n}"

internal enum class Demo(val en: String, val zh: String) {
    Foundations("Foundations", "视觉基础"), Buttons("Buttons", "按钮"), Navigation("Navigation", "导航与页签"),
    Fields("Text fields", "文本输入"), Select("Select & slider", "选择器与滑条"), Selection("Selection controls", "选择控件"),
    Display("Cards & feedback", "卡片与反馈"), Overlays("Overlays", "弹层"), Tooltip("Tooltip", "工具提示"), Examples("Settings example", "设置页示例")
}

/** The gallery uses only public library APIs, including every interactive preview. */
@Composable
public fun GalleryApp() {
    var chinese by remember { mutableStateOf(defaultChinese()) }
    var page by remember { mutableStateOf(Demo.Foundations) }
    var detail by remember { mutableStateOf(false) }
    var alert by remember { mutableStateOf(false) }
    var drawer by remember { mutableStateOf(false) }
    var events by remember { mutableIntStateOf(0) }
    var lastEvent by remember { mutableStateOf("") }
    var alertMessage by remember { mutableStateOf<String?>(null) }
    var allowCancel by remember { mutableStateOf(true) }
    var size by remember { mutableStateOf(ZenlessSize.Default) }
    val message = alertMessage ?: if (chinese) "应用这些修改？\n当前设置将被替换。" else "Apply these changes?\nYour existing settings will be replaced."
    val contentScroll = rememberScrollState()
    LaunchedEffect(page) { contentScroll.scrollTo(0) }
    val tr: (String, String) -> String = { en, zh -> if (chinese) zh else en }
    val record: (String) -> Unit = { events++; lastEvent = it }
    GalleryBackHandler(detail && !alert && !drawer) { detail = false }
    CompositionLocalProvider(LocalExampleSize provides size) {
    ZenlessTheme(size = size) {
        ZenlessOverlayHost(Modifier.fillMaxSize()) {
            ZenlessSurface(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                    Row(Modifier.fillMaxWidth().background(Color.Black).padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column(Modifier.weight(1f)) {
                            ZenlessText("ZENLESS UI", style = ZenlessTextStyle.Subtitle)
                            ZenlessText(tr("COMPONENT GALLERY / 0.1.0", "组件展台 / 0.1.0"), style = ZenlessTextStyle.Caption)
                        }
                        ZenlessButton({ chinese = !chinese }, size = ZenlessSize.Compact) { ZenlessText(if (chinese) "English" else "中文") }
                    }
                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                        val wide = maxWidth >= 900.dp
                        Row(Modifier.fillMaxSize()) {
                            if (wide || !detail) Column(Modifier.then(if (wide) Modifier.width(240.dp) else Modifier.fillMaxWidth()).fillMaxHeight().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                                ZenlessText(tr("EXPLORE COMPONENTS", "浏览组件"), style = ZenlessTextStyle.Caption)
                                ZenlessNavigation(Demo.entries.map { if (chinese) it.zh else it.en }, page.ordinal, {
                                    page = Demo.entries[it]; detail = true
                                }, Modifier.fillMaxWidth(), size=ZenlessSize.Compact)
                                if (!wide) ZenlessButton({ detail = true }, Modifier.fillMaxWidth(), tone = ZenlessTone.Accent) { ZenlessText(tr("Open selected", "打开当前分类")) }
                                ZenlessText(tr("Fixed palette. Full motion.\nNo icons, fonts or audio bundled.", "固定配色，完整动效。\n不附带图标集、字体或音效。"), style = ZenlessTextStyle.Caption)
                            }
                            if (wide || detail) Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(contentScroll).padding(if (wide) 32.dp else 20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                if (!wide) ZenlessBackButton({ detail = false }, tr("Back to categories", "返回分类"))
                                ZenlessText(if (chinese) page.zh else page.en, style = ZenlessTextStyle.Title)
                                ZenlessText(tr("Interact with the preview. Adjust parameters. Copy the Kotlin example.", "操作预览，调整参数，复制 Kotlin 示例。"), style = ZenlessTextStyle.Caption)
                                ZenlessSelect(listOf(tr("Compact · 40 dp / 14 sp", "紧凑 · 40 dp / 14 sp"), tr("Default · 52 dp / 20 sp", "常规 · 52 dp / 20 sp"), tr("Comfortable · 62 dp / 24 sp", "宽松 · 62 dp / 24 sp")), size.ordinal, { size=ZenlessSize.entries[it] }, label=tr("Control size", "控件尺寸"))
                                key(page) {
                                    when (page) {
                                        Demo.Foundations -> Foundations(tr)
                                        Demo.Buttons -> ButtonsDemo(tr, record)
                                        Demo.Navigation -> NavigationDemo(tr, record)
                                        Demo.Fields -> FieldsDemo(tr, record)
                                        Demo.Select -> SelectDemo(tr, record)
                                        Demo.Selection -> SelectionDemo(tr, record)
                                        Demo.Display -> DisplayDemo(tr)
                                        Demo.Overlays -> {
                                            Preview(tr("Full-width alert", "通栏 Alert")) {
                                                ZenlessButton({ alert = true }) { ZenlessText(tr("Open alert", "打开 Alert")) }
                                                ZenlessButton({ drawer = true }) { ZenlessText(tr("Open drawer", "打开抽屉")) }
                                            }
                                            Preview(tr("Parameters", "参数")) {
                                                ZenlessTextField(message, { alertMessage = it }, label = tr("Message", "提示内容"), singleLine = false)
                                                ZenlessSwitch(allowCancel, { allowCancel = it }, label = tr("Show cancel action", "显示取消操作"))
                                            }
                                            CodeExample(alertExample(chinese, message, allowCancel), tr)
                                            CodeExample(drawerExample(tr), tr)
                                        }
                                        Demo.Tooltip -> TooltipDemo(tr)
                                        Demo.Examples -> SettingsExample(tr, record, showCode = true)
                                    }
                                }
                                ZenlessNotice(tr("Events: $events · $lastEvent", "事件：$events · $lastEvent"))
                                ZenlessText(tr("moe.tlaster.zenlessui:zenless-ui:0.1.0 • MIT", "moe.tlaster.zenlessui:zenless-ui:0.1.0 • MIT"), style = ZenlessTextStyle.Caption)
                            }
                        }
                    }
                }
                ZenlessAlert(alert, message, tr("Apply", "应用"),
                    onConfirm = { alert = false; record(tr("Confirmed", "已确认")) }, onDismissRequest = if (allowCancel) {{ alert = false; record(tr("Cancelled", "已取消")) }} else null, cancelText = if (allowCancel) tr("Cancel", "取消") else null)
                ZenlessDrawer(drawer, tr("Preferences", "偏好设置"), tr("Close", "关闭"), { drawer = false }, footer = {
                    ZenlessButton({ drawer = false; record(tr("Saved", "已保存")) }, tone = ZenlessTone.Accent) { ZenlessText(tr("Save", "保存")) }
                }) { SettingsExample(tr, record) }
            }
        }
    }
    }
}

@Composable private fun Preview(title: String, content: @Composable ColumnScope.() -> Unit) {
    ZenlessCard(Modifier.fillMaxWidth()) { ZenlessText(title, style = ZenlessTextStyle.Subtitle); Spacer(Modifier.height(8.dp)); content() }
}

@Composable private fun CodeExample(example: String, tr: (String, String) -> String) {
    val code = sizedExample(example, LocalExampleSize.current)
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var copied by remember(code) { mutableStateOf(false) }
    var copying by remember { mutableStateOf(false) }
    var copyFailed by remember(code) { mutableStateOf(false) }
    ZenlessCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            ZenlessText(tr("Kotlin example", "Kotlin 示例"), style = ZenlessTextStyle.Subtitle)
            ZenlessButton({
                copying = true; copyFailed = false
                scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    try { clipboard.setClipEntry(textClipEntry(code)); copied = true }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { copyFailed = true }
                    finally { copying = false }
                }
            }, size = ZenlessSize.Compact, loading = copying) { ZenlessText(if (copied) tr("Copied", "已复制") else tr("Copy", "复制")) }
        }
        if (copyFailed) ZenlessNotice(tr("Copy failed. Select the example text to copy it manually.", "复制失败，请选中示例文字后手动复制。"), tone = ZenlessTone.Warning)
        SelectionContainer { ZenlessText(code, Modifier.fillMaxWidth().background(Color.Black).padding(16.dp)) }
    }
}

@Composable private fun Foundations(tr: (String, String) -> String) {
    Preview(tr("Control size comparison", "控件尺寸对照")) {
        for (size in ZenlessSize.entries) ZenlessTheme(size=size) {
            ZenlessText(size.name)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                ZenlessButton({}) { ZenlessText(tr("Action", "操作")) }
                ZenlessButton({},leadingIcon={CallerIcon()}) { ZenlessText(tr("Action", "操作")) }
                ZenlessSwitch(true,{})
                ZenlessCheckbox(ToggleableState.On,{},label=tr("Option", "选项"))
                ZenlessBackButton({},tr("Back", "返回")); ZenlessCloseButton({},tr("Close", "关闭")); ZenlessSpinner()
            }
            ZenlessTabs(listOf(tr("Files", "文件"),tr("Details", "详情")),0,{},folder=true)
        }
    }
    Preview(tr("Fixed semantic tones", "固定语义配色")) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { ZenlessTone.entries.forEach { ZenlessBadge(it.name, tone = it) } }
    }
    Preview(tr("Typography / platform default", "文字层级 / 平台默认字体")) {
        ZenlessText(tr("Ready for anything", "准备就绪"), style = ZenlessTextStyle.Title)
        ZenlessText(tr("Clear structure. Expressive details.", "清晰的结构，鲜明的细节。"), style = ZenlessTextStyle.Subtitle)
        ZenlessText(tr("Body text uses the platform default font.", "正文使用平台默认字体，支持中文与英文。"),style=ZenlessTextStyle.Body)
        ZenlessText(tr("Caption / supporting information", "注释 / 辅助信息"), style = ZenlessTextStyle.Caption)
        ZenlessMetric("026", unit = "%", change = "+12")
    }
    CodeExample("ZenlessOverlayHost {\n    ZenlessSurface(Modifier.fillMaxSize()) {\n        ZenlessText(${quote(tr("Hello", "你好"))})\n    }\n}", tr)
}

internal fun quote(value: String): String = buildString {
    append('"'); value.forEach { when (it) { '\\' -> append("\\\\"); '"' -> append("\\\""); '\n' -> append("\\n"); '\r' -> append("\\r"); '\t' -> append("\\t"); '$' -> append("\\$"); else -> append(it) } }; append('"')
}
internal fun buttonExample(text: String, tone: ZenlessTone, size: ZenlessSize, variant: ZenlessButtonVariant, enabled: Boolean, loading: Boolean, selected: Boolean, round: Boolean, leadingIcon: Boolean = false) =
    "ZenlessButton(\n    onClick = { /* handle action */ },\n    tone = ZenlessTone.${tone.name},\n    size = ZenlessSize.${size.name},\n    variant = ZenlessButtonVariant.${variant.name},\n    enabled = $enabled, loading = $loading,\n    selected = $selected, round = $round,\n" +
        (if(leadingIcon)"    leadingIcon = {\n        val ink = zenlessContentColor\n        Canvas(Modifier.fillMaxSize().padding(5.dp)) { drawCircle(ink) }\n    },\n" else "") +
        ") {\n    ZenlessText(${quote(text)})\n}"

@Composable private fun CallerIcon() {
    val ink=zenlessContentColor
    Canvas(Modifier.fillMaxSize().padding(5.dp)) { drawCircle(ink) }
}

@Composable private fun ButtonsDemo(tr: (String, String) -> String, record: (String) -> Unit) {
    var text by remember { mutableStateOf(tr("Continue", "继续")) }
    var tone by remember { mutableIntStateOf(1) }; var variant by remember { mutableIntStateOf(0) }
    val size = LocalExampleSize.current
    var enabled by remember { mutableStateOf(true) }; var loading by remember { mutableStateOf(false) }; var selected by remember { mutableStateOf(false) }; var round by remember { mutableStateOf(true) }
    var leadingIcon by remember { mutableStateOf(true) }
    Preview(tr("Live preview", "实时预览")) {
        Box(Modifier.fillMaxWidth().heightIn(min = 120.dp), contentAlignment = Alignment.Center) {
            ZenlessButton({ record(text) }, tone = ZenlessTone.entries[tone], size = size, variant = ZenlessButtonVariant.entries[variant], enabled = enabled, loading = loading, selected = selected, round = round, leadingIcon = if(leadingIcon)({CallerIcon()}) else null) { ZenlessText(text) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ZenlessBackButton({ record("Back") }, tr("Back", "返回")); ZenlessCloseButton({ record("Close") }, tr("Close", "关闭"))
            ZenlessIconButton({record("Icon")},tr("Icon button","图标按钮")) { CallerIcon() }
        }
    }
    Preview(tr("Enabled / disabled", "启用 / 禁用")) {
        FlowRow(horizontalArrangement=Arrangement.spacedBy(26.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            ZenlessButton({},Modifier.widthIn(min=244.dp),tone=ZenlessTone.Warning,enabled=false,leadingIcon={CallerIcon()}) { ZenlessText(tr("Unavailable","暂不可用")) }
            ZenlessButton({record(tr("Continue","继续操作"))},Modifier.widthIn(min=244.dp),tone=ZenlessTone.Danger,leadingIcon={CallerIcon()}) { ZenlessText(tr("Continue","继续操作")) }
        }
    }
    Preview(tr("Parameters", "参数")) {
        ZenlessTextField(text, { text = it }, label = tr("Label", "文字"))
        ZenlessSelect(ZenlessTone.entries.map { it.name }, tone, { tone = it }, label = "Tone")
        ZenlessSelect(ZenlessButtonVariant.entries.map { it.name }, variant, { variant = it }, label = "Variant")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ZenlessSwitch(enabled, { enabled = it }, label = tr("Enabled", "启用")); ZenlessSwitch(loading, { loading = it }, label = tr("Loading", "加载"))
            ZenlessSwitch(selected, { selected = it }, label = tr("Selected", "选中")); ZenlessSwitch(round, { round = it }, label = tr("Round", "圆角"))
            ZenlessSwitch(leadingIcon, {leadingIcon=it},label=tr("Leading icon","左侧图标"))
        }
    }
    CodeExample(buttonExample(text, ZenlessTone.entries[tone], size, ZenlessButtonVariant.entries[variant], enabled, loading, selected, round, leadingIcon), tr)
}

@Composable private fun NavigationDemo(tr: (String, String) -> String, record: (String) -> Unit) {
    var selected by remember { mutableIntStateOf(0) }; var folder by remember { mutableStateOf(true) }
    var expand by remember { mutableStateOf(true) }
    val labels = listOf(tr("Overview", "概览"), tr("Details", "详情"), tr("History", "历史"))
    Preview(tr("Tab styles", "页签样式")) {
        ZenlessTabs(labels, selected, { selected = it; record(labels[it]) }, Modifier.fillMaxWidth(), folder, expand)
        ZenlessSwitch(folder, { folder = it }, label = tr("Folder style", "文件夹样式"))
        if(!folder) ZenlessSwitch(expand, { expand = it }, label = tr("Scale pulse", "缩放呼吸"))
        ZenlessInfoRow(tr("Selected", "当前选中"), labels[selected])
    }
    CodeExample("var selected by remember { mutableIntStateOf($selected) }\nZenlessTabs(\n    items = listOf(${labels.joinToString { quote(it) }}),\n    selectedIndex = selected,\n    onSelected = { selected = it },\n    folder = $folder,\n    expand = $expand,\n)", tr)
}

@Composable private fun FieldsDemo(tr: (String, String) -> String, record: (String) -> Unit) {
    var value by remember { mutableStateOf("") }; var enabled by remember { mutableStateOf(true) }; var error by remember { mutableStateOf(false) }; var readOnly by remember { mutableStateOf(false) }; var multiline by remember { mutableStateOf(false) }
    val label = tr("Display name", "显示名称"); val placeholder = tr("Type here…", "在这里输入…"); val errorText = tr("Please check this value.", "请检查输入内容。")
    Preview(tr("Text input / IME / selection", "文字输入 / 输入法 / 选择")) {
        ZenlessTextField(value, { value = it; record("Text changed") }, label = label, placeholder = placeholder, enabled = enabled, readOnly = readOnly, error = if (error) errorText else null, singleLine = !multiline)
        ZenlessSwitch(enabled, { enabled = it }, label = tr("Enabled", "启用")); ZenlessSwitch(readOnly, { readOnly = it }, label = tr("Read only", "只读")); ZenlessSwitch(error, { error = it }, label = tr("Error", "错误")); ZenlessSwitch(multiline, { multiline = it }, label = tr("Multiline", "多行"))
    }
    CodeExample("var value by remember { mutableStateOf(${quote(value)}) }\nZenlessTextField(\n    value, { value = it },\n    label = ${quote(label)},\n    placeholder = ${quote(placeholder)},\n    enabled = $enabled, readOnly = $readOnly,\n    singleLine = ${!multiline},\n    error = ${if (error) quote(errorText) else "null"},\n)", tr)
}

@Composable private fun SelectDemo(tr: (String, String) -> String, record: (String) -> Unit) {
    var selected by remember { mutableIntStateOf(0) }; var value by remember { mutableFloatStateOf(.4f) }; var enabled by remember { mutableStateOf(true) }; var stepped by remember { mutableStateOf(false) }
    val options = listOf(tr("Automatic", "自动"), tr("Compact", "紧凑"), tr("Comfortable", "舒适"))
    Preview(tr("Selection & numeric input", "选择与数值输入")) {
        ZenlessSelect(options, selected, { selected = it; record(options[it]) }, enabled = enabled, label = tr("Layout", "布局"))
        ZenlessSlider(value, { value = it }, enabled = enabled, steps = if (stepped) 9 else 0, label = tr("Level", "等级"), onValueChangeFinished = { record("Slider finished") })
        ZenlessMetric((value * 100).roundToInt().toString(), unit = "%")
        ZenlessSwitch(enabled, { enabled = it }, label = tr("Enabled", "启用")); ZenlessSwitch(stepped, { stepped = it }, label = tr("Discrete steps", "分档"))
    }
    CodeExample("var selected by remember { mutableIntStateOf($selected) }\nvar level by remember { mutableFloatStateOf(${value}f) }\nZenlessSelect(listOf(${options.joinToString { quote(it) }}), selected, { selected = it }, enabled = $enabled)\nZenlessSlider(level, { level = it }, steps = ${if (stepped) 9 else 0}, enabled = $enabled)", tr)
}

@Composable private fun SelectionDemo(tr: (String, String) -> String, record: (String) -> Unit) {
    var state by remember { mutableStateOf(ToggleableState.Indeterminate) }; var radio by remember { mutableIntStateOf(0) }; var checked by remember { mutableStateOf(true) }; var enabled by remember { mutableStateOf(true) }
    val checkboxLabel = tr("Select all", "全选"); val switchLabel = tr("Notifications", "通知")
    Preview(tr("Controlled selection", "外部管理选中状态")) {
        ZenlessCheckbox(state, { state = it; record(it.name) }, enabled = enabled, label = checkboxLabel)
        ZenlessSelect(ToggleableState.entries.map { it.name }, state.ordinal, { state = ToggleableState.entries[it] }, label = tr("Checkbox state", "复选框状态"))
        Column(Modifier.selectableGroup()) { listOf(tr("Standard", "标准"), tr("Advanced", "高级")).forEachIndexed { i, label -> ZenlessRadioButton(radio == i, { radio = i; record(label) }, enabled = enabled, label = label) } }
        ZenlessSwitch(checked, { checked = it; record("Switch: $it") }, enabled = enabled, label = switchLabel)
        ZenlessSwitch(enabled, { enabled = it }, label = tr("Controls enabled", "启用控件"))
    }
    CodeExample("var state by remember { mutableStateOf(ToggleableState.${state.name}) }\nvar selected by remember { mutableIntStateOf($radio) }\nvar checked by remember { mutableStateOf($checked) }\nZenlessCheckbox(state, { state = it }, enabled = $enabled, label = ${quote(checkboxLabel)})\nColumn(Modifier.selectableGroup()) {\n    ZenlessRadioButton(selected == 0, { selected = 0 }, enabled = $enabled, label = ${quote(tr("Standard", "标准"))})\n    ZenlessRadioButton(selected == 1, { selected = 1 }, enabled = $enabled, label = ${quote(tr("Advanced", "高级"))})\n}\nZenlessSwitch(checked, { checked = it }, enabled = $enabled, label = ${quote(switchLabel)})", tr)
}

@Composable private fun DisplayDemo(tr: (String, String) -> String) {
    var value by remember { mutableFloatStateOf(.65f) }; var expanded by remember { mutableStateOf(true) }
    val title = tr("More information", "更多信息"); val body = tr("Cards share the same foundation as the rest of the library.", "卡片和其他组件使用同一套基础样式。")
    Preview(tr("Information & feedback", "信息与反馈")) {
        ZenlessBadge(tr("Ready", "就绪"), tone = ZenlessTone.Success)
        ZenlessProgress(value); ZenlessSlider(value, { value = it }, label = tr("Progress", "进度"))
        ZenlessMetric((value * 100).roundToInt().toString(), unit = "%", change = "+5")
        ZenlessInfoRow(tr("Status", "状态"), tr("Available", "可用"))
        ZenlessNotice(tr("Your changes have been saved.", "修改已保存。"), tone = ZenlessTone.Success)
        ZenlessCollapse(title, expanded, { expanded = it }) { ZenlessText(body) }
    }
    CodeExample("var expanded by remember { mutableStateOf($expanded) }\nZenlessCard {\n    ZenlessBadge(${quote(tr("Ready", "就绪"))}, tone = ZenlessTone.Success)\n    ZenlessProgress(${value}f)\n    ZenlessInfoRow(${quote(tr("Status", "状态"))}, ${quote(tr("Available", "可用"))})\n    ZenlessNotice(${quote(tr("Your changes have been saved.", "修改已保存。"))}, tone = ZenlessTone.Success)\n    ZenlessCollapse(${quote(title)}, expanded, { expanded = it }) {\n        ZenlessText(${quote(body)})\n    }\n}", tr)
}

@Composable private fun TooltipDemo(tr: (String, String) -> String) {
    var text by remember { mutableStateOf(tr("A little context, just when you need it.", "需要时出现的辅助说明。")) }
    val anchor = tr("Hover or long press", "悬停或长按")
    Preview(tr("Mouse & touch", "鼠标与触摸")) {
        Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
            ZenlessTooltip(text) { ZenlessBadge(anchor, tone = ZenlessTone.Info) }
        }
        ZenlessTextField(text, { text = it }, label = tr("Tooltip text", "提示内容"))
    }
    CodeExample("ZenlessTooltip(${quote(text)}) {\n    ZenlessBadge(${quote(anchor)}, tone = ZenlessTone.Info)\n}", tr)
}

@Composable private fun SettingsExample(tr: (String, String) -> String, record: (String) -> Unit, showCode: Boolean = false) {
    var name by remember { mutableStateOf("Proxy") }; var notifications by remember { mutableStateOf(true) }; var quality by remember { mutableIntStateOf(0) }; var level by remember { mutableFloatStateOf(.7f) }
    ZenlessCard(Modifier.fillMaxWidth()) {
        ZenlessText(tr("Profile & preferences", "资料与偏好"), style = ZenlessTextStyle.Subtitle)
        ZenlessTextField(name, { name = it }, label = tr("Display name", "显示名称"))
        ZenlessSwitch(notifications, { notifications = it }, label = tr("Notifications", "通知"))
        ZenlessSelect(listOf(tr("Balanced", "均衡"), tr("High", "高")), quality, { quality = it }, label = tr("Quality", "质量"))
        ZenlessSlider(level, { level = it }, label = tr("Intensity", "强度"))
        ZenlessButton({ record(tr("Preferences saved for $name", "已保存 $name 的偏好")) }, tone = ZenlessTone.Accent) { ZenlessText(tr("Save preferences", "保存偏好")) }
    }
    if (showCode) CodeExample("""
        var name by remember { mutableStateOf(${quote(name)}) }
        var notifications by remember { mutableStateOf($notifications) }
        var quality by remember { mutableIntStateOf($quality) }
        var level by remember { mutableFloatStateOf(${level}f) }
        ZenlessCard {
            ZenlessTextField(name, { name = it }, label = ${quote(tr("Display name", "显示名称"))})
            ZenlessSwitch(notifications, { notifications = it }, label = ${quote(tr("Notifications", "通知"))})
            ZenlessSelect(listOf(${quote(tr("Balanced", "均衡"))}, ${quote(tr("High", "高"))}), quality, { quality = it })
            ZenlessSlider(level, { level = it }, label = ${quote(tr("Intensity", "强度"))})
            ZenlessButton({ /* persist the values */ }, tone = ZenlessTone.Accent) {
                ZenlessText(${quote(tr("Save preferences", "保存偏好"))})
            }
        }
    """.trimIndent(), tr)
}

private fun alertExample(chinese: Boolean, message: String, allowCancel: Boolean): String = "var visible by remember { mutableStateOf(false) }\nZenlessButton({ visible = true }) { ZenlessText(${quote(if (chinese) "打开" else "Open")}) }\nZenlessAlert(\n    visible = visible,\n    message = ${quote(message)},\n    confirmText = ${quote(if (chinese) "应用" else "Apply")},\n    onConfirm = { visible = false },\n    cancelText = ${if (allowCancel) quote(if (chinese) "取消" else "Cancel") else "null"},\n    onDismissRequest = ${if (allowCancel) "{ visible = false }" else "null"},\n)"

private fun drawerExample(tr: (String, String) -> String): String = """
    var visible by remember { mutableStateOf(false) }
    ZenlessButton({ visible = true }) { ZenlessText(${quote(tr("Open drawer", "打开抽屉"))}) }
    ZenlessDrawer(
        visible, ${quote(tr("Preferences", "偏好设置"))}, ${quote(tr("Close", "关闭"))},
        onDismissRequest = { visible = false },
        footer = { ZenlessButton({ visible = false }) { ZenlessText(${quote(tr("Save", "保存"))}) } },
    ) {
        ZenlessText(${quote(tr("Your content goes here.", "在这里放置内容。"))})
    }
""".trimIndent()
