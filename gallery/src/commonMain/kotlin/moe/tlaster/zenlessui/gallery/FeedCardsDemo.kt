package moe.tlaster.zenlessui.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import moe.tlaster.zenlessui.*

@Composable
internal fun FeedCardsDemo(tr: (String, String) -> String, record: (String) -> Unit) {
    var selected by remember { mutableIntStateOf(1) }
    var enabled by remember { mutableStateOf(true) }
    var showLabel by remember { mutableStateOf(true) }
    var showStatus by remember { mutableStateOf(true) }
    var longText by remember { mutableStateOf(false) }
    val ratios = listOf(1.6f, 1f, .75f)
    val author = tr("Signal studio", "信号工作室")
    val title = tr("A new story around the corner", "转过街角，发现新的故事")
    val summary = tr("Cover, avatar and all text belong to the caller.", "封面、头像与所有文字均由调用方提供。")
    val label = tr("FEATURED", "精选内容")
    val status = tr("Read", "已读")
    Preview(tr("Masonry preview", "瀑布流预览")) {
        ZenlessText(tr("Click to select. Hold or focus to highlight; hover stays quiet.", "点击选中；按住或获得焦点显示呼吸边框，悬停不高亮。"))
        LazyVerticalStaggeredGrid(columns = StaggeredGridCells.Adaptive(250.dp), modifier = Modifier.fillMaxWidth().height(620.dp).testTag("feed-grid"),
            horizontalArrangement = Arrangement.spacedBy(20.dp), verticalItemSpacing = 20.dp) {
            items(6, key = { it }) { index ->
                ZenlessFeedCard(
                    onClick = { selected = index; record(tr("Card ${index+1}", "卡片 ${index+1}")) },
                    coverAspectRatio = ratios[index % 3],
                    cover = {
                        Image(BrushPainter(Brush.linearGradient(listOf(Color(0xff30558c), Color(0xffa14b83), Color(0xffffb970)))),
                            contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.Center)
                    },
                    avatar = { Box(Modifier.fillMaxSize().background(Color(0xff949595)), contentAlignment = Alignment.Center) { ZenlessText("Z") } },
                    author = { ZenlessText(author, maxLines = 1) },
                    title = { ZenlessText(if (longText) "$title · $title · $title" else title) },
                    summary = { ZenlessText(summary, maxLines = 1) },
                    modifier = Modifier.fillMaxWidth().testTag("feed-card-$index"), selected = selected == index, enabled = enabled,
                    label = if (showLabel && index % 2 == 0) ({ ZenlessText(label, maxLines = 1) }) else null,
                    status = if (showStatus && index % 3 == 0) ({ ZenlessText(status, maxLines = 1) }) else null,
                )
            }
        }
    }
    Preview(tr("Parameters", "参数")) {
        ZenlessSwitch(enabled, { enabled = it }, label = tr("Cards enabled", "启用卡片"))
        ZenlessSwitch(showLabel, { showLabel = it }, label = tr("Category capsule", "分类胶囊"))
        ZenlessSwitch(showStatus, { showStatus = it }, label = tr("Status capsule", "状态胶囊"))
        ZenlessSwitch(longText, { longText = it }, label = tr("Unrestricted title", "长标题自然换行"))
        ZenlessButton({ selected = -1 }) { ZenlessText(tr("Clear selection", "清除选中")) }
    }
    val exampleIndex = selected.coerceAtLeast(0)
    CodeExample(feedCardExample(ratios[exampleIndex % 3], enabled, selected >= 0, showLabel && exampleIndex % 2 == 0, showStatus && exampleIndex % 3 == 0,
        author, if (longText) "$title · $title · $title" else title, summary, label, status), tr)
}

internal fun feedCardExample(ratio: Float, enabled: Boolean, selected: Boolean, showLabel: Boolean, showStatus: Boolean,
    author: String, title: String, summary: String, label: String, status: String): String = """
    // Image, BrushPainter, Brush, Color, ContentScale and Alignment are Compose APIs.
    var selected by remember { mutableStateOf($selected) }
    ZenlessFeedCard(
        onClick = { selected = true },
        modifier = Modifier.width(320.dp),
        coverAspectRatio = ${ratio}f,
        selected = selected,
        enabled = $enabled,
        cover = {
            Image(
                painter = BrushPainter(Brush.linearGradient(listOf(Color(0xff30558c), Color(0xffa14b83), Color(0xffffb970)))),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
            )
        },
        avatar = {
            Box(Modifier.fillMaxSize().background(Color(0xff949595)), contentAlignment = Alignment.Center) {
                ZenlessText("Z")
            }
        },
        author = { ZenlessText(${quote(author)}, maxLines = 1) },
        title = { ZenlessText(${quote(title)}) },
        summary = { ZenlessText(${quote(summary)}, maxLines = 1) },
        label = ${if (showLabel) "{ ZenlessText(${quote(label)}, maxLines = 1) }" else "null"},
        status = ${if (showStatus) "{ ZenlessText(${quote(status)}, maxLines = 1) }" else "null"},
    )
""".trimIndent()
