package moe.tlaster.zenlessui.gallery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.sun.management.ThreadMXBean
import java.io.File
import java.lang.management.ManagementFactory
import java.time.Duration
import jdk.jfr.Configuration
import jdk.jfr.Recording
import moe.tlaster.zenlessui.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Opt-in CPU raster benchmark. It does not measure GPU presentation or display FPS. */
@OptIn(ExperimentalComposeUiApi::class)
class PerformanceBenchmark {
    @Test fun renderScenarios() {
        assumeTrue("Set ZENLESS_BENCHMARK=1 to run", System.getenv("ZENLESS_BENCHMARK") == "1")
        val frames = System.getenv("ZENLESS_BENCHMARK_FRAMES")?.toInt() ?: 180
        val rounds = System.getenv("ZENLESS_BENCHMARK_ROUNDS")?.toInt() ?: 3
        val warmup = System.getenv("ZENLESS_BENCHMARK_WARMUP")?.toInt() ?: 60
        require(frames > 0 && rounds > 0 && warmup > 0)
        val output = File("../verification/performance/${System.getenv("ZENLESS_BENCHMARK_RUN") ?: "latest"}").apply { mkdirs() }
        val bean = ManagementFactory.getThreadMXBean() as ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported)
        bean.isThreadAllocatedMemoryEnabled = true
        val thread = Thread.currentThread().id
        val collectors = ManagementFactory.getGarbageCollectorMXBeans()
        val recording = if (System.getenv("ZENLESS_BENCHMARK_PROFILE") == "1") Recording(Configuration.getConfiguration("profile")).apply {
            enable("jdk.ExecutionSample").withPeriod(Duration.ofMillis(5))
            enable("jdk.NativeMethodSample").withPeriod(Duration.ofMillis(5))
            start()
        } else null
        val cases = listOf("empty", "plain-48", "leading-48", "selected-plain-12", "selected-leading-12",
            "held-plain", "held-leading", "loading-12", "tabs-12", "folder-tabs-12", "mixed", "alert-cycle",
            "resize-leading-1", "resize-leading-6", "resize-leading-12", "switch-cycle-12", "select-100-open", "select-1000-open", "gallery")
        val selectedCases = System.getenv("ZENLESS_BENCHMARK_CASES")?.split(',') ?: cases
        require(selectedCases.all { it in cases })
        val densities = System.getenv("ZENLESS_BENCHMARK_DENSITIES")?.split(',')?.map { it.toFloat() } ?: listOf(1f, 2f)
        require(densities.all { it > 0f && it <= 2f })
        File(output, "environment.txt").writeText("pid=${ProcessHandle.current().pid()}\njava=${System.getProperty("java.runtime.version")}\nos=${System.getProperty("os.name")} ${System.getProperty("os.version")}\nprocessors=${Runtime.getRuntime().availableProcessors()}\nmaxHeap=${Runtime.getRuntime().maxMemory()}\nviewport=1280x900 dp\nrenderer=ImageComposeScene / Skia CPU raster\nframes=$frames; warmup=$warmup; rounds=$rounds\n")
        try {
            File(output, "frames.csv").bufferedWriter().use { raw ->
                File(output, "summary.csv").bufferedWriter().use { summary ->
                    raw.appendLine("scenario,density,round,frame,ms,java_bytes,pending")
                    summary.appendLine("scenario,density,round,mount_ms,interaction_peak_ms,median_ms,p95_ms,p99_ms,max_ms,java_kib_per_frame,pending_percent,over_16_67_percent,gc_count,gc_ms,heap_mib")
                    // Prime font/native loading before recording scene construction costs.
                    val prime = ImageComposeScene(320, 120) { ZenlessTheme { ZenlessButton({}) { ZenlessText("Warm up") } } }
                    repeat(60) { prime.render(it * 16_666_667L).close() }; prime.close()
                    for (round in 1..rounds) for (density in densities) for (name in if(round % 2 == 0) selectedCases.reversed() else selectedCases) {
                        val tick = mutableIntStateOf(0)
                        var time = 0L
                        val mountStart = System.nanoTime()
                        val scene = ImageComposeScene((1280*density).toInt(), (900*density).toInt(), Density(density)) {
                            ZenlessTheme { Scenario(name, tick) }
                        }
                        try {
                            scene.render(time).close()
                            val mountMs = (System.nanoTime()-mountStart)/1e6
                            if (name.startsWith("held-")) scene.sendPointerEvent(PointerEventType.Press, Offset(100*density,42*density))
                            var interactionMs = 0.0
                            if (name.startsWith("select-")) {
                                val start=System.nanoTime()
                                scene.sendPointerEvent(PointerEventType.Press,Offset(100*density,42*density))
                                scene.sendPointerEvent(PointerEventType.Release,Offset(100*density,42*density))
                                interactionMs=(System.nanoTime()-start)/1e6
                                // Popup composition can be deferred beyond the first frame after release.
                                repeat(30) {
                                    val frameStart=System.nanoTime()
                                    time += 16_666_667L
                                    scene.render(time).close()
                                    interactionMs=maxOf(interactionMs,(System.nanoTime()-frameStart)/1e6)
                                }
                            }
                            fun frame(index: Int) {
                                if (name.startsWith("resize-") || name == "alert-cycle") tick.intValue=index
                                if (name.startsWith("switch-cycle")) tick.intValue=index/30
                                time += 16_666_667L
                                scene.render(time).close()
                            }
                            repeat(warmup) { frame(it) }
                            val timings = DoubleArray(frames)
                            val allocations = LongArray(frames)
                            val pending = BooleanArray(frames)
                            val gcCount = collectors.sumOf { it.collectionCount }
                            val gcTime = collectors.sumOf { it.collectionTime }
                            repeat(frames) { index ->
                                val before = bean.getThreadAllocatedBytes(thread)
                                val start = System.nanoTime()
                                frame(index + warmup)
                                timings[index] = (System.nanoTime()-start)/1e6
                                allocations[index] = bean.getThreadAllocatedBytes(thread)-before
                                pending[index] = scene.hasInvalidations()
                            }
                            val sorted = timings.sorted()
                            fun percentile(p: Double) = sorted[((frames-1)*p).toInt()]
                            for (index in timings.indices) raw.appendLine("$name,$density,$round,$index,${timings[index]},${allocations[index]},${pending[index]}")
                            val row = "$name,$density,$round,$mountMs,$interactionMs,${percentile(.5)},${percentile(.95)},${percentile(.99)},${sorted.last()},${allocations.average()/1024},${pending.count { it }*100.0/frames},${timings.count { it>16.666667 }*100.0/frames},${collectors.sumOf { it.collectionCount }-gcCount},${collectors.sumOf { it.collectionTime }-gcTime},${ManagementFactory.getMemoryMXBean().heapMemoryUsage.used/1048576.0}"
                            summary.appendLine(row); summary.flush(); raw.flush()
                            println("BENCH $row")
                            if (round == 1 && density == 1f) scene.render(time+16_666_667L).use { image ->
                                image.encodeToData()?.use { File(output, "$name.png").writeBytes(it.bytes) }
                            }
                        } finally { scene.close() }
                    }
                }
            }
        } finally {
            recording?.stop()
            recording?.dump(File(output, "profile.jfr").toPath())
            recording?.close()
        }
    }

    @Composable private fun Scenario(name: String, tick: State<Int>) {
        if (name == "gallery") { GalleryApp(); return }
        ZenlessOverlayHost {
            Box(Modifier.fillMaxSize().background(Color(0xff101012))) {
                when (name) {
                    "empty" -> Unit
                    "mixed", "alert-cycle" -> Row(Modifier.padding(16.dp), horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                        repeat(3) { Column(Modifier.width(384.dp), verticalArrangement=Arrangement.spacedBy(16.dp)) {
                            ZenlessButton({},leadingIcon={ Glyph() }) { ZenlessText("Apply changes") }
                            ZenlessTextField("Example",{})
                            ZenlessSelect(listOf("Balanced","Quality"),0,{})
                            ZenlessSwitch(true,{},label="Notifications")
                            ZenlessCheckbox(ToggleableState.On,{},label="Select all")
                            ZenlessRadioButton(true,{},label="Default")
                            ZenlessSlider(.5f,{})
                            ZenlessTabs(listOf("One","Two"),0,{})
                            ZenlessInfoRow("State","Ready")
                            ZenlessProgress(.6f)
                        } }
                    }
                    "tabs-12", "folder-tabs-12" -> Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                        repeat(4) { Row(horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                            repeat(3) { ZenlessTabs(listOf("One","Two","Three"),0,{},Modifier.width(384.dp),folder=name.startsWith("folder")) }
                        } }
                    }
                    "select-100-open", "select-1000-open" -> Box(Modifier.padding(16.dp)) {
                        val count=if(name=="select-100-open")100 else 1000
                        val options=remember { List(count) { "Option $it" } }
                        ZenlessSelect(options,0,{},Modifier.width(384.dp))
                    }
                    else -> {
                        val count = name.substringAfterLast('-').toIntOrNull() ?: 1
                        val leading = "leading" in name
                        val width = if(name.startsWith("resize")) 180 + tick.value % 12 else 180
                        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                            repeat((count+5)/6) { row -> Row(horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                                repeat(minOf(6,count-row*6)) {
                                    if(name.startsWith("switch-cycle")) ZenlessSwitch(tick.value%2==0,{},Modifier.width(width.dp))
                                    else ZenlessButton({},Modifier.width(width.dp),selected=name.startsWith("selected"),loading=name.startsWith("loading"),leadingIcon=if(leading){{ Glyph() }}else null) { ZenlessText("Action") }
                                }
                            } }
                        }
                    }
                }
            }
            if (name == "alert-cycle") ZenlessAlert(tick.value % 120 < 60,"Apply these changes?","Apply",{}, {},"Cancel")
        }
    }

    @Composable private fun Glyph() {
        val ink=zenlessContentColor
        Canvas(Modifier.fillMaxSize().padding(5.dp)) { drawCircle(ink) }
    }
}
