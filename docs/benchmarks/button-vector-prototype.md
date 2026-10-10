# Direct-vector button prototype, 2026-10-10

This records the initial **uncached** experiment. Those alternatives were not adopted at that stage: they reduced creation/resize work but changed calibrated pixels and increased forced redraw cost. After accepting the direct-vector appearance, the follow-up implementation added a native-resolution bitmap cache; see [the implementation results](button-select-optimization.md). The measurements below remain the original experiment.

## Implementations

The baseline is the production renderer at `237e99f9d5681f0c28b15c236cbf71059febde98`, frozen in the test-only `SupersampledButtonReference.kt`. Alternatives live in `zenless-ui/src/desktopTest/kotlin/moe/tlaster/zenlessui/ButtonVectorPrototype.kt`:

| Mode | Rendering |
| --- | --- |
| Baseline | Existing weighted face masks, 4x rasterization and two downsampling passes; final bitmap reused |
| NativeLayers | Same weighted masks and texture compositing at native resolution; no full-button bitmap cache |
| Direct | One native-antialiased path per face, directly filled with an opaque repeating texture; no mask layers or supersampling |
| DirectSoft | Four nested native-antialiased paths with cumulative soft-edge coverage encoded into opaque fills; no mask layers or supersampling |

The alternatives preserve the calibrated geometric coordinates, continuous circular junctions, mirrored faces, texture phase and outer bevel definition. `DirectSoft` is an exploratory approximation: overlapping native antialiasing is not mathematically identical to averaging coverage in a supersampled mask. Its nested face painting also introduces overdraw. These measurements do not establish a lower bound on every possible vector implementation.

## Visual comparison

The harness captures 144 shell images: four implementations, four densities (1, 1.5, 2, 3), and nine configurations. Configurations include all three control heights, the 244 x 58 dp reference, RTL, square corners, an untextured shell, a non-black fill and a narrow button. Text and business icons are omitted to isolate the shell.

Two comparisons are kept distinct:

- Full-image differences against the current renderer at identical dimensions/density, including maximum channel error and the fraction of pixels differing by more than one 8-bit channel level.
- Fixed upper/lower circular-junction regions against the supplied reference screenshot at its native scale. Existing rendering already has a small nonzero error against the capture; matching the current implementation and matching the capture are not identical criteria.

At the 488 x 116 px reference size:

| Mode | Upper join mean channel error vs reference | Lower join mean channel error vs reference | Pixels differing by >1 vs current full shell |
| --- | ---: | ---: | ---: |
| Baseline | 0.462 | 0.453 | 0% |
| NativeLayers | 0.523 | 0.533 | 1.97% |
| Direct | 1.015 | 1.040 | 3.05% |
| DirectSoft | 0.590 | 0.724 | 2.79% |

Errors are measured in 0-255 channel units. The source comparison uses 35 x 21 px regions at `(70,5)` and `(70,92)` relative to the cropped reference button. The direct path visibly hardens the circular junctions. Reintroducing soft coverage gets closer, but does not preserve all calibrated edges. The non-black `DirectSoft` fixture at density 2 differs from the current renderer by up to 56 channel levels; a small whole-image average would conceal this localized regression.

Original reference imagery and enlarged comparison sheets remain under ignored `verification/vector-prototype/repeat/`; they are not library assets. Numeric evidence is in [the pixel-difference CSV](button-vector-pixels.csv) and [reference-region CSV](button-vector-reference-regions.csv).

## Timing method

This is an isolated **CPU raster shell microbenchmark**, not the earlier full-control Gallery benchmark and not GPU/display FPS. Compare implementations within this run only.

- Windows 11, Ryzen 9 7950X3D, JDK 25.0.4.1, Compose 1.12.1, Skiko 0.150.1.
- `ImageComposeScene`, 1280 x 900 dp, densities 1 and 2, image snapshots closed after every render.
- Each renderer is primed before measurement. Every scenario gets 60 warm-up frames and 180 measured frames, repeated three times; renderer order reverses in round two.
- 48 stable shells, one continuously resized shell, and 12 continuously resized shells. All fit in the viewport. Height is 52 dp, width is 180 dp or cycles through 180-191 dp.
- 12,960 measured frames. First-render construction has only three samples per configuration and is exploratory.
- Stable scenes are deliberately redrawn to expose drawing cost. They do not animate or continuously request frames by themselves; this column is not idle CPU consumption.
- No icons, text, input handling, press animation or full application layout is included. CPU affinity and power mode are not pinned.

Timing results are recorded in [the frame aggregate](button-vector-timings.csv) and [per-trial summaries](button-vector-trials.csv).

At density 2, aggregating 540 measured frames per cell:

| Mode | Create 48 shells, median first render | Resize 1, P95 | Resize 12, P95 | Redraw 48 stable shells, P95 |
| --- | ---: | ---: | ---: | ---: |
| Baseline | 560.60 ms | 14.21 ms | 160.98 ms | 2.24 ms |
| NativeLayers | 44.27 ms | 2.63 ms | 11.82 ms | 41.70 ms |
| Direct | 20.86 ms | 1.78 ms | 5.37 ms | 17.25 ms |
| DirectSoft | 56.04 ms | 2.72 ms | 15.30 ms | 56.40 ms |

The full-button bitmap saves substantial work when the size is stable. Removing it trades creation cost for repeated path/texture rasterization; the direct soft-edge prototype additionally paints large face regions several times. Trial-level variability is included in the CSV rather than treating these measurements as device-independent thresholds.

## Decision

The initial decision was to keep the production implementation. The tested paths made width changes cheaper, but none demonstrated both preserved visual calibration and acceptable redraw cost. The prototype's completion was not a pixel-perfect acceptance result.

A follow-up should target reusable coverage of the complex local junction or investigate an analytically evaluated coverage shader. Merely removing the bitmap cache moves substantial work into repeated draws. GPU-presented measurements would be required before extrapolating the software-rendering regression to actual devices.

## Reproduce

```powershell
$env:ZENLESS_VECTOR_PROTOTYPE = '1'
$env:ZENLESS_VECTOR_RUN = 'local-vector'
$env:ZENLESS_VECTOR_MODES = 'Baseline,NativeLayers,Direct,DirectSoft'
$env:ZENLESS_VECTOR_BENCHMARK = '1'
.\gradlew.bat :zenless-ui:desktopTest --tests '*ButtonVectorPrototypeTest' --rerun
```

Omit `ZENLESS_VECTOR_BENCHMARK` to capture visual fixtures without timing. `ZENLESS_VECTOR_MODES` accepts comma-separated mode names; omit it to also include the new `CachedDirect` implementation. `ZENLESS_VECTOR_FRAMES`, `ZENLESS_VECTOR_WARMUP`, and `ZENLESS_VECTOR_ROUNDS` can shorten exploratory runs; the published repeat uses defaults. Ordinary test runs skip both opt-in experiment methods.

No full local platform build, GPU profiling, browser/mobile validation or CI wait was performed for this prototype. CI configuration is unchanged.
