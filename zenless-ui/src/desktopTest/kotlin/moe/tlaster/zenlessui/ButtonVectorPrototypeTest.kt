package moe.tlaster.zenlessui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Exploratory visual/timing comparison, deliberately not a pixel-perfect acceptance test. */
@OptIn(ExperimentalComposeUiApi::class)
class ButtonVectorPrototypeTest {
    private fun enabled() = assumeTrue("Set ZENLESS_VECTOR_PROTOTYPE=1 to run", System.getenv("ZENLESS_VECTOR_PROTOTYPE") == "1")
    private fun output() = File("../verification/vector-prototype/${System.getenv("ZENLESS_VECTOR_RUN") ?: "latest"}").apply { mkdirs() }
    private fun modes() = System.getenv("ZENLESS_VECTOR_MODES")?.split(',')?.map(ButtonPrototype::valueOf) ?: ButtonPrototype.entries

    @Composable private fun Shell(mode: ButtonPrototype, width: Int, height: Int, fill: Color = Color.Black, round: Boolean = true, pattern: Boolean = true) {
        Box(Modifier.size(width.dp, height.dp).background(Color.Black).drawWithCache {
            val draw = prototypeShell(mode, fill, Color(0xff262626), round, pattern)
            onDrawBehind { draw() }
        })
    }

    @Test fun captureVariants() {
        enabled()
        val output = output()
        for (density in listOf(1f, 1.5f, 2f, 3f)) for (mode in modes()) {
            for ((name, height) in listOf("compact" to 40, "default" to 52, "comfortable" to 62, "reference" to 58)) {
                val scene = ImageComposeScene((244 * density).toInt(), (height * density).toInt(), Density(density)) { Shell(mode, 244, height) }
                try { scene.render(0).use { image -> image.encodeToData()!!.use { File(output, "$name-$density-$mode.png").writeBytes(it.bytes) } } }
                finally { scene.close() }
            }
            for (name in listOf("rtl", "square", "plain", "color", "narrow")) {
                val width = if (name == "narrow") 110 else 244
                val scene = ImageComposeScene((width * density).toInt(), (52 * density).toInt(), Density(density)) {
                    CompositionLocalProvider(LocalLayoutDirection provides if (name == "rtl") LayoutDirection.Rtl else LayoutDirection.Ltr) {
                        Shell(mode, width, 52, fill = if (name == "color") Color(0xff999999) else Color.Black, round = name != "square", pattern = name != "plain")
                    }
                }
                try { scene.render(0).use { image -> image.encodeToData()!!.use { File(output, "$name-$density-$mode.png").writeBytes(it.bytes) } } }
                finally { scene.close() }
            }
        }
        // Compare full shells, so broad dark interiors cannot hide a handful of bad edge pixels.
        File(output, "pixel-differences.csv").bufferedWriter().use { csv ->
            csv.appendLine("case,mode,mean_channel_error,max_channel_error,pixels_over_1,percent_over_1")
            output.listFiles()!!.filter { it.name.endsWith("-Baseline.png") }.sortedBy { it.name }.forEach { file ->
                val baseline = ImageIO.read(file)
                for (mode in modes().filter { it != ButtonPrototype.Baseline }) {
                    val candidate = ImageIO.read(File(output, file.name.replace("-Baseline.png", "-$mode.png")))
                    check(baseline.width == candidate.width && baseline.height == candidate.height)
                    var error = 0L; var maximum = 0; var changed = 0
                    for (y in 0 until baseline.height) for (x in 0 until baseline.width) {
                        val a = baseline.getRGB(x, y); val b = candidate.getRGB(x, y)
                        var pixelMaximum = 0
                        check(a ushr 24 == 255 && b ushr 24 == 255) { "Shell fixture must have an opaque backdrop" }
                        for (shift in 0..16 step 8) {
                            val delta = kotlin.math.abs((a ushr shift and 255) - (b ushr shift and 255))
                            error += delta; pixelMaximum = maxOf(pixelMaximum, delta)
                        }
                        maximum = maxOf(maximum, pixelMaximum)
                        if (pixelMaximum > 1) changed++
                    }
                    val pixels = baseline.width * baseline.height
                    csv.appendLine("${file.name.removeSuffix("-Baseline.png")},$mode,${error / (pixels * 3.0)},$maximum,$changed,${changed * 100.0 / pixels}")
                }
            }
        }
    }

    @Test fun benchmarkShells() {
        enabled()
        assumeTrue("Set ZENLESS_VECTOR_BENCHMARK=1 to time", System.getenv("ZENLESS_VECTOR_BENCHMARK") == "1")
        val output = output()
        val frames = System.getenv("ZENLESS_VECTOR_FRAMES")?.toInt() ?: 180
        val warmup = System.getenv("ZENLESS_VECTOR_WARMUP")?.toInt() ?: 60
        val rounds = System.getenv("ZENLESS_VECTOR_ROUNDS")?.toInt() ?: 3
        require(frames > 0 && warmup > 0 && rounds > 0)
        File(output, "environment.txt").writeText("java=${System.getProperty("java.runtime.version")}\nos=${System.getProperty("os.name")}\nrenderer=ImageComposeScene CPU raster, shells only\nframes=$frames; warmup=$warmup; rounds=$rounds\n")
        // Prime native initialization and each renderer before measuring first-frame construction.
        for (mode in modes()) {
            val prime = ImageComposeScene(360, 104, Density(2f)) { Shell(mode, 180, 52) }
            try { repeat(60) { prime.render(it * 16_666_667L).close() } } finally { prime.close() }
        }
        File(output, "frames.csv").bufferedWriter().use { raw ->
            File(output, "summary.csv").bufferedWriter().use { summary ->
                raw.appendLine("mode,scenario,density,round,frame,ms")
                summary.appendLine("mode,scenario,density,round,mount_ms,median_ms,p95_ms,p99_ms")
                for (round in 1..rounds) for (density in listOf(1f, 2f)) for (scenario in listOf("idle-48", "resize-1", "resize-12")) {
                    for (mode in if (round % 2 == 0) modes().reversed() else modes()) {
                        val tick = mutableIntStateOf(0)
                        val count = scenario.substringAfter('-').toInt()
                        val start = System.nanoTime()
                        val scene = ImageComposeScene((1280 * density).toInt(), (900 * density).toInt(), Density(density)) {
                            Column(Modifier.fillMaxSize().background(Color(0xff101012)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                repeat((count + 5) / 6) { row -> Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                    repeat(minOf(6, count - row * 6)) { Shell(mode, if (scenario.startsWith("resize")) 180 + tick.intValue % 12 else 180, 52) }
                                } }
                            }
                        }
                        try {
                            scene.render(0).close()
                            val mount = (System.nanoTime() - start) / 1e6
                            fun render(index: Int) {
                                if (scenario.startsWith("resize")) tick.intValue = index
                                scene.render((index + 1) * 16_666_667L).close()
                            }
                            repeat(warmup) { render(it) }
                            val times = DoubleArray(frames) { index ->
                                val before = System.nanoTime()
                                render(index + warmup)
                                (System.nanoTime() - before) / 1e6
                            }
                            times.forEachIndexed { index, ms -> raw.appendLine("$mode,$scenario,$density,$round,$index,$ms") }
                            val sorted = times.sorted()
                            fun percentile(p: Double) = sorted[((frames - 1) * p).toInt()]
                            val result = "$mode,$scenario,$density,$round,$mount,${percentile(.5)},${percentile(.95)},${percentile(.99)}"
                            summary.appendLine(result); summary.flush(); raw.flush()
                            println("VECTOR $result")
                        } finally { scene.close() }
                    }
                }
            }
        }
    }
}
