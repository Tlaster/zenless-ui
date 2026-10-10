# Performance measurements

The desktop benchmark is opt-in and does not change component behavior. Run it on an otherwise idle machine:

```powershell
$env:ZENLESS_BENCHMARK = '1'
$env:ZENLESS_BENCHMARK_RUN = 'local'
.\gradlew.bat :gallery:desktopTest --tests '*PerformanceBenchmark' --rerun
```

Results and scene screenshots are written to `verification/performance/local/`. Ordinary CI runs skip this benchmark; machine-specific timing is not a pass/fail threshold.

## Method

- `ImageComposeScene` renders into a Skia CPU raster surface, including Compose frame-clock processing, recomposition, measurement, drawing and an immediately closed image snapshot. This is **not GPU presentation time or observed display FPS**.
- The viewport is 1280 × 900 logical pixels, tested at density 1 and 2 (1280 × 900 and 2560 × 1800 physical pixels).
- Each scene gets 60 warm-up frames and 180 measured frames, repeated three times. Scene order reverses in the second round to reduce ordering bias. The animation clock advances by 16.667 ms per frame; wall-clock render time is measured separately.
- Timings include forced drawing of static scenes. `pending_percent` records whether the scene requests another frame afterward. A static scene with zero pending frames does not continuously incur its forced-render cost in a real application.
- `mount_ms` includes scene creation and the first render after priming basic font/native loading. With only three mounts per density, it is an exploratory startup measurement, not a robust cold-start distribution.
- `interaction_peak_ms` measures the longest pointer-event or render operation during the first 30 frames after opening a select. It includes deferred popup composition and layout, rather than timing only the click handler.
- Allocation figures come from the benchmark thread's `ThreadMXBean` counter. They cover Java allocations on that thread, **not native Skia images, GPU memory, other threads or retained memory**. GC counters are process-wide. The benchmark is not a memory-leak test.
- The 16.667 ms column is a reference budget comparison for this CPU workload, not a measured dropped-frame percentage.

The suite covers idle and selected buttons, a sustained press, loading indicators, both tab styles, a mixed settings page, Alert opening/closing, continuous leading-button width changes, switch transitions, selects with 100/1000 options, and the actual Gallery landing page. All 48-button cases fit in the viewport. Resizing cycles widths from 180 to 191 dp so all 12 buttons remain visible.

For a focused repeat, set `ZENLESS_BENCHMARK_CASES` to comma-separated scenario names and `ZENLESS_BENCHMARK_DENSITIES` to `1`, `2` or `1,2`. `ZENLESS_BENCHMARK_FRAMES`, `ZENLESS_BENCHMARK_WARMUP` and `ZENLESS_BENCHMARK_ROUNDS` change sampling duration. Optional `ZENLESS_BENCHMARK_PROFILE=1` records Java Flight Recorder samples; profile runs are kept separate from the reported timing baseline.


## 2026-10-10 baseline: b70bc02

Library revision: `b70bc028ee9e4415e73d5cf8b4083e68d00c186d`. Windows 11, Ryzen 9 7950X3D (16 cores / 32 threads), 64 GiB RAM, Microsoft OpenJDK 25.0.4.1, Kotlin 2.4.10, Compose 1.12.1, Skiko 0.150.1. The installed RTX 4090 was **not used by this raster benchmark**. The test worker had a 512 MiB maximum Java heap. CPU affinity and power mode were not pinned; the three trial ranges are included in the data.

The final dataset contains 19 scenarios × 2 densities × 3 rounds × 180 frames = **20,520 measured frames**, excluding warm-up and the separate JFR run. Full scenario summaries are in [the CSV](benchmarks/b70bc02-windows-cpu.csv).

| Scene | P50, density 1 | P95, density 1 | P50, density 2 | P95, density 2 |
| --- | ---: | ---: | ---: | ---: |
| Gallery landing page | 3.42 ms | 3.71 ms | 9.68 ms | 10.71 ms |
| Mixed settings page, 30 controls | 2.42 ms | 3.45 ms | 7.10 ms | 10.11 ms |
| One held leading-icon button | 0.20 ms | 0.30 ms | 0.57 ms | 0.87 ms |
| 12 animated tab groups | 3.61 ms | 6.07 ms | 10.52 ms | 15.10 ms |
| One leading button changing width | 4.19 ms | 5.16 ms | 16.36 ms | 19.70 ms |
| Six leading buttons changing width | 20.26 ms | 25.77 ms | 92.62 ms | 106.25 ms |
| 12 leading buttons changing width | 39.26 ms | 62.40 ms | 186.65 ms | 223.99 ms |
| Alert opening/closing over settings | 4.08 ms | 47.11 ms | 9.36 ms | 133.97 ms |

### Findings and priorities

1. **Leading-button cache construction is the clearest bottleneck.** Creating 48 leading buttons took a median 176 ms at density 1 and 567 ms at density 2 across three mounts; ordinary buttons took 12 / 22 ms. Once constructed, the 48 cached buttons rendered at P95 0.96 / 2.53 ms and stopped requesting frames while idle. A sustained press on one leading button stayed below P95 0.88 ms at density 2. The expensive path is creation/size invalidation, not a cache rebuild on every held frame. `Drawing.kt` builds a bitmap at four times the physical width and height, then downsamples twice. JFR on a separate resize run sampled `buttonShell`/`maskedFace`, Skia rectangle/path rasterization and image downsampling. Prioritize reducing the region that needs supersampling and reusing unchanged coverage during resizing, while preserving the calibrated circular joins.
2. **Repeated cache rebuilds produce substantial process-memory pressure.** During the focused stress run, the Windows process peak working set reached 4.41 GiB; sampled private memory reached 4.39 GiB. End-of-scene Java heap samples were roughly 20–40 MiB, so the Java allocation column does not describe the process footprint. Repeated large native bitmap construction is consistent with this gap, but the process totals do not attribute every byte to a specific allocator. Working set subsequently dropped; these measurements do not establish a retained-memory leak. For scale, the three RGBA8888 pixel buffers for one 180 × 52 dp leading button at density 2 total about 3 MiB (4×, 2× and final resolution), before temporary layers and renderer bookkeeping.
3. **Large selects have an opening spike.** For 100 options, the median of each trial's worst opening frame was 10.2 / 11.1 ms at densities 1 / 2; for 1000 options it was 120.7 / 84.5 ms. The density ordering is not a scaling law: this is a small set of transient samples affected by JIT and GC. `ZenlessSelect` eagerly composes all options with `Column` and `forEachIndexed`, although only five rows fit. A lazy option list is the next targeted improvement, preserving selected-row scrolling and existing motion.
4. **Alert blur is expensive in software rendering.** P95 was 47 / 134 ms over the open/close cycle. This scene blurs the live settings background and includes animated modal artwork; the measurement does not isolate blur from the rest of the transition. The full-surface `BlurEffect` is a likely contributor, but GPU-presented desktop/Web and mobile profiling is required before treating these figures as production frame times or changing the visual design.
5. **Ordinary interactions do not show the same sustained bottleneck here.** The Gallery's density-2 trial P95 values were 10.25–11.35 ms, the mixed settings page was 7.25–11.02 ms, and held buttons were around 1 ms or less. Idle buttons and settled folder tabs requested no further frames. Animated Gallery/tab scenes did continue requesting frames, consistent with their breathing/loading effects. The mixed page allocated about 114 KiB per forced frame on the benchmark thread (about 6.7 MiB/s at 60 updates/s); this is a measured optimization opportunity, not by itself evidence of a leak.

Source locations: [button cache](../zenless-ui/src/commonMain/kotlin/moe/tlaster/zenlessui/Drawing.kt), [select options and blur host](../zenless-ui/src/commonMain/kotlin/moe/tlaster/zenlessui/Overlays.kt), [benchmark harness](../gallery/src/desktopTest/kotlin/moe/tlaster/zenlessui/gallery/PerformanceBenchmark.kt).

### Evidence and limits

Raw frames, environment metadata and screenshots remain under ignored `verification/performance/`. `baseline-b70bc02` supplies the ordinary scenes; `focused-b70bc02` replaces the resize, switch and select cases after tightening their measurement method (keep every resized button in the viewport, mutate switch state only at transitions, and include deferred popup creation in the opening window). Those superseded samples are excluded from the CSV. `profile-resize-b70bc02/profile.jfr` is a separate diagnostic run and is excluded from all reported timing percentiles. `focused-b70bc02/process-memory.csv` records the process-memory samples; the OS peak counter also covers periods before sampling began.

This task added measurement code and documentation only. No production UI behavior was changed. Actual GPU presentation, browser/Wasm performance, Android/iOS devices, macOS/Linux hosts, energy use and long-duration leak behavior remain unmeasured. Full platform builds were not needed for this local benchmark.
