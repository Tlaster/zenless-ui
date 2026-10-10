# Native-resolution button cache and lazy selects

Leading-icon buttons now render the accepted direct-vector geometry into a single native-resolution bitmap when their appearance or size changes. Normal redraws reuse that bitmap. The 4x and 2x intermediate images, weighted soft-face passes and offscreen mask layers are removed. Buttons without a leading icon retain their existing drawing path. Public APIs, label positioning, preset colors and press/release motion are unchanged.

This intentionally adopts the direct-vector appearance from the earlier prototype. Its inner-circle edge is sharper than the supersampled version; it is not a claim of byte-identical rendering against the original screenshot. `ButtonCacheTest` compares the cached renderer against the accepted uncached vector at densities 1, 1.5, 2 and 3, including changing width, all three heights, color, RTL, corners and texture. Across 40 transitions the allowed channel difference is one rounding level.

For a 180 x 52 dp button at density 2, the old 4x/2x/final RGBA pixel buffers totaled about 3 MiB per reconstruction; the new final buffer is about 146 KiB. This is a reduction in transient pixel-buffer allocation, not a measurement of total process memory or the persistent final cache, which was already native-sized. No unbounded global cache is introduced.

`ZenlessSelect` now uses `LazyColumn` with index-based items and a bounded viewport. Opening starts at the selected index, and external selection, size or option-count changes reposition it by item rather than an estimated pixel offset. Small menus shrink to their contents. The existing border, row styling, five-row size cap, scroll hint, popup positioning and exit animation remain. Duplicate labels are supported because labels are not used as unique item keys.

## Fresh before/after measurements

Both runs use the existing full-control `PerformanceBenchmark` harness. The before build is `237e99f9d5681f0c28b15c236cbf71059febde98`; the after build contains the `Drawing.kt` and `Overlays.kt` changes in this delivery. These values must not be compared directly with the shell-only vector prototype.

- Windows 11, Ryzen 9 7950X3D, JDK 25.0.4.1, Compose 1.12.1, Skiko 0.150.1, 512 MiB Java heap.
- CPU raster `ImageComposeScene`; viewport 1280 x 900 dp, densities 1 and 2. No GPU presentation or physical-device FPS is measured.
- Six relevant scenarios, three trials per density, 60 warm-up and 180 measured frames per trial: 6,480 measured frames per build, 12,960 total.
- Scenario order reverses in trial two. CPU affinity and power mode are not pinned. The CSV includes trial ranges, since transient timings vary.
- Construction and popup-opening medians each have three samples. Opening measures the maximum operation during the first 30 frames after the click, including deferred popup composition. Frame P95 pools 540 measured frames per scenario/density/build.

At density 2:

| Measurement | Before | After |
| --- | ---: | ---: |
| Create 48 leading-icon buttons, median | 581.02 ms | 29.60 ms |
| Resize one leading-icon button, P95 | 15.35 ms | 1.90 ms |
| Resize 12 leading-icon buttons, P95 | 201.60 ms | 7.37 ms |
| Redraw 48 stable leading-icon buttons, P95 | 2.62 ms | 2.36 ms |
| Hold one leading-icon button, P95 | 1.10 ms | 0.89 ms |
| Open 100-option select, median opening peak | 12.87 ms | 5.64 ms |
| Open 1,000-option select, median opening peak | 88.28 ms | 4.83 ms |
| Redraw open 1,000-option select, P95 | 11.21 ms | 1.45 ms |

The important result is cheaper construction/resizing **without the uncached prototype's stable-redraw regression**. Stable buttons still stop requesting frames; the redraw measurement forces rendering and is not idle CPU consumption. The select's breathing animation continues intentionally.

Data: [aggregate results](button-select-optimized.csv), [per-trial summaries](button-select-trials.csv). Raw frames, environment metadata and screenshots are under ignored `verification/performance/before-button-select/` and `after-button-select/`.

## Validation

- Button cache equivalence while size, density, color and layout direction change.
- Existing button contour, enabled/disabled ink, typography, centering, press/release behavior and Alert reuse checks.
- Select initial composition touches fewer than 40 distinct options in a 1,000-item list starting at index 995; it never reads index 0 on opening. This is a bounded-work regression check, not an exact prefetch-count contract.
- Mouse wheel and touch scrolling, dismissal/reopening, external selection changes, duplicate labels, shrinking to a one-item menu, disabling while open, bottom-anchor placement and enlarged-text selected-row visibility.
- Existing arrow and size-preset checks. No API, dependency or CI configuration changes.

Only relevant local desktop tests and the focused benchmark were run. Full platform builds remain in CI; this report does not claim GPU, browser or mobile-device verification.

```powershell
$env:ZENLESS_BENCHMARK = '1'
$env:ZENLESS_BENCHMARK_RUN = 'local-button-select'
$env:ZENLESS_BENCHMARK_CASES = 'leading-48,held-leading,resize-leading-1,resize-leading-12,select-100-open,select-1000-open'
.\gradlew.bat :gallery:desktopTest --tests '*PerformanceBenchmark' --rerun
```
