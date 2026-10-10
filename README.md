# zenless-ui

A fixed dark-theme UI library for Compose Multiplatform, with a Chinese / English interactive gallery. Layered plates, geometric tabs, a yellow-green selection pulse, release flashes, and full-width alerts.

[Web gallery](https://tlaster.github.io/zenless-ui/) · [UI checks](https://github.com/Tlaster/zenless-ui/actions/workflows/gallery.yml) · [Platform builds](https://github.com/Tlaster/zenless-ui/actions/workflows/verify.yml) · [中文说明](docs/README.zh-CN.md)

Version **0.1.0**. Maven Central publication is configured; the first upload requires the repository owner's Central namespace, token and signing key. Do not assume the coordinate is available until a successful Publish workflow is linked in a release.

## Components

- Theme, text, surface, button, icon content slot, back and close buttons, spinner
- Horizontal tabs, redrawn folder tabs, vertical navigation
- Text field, select, slider, tri-state checkbox, radio button, switch
- Card, feed card, pill container, badge, progress, metric, information row, notice, collapse
- Dialog, reusable animated background, Alert, drawer, tooltip, overlay host with live background blur

The gallery provides categories, live examples, editable parameters and copyable Kotlin. State is owned by callers. There is one fixed palette, no color override API, no light theme, no bundled icon set, fonts or audio. Intrinsic marks are drawn by the components. Business icons are composable content. Custom keyboard navigation, shortcuts and gamepads are outside this version's scope; feed cards support native input focus/activation, and normal text editing and IME remain available.

## Platforms

| Target | Build / minimum |
| --- | --- |
| Android | APK, Android 12 / API 31+; required for live RenderEffect blur |
| Windows | x64 MSI; Windows 10+ |
| macOS | Apple Silicon DMG; macOS 12+ |
| Linux | x64 DEB; current Ubuntu / Debian with a graphical desktop |
| iOS | ARM64 device framework, ARM64 simulator app; iOS 14+ |
| Web | Kotlin/Wasm, current Chrome, Edge, Firefox and Safari with Wasm GC |

Intel Mac, Windows/Linux ARM64 and legacy browsers are not targeted. The default system font is used: Linux installations need a system CJK font to render Chinese. Installers are unsigned; a distributable iOS device app requires your Apple signing configuration. CI simulator builds do not constitute real-device visual acceptance. The macOS installer uses internal bundle version `1.0.0` for jpackage compatibility; the library and gallery release remain `0.1.0`.

## Use

After publication, add to `commonMain.dependencies`:

```kotlin
implementation("moe.tlaster.zenlessui:zenless-ui:0.1.0")
```

For local development, use `implementation(project(":zenless-ui"))` or publish to Maven Local. Imports in gallery snippets come from `moe.tlaster.zenlessui.*`, Compose runtime, foundation layout, `Modifier`, and `ToggleableState` where needed.

```kotlin
import androidx.compose.runtime.*
import moe.tlaster.zenlessui.*

@Composable
fun Example() {
    var open by remember { mutableStateOf(false) }
    ZenlessTheme {
        ZenlessOverlayHost {
            ZenlessSurface {
                ZenlessButton({ open = true }, tone = ZenlessTone.Accent) {
                    ZenlessText("Continue")
                }
                ZenlessAlert(
                    visible = open,
                    message = "Apply these changes?",
                    confirmText = "Apply",
                    onConfirm = { open = false },
                    cancelText = "Cancel",
                    onDismissRequest = { open = false },
                )
            }
        }
    }
}
```

Use one `ZenlessOverlayHost` around the application, including selects and tooltips. Alert and drawer callbacks run after their exit animation. Alerts do not dismiss on outside taps. Drawer outside taps dismiss. Tooltip hover delay is 600 ms; touch uses the platform long-press threshold. Sliding after a long press dismisses the hint and allows parent scrolling; the long press never activates its anchor. Moving its anchor or tapping outside also dismisses it. Tooltips accept text only.

`ZenlessDialog` owns the asymmetric shell, textured fixed header, title, close control, translucent inner plate and content padding. Its `ColumnScope` content slot owns all business controls and responsive layouts. Default width is 762 dp; caller `Modifier` can constrain it, and the window stays within the safe viewport with 16 dp margins. Long content scrolls inside the plate, leaving the title and close control fixed. The title uses one line with ellipsis; reopening resets scroll. Keep the composable mounted while changing `visible` so the exit can finish. Close, Escape and Android Back invoke `onDismissRequest` once after exit; outside taps only block. Focus enters the modal, stays inside it, then returns to the previous control when possible. This version supports one active Dialog, without stacking or nested modals.

```kotlin
ZenlessDialog(
    visible = open,
    title = "More actions",
    closeDescription = "Close dialog",
    onDismissRequest = { open = false },
    background = {
        ZenlessAnimatedBackground {
            ZenlessText("026", Modifier.padding(24.dp), style = ZenlessTextStyle.Title)
        }
    },
) {
    ZenlessButton({ /* caller action */ }) { ZenlessText("Change username") }
}
```

The optional `background` slot replaces only the body decoration. `ZenlessAnimatedBackground` is also usable alone with a bounded viewport, for example `Modifier.fillMaxWidth().height(240.dp)`. Its content slot supplies **one repeatable tile**, composed once; the component records and draws it with fixed tilt, low opacity, staggered repetition and continuous diagonal drift. Supply text, numbers, images or custom drawing, including their dimensions and padding. The tile is measured within the viewport; each repeat pitch is at least 48 dp to bound tiny-tile work. Empty/zero-size content leaves the static base and dots. Decorations do not accept input, focus or accessibility traversal. Font scale is fixed at 1 only inside the decorative tile; normal Dialog text retains system scaling. Motion survives recomposition and tile changes, pauses offscreen, and ends on disposal. A reopened Dialog starts a new background instance. The Gallery's **Dialogs & backgrounds** page includes editable pattern text, a custom background, narrow/long content and copyable Kotlin. See [Dialog calibration](docs/SPEC.md#dialog-calibration) for independent region comparisons and font/background exceptions.

Controls share three size presets: `Compact` (40 dp / 14 sp), `Default` (52 dp / 20 sp), and `Comfortable` (62 dp / 24 sp). Set `ZenlessTheme(size = ZenlessSize.Compact) { ... }` for a whole group, or pass `size` to one control. Heights are minimums and grow with wrapped or enlarged text. Buttons, fields, selects, switches, checkboxes, radios, sliders, both tab styles/navigation, information rows, collapse headers and Alert actions use the same presets. Back/close controls use the same height with a 3:2 aspect ratio (60×40 / 78×52 / 93×62 dp). Their contours, glyphs and bevels scale together; folder rails, corners and lift distances follow the preset. Icon slots and padding follow the preset; switch tracks scale proportionally. Spinners, badges, notices, progress bars, metrics, tooltips, card insets, navigation caps and Alert/drawer spacing retain their own proportions while scaling from Default by 40/52 or 62/52. Cards and drawers pass a local size override to their content. Explicit text roles such as Title and Caption remain independent of control size.

This replaces the earlier five button-only sizes before the first Central release. Migrate `Mini` / `Small` to `Compact`, and `Large` / `Extra` to `Default`. Existing `Default` now means 52 dp / 20 sp; use `Compact` for the previous 40 dp / 14 sp scale. Gallery's **Control size** selector updates the previews and their copyable code; Foundations includes all three sizes together.

`ZenlessCheckbox` uses a black capsule, a shaded green ring/check and gray text. Checking squashes and turns the icon while revealing the check, settling over 400 ms; unchecking restores the empty ring immediately. Only the icon moves. Holding adds a yellow-green outer rim with 100 ms fades, without a release flash or hover highlight. Reserve 4.5 dp at Default (scaled with the indicator) beside the capsule if a parent clips its drawing. The whole row toggles; mixed selects On, disabled controls use muted ink, and external state changes never invoke the callback. Existing platform fonts, three size presets and RadioButton behavior are unchanged. See [checkbox calibration](docs/SPEC.md#checkbox-calibration) for reference comparisons.

`ZenlessFeedCard` provides required `cover`, `avatar`, `author`, `title` and `summary` composable slots, plus optional `label` and `status` slots with built-in blue/dark capsule backgrounds. Its width comes from the parent; `coverAspectRatio` (width / height, finite and positive) reserves the cover before loading. Use `fillMaxSize()` in image slots, and supply crop/alignment, loading/error artwork and avatar background yourself. `ZenlessText` inherits each slot's default typography; the caller chooses any `maxLines`. No slot is truncated by the card. Pass null to hide a capsule; an empty lambda still draws its background. Slots are display-only, with one whole-card `onClick`. `selected` stays caller-owned; pressing or native input focus also activates the existing two-second color cycle, without scaling, hover highlighting or release flashes. Disabled cards suppress activation and breathing. The **Feed cards** gallery shows a staggered grid, three cover ratios, selection, optional capsules, disabled state, unrestricted titles and copyable code. See [the calibration procedure](docs/SPEC.md#feed-card-calibration) for local reference overlays and differences.

Non-folder `ZenlessTabs` use circular outer ends and slanted interior edges. Selection slides and morphs over 280 ms with a smooth ease-out, keeping its breathing phase through rapid selection changes. Text turns black where the slider covers it. Set `expand = false` for color-only breathing without the scale pulse; selection still moves and morphs. The Gallery's Navigation page exposes this as **Scale pulse**. Folder tabs ignore `expand`.

Buttons use black text and standalone icons while held. A leading icon in a black disc instead follows the plate's yellow-green breathing color, as do Alert action badges. Pass business artwork through `leadingIcon` and read `zenlessContentColor` inside the slot; this is a read-only state color, not a palette override. Regular content slots also expose this color for caller-drawn icons. Use `fillMaxSize()` inside the bounded icon slot to follow the control size. For example (with Compose `Canvas`, `Modifier`, `fillMaxSize`, `padding` and `dp` imports):

```kotlin
ZenlessButton(onClick = { /* navigate */ }, leadingIcon = {
    val ink = zenlessContentColor
    Canvas(Modifier.fillMaxSize().padding(5.dp)) { drawCircle(ink) }
}) {
    ZenlessText("Navigate")
}
```

All general button variants and standalone icon buttons share the gray shell and state text colors: white when enabled, `#565657` when disabled, and black while held. Filled buttons with a leading icon keep a dark plate; `tone` selects the fixed color of the leading badge. The disabled badge retains a dim version of its color while the shell and checker texture remain unchanged. Buttons use normal-weight platform-default text without forced italics. Labels with an icon are centered between the leading circle's far edge and the trailing semicircle's center, mirrored in RTL; labels without an icon are centered in the whole button. Alert actions reuse this same `ZenlessButton` with `leadingIcon`.

`ZenlessPillContainer` draws an opaque, shaded capsule around one display-only `RowScope` content slot. Width follows the content or `Modifier`; the three presets supply minimum heights, inherited typography and proportional padding (18 dp horizontal / 6 dp vertical at Default). Taller content grows the shell, while explicit size constraints clip it. Omit `onClick` for a display-only container. Otherwise it has one whole-container action and native keyboard focus/activation; the caller owns `selected`. Press, focus and selection share the existing two-second yellow/green rim cycle without scaling, hover feedback or release flashes. Disabling retains the shell and suppresses input/breathing. Leave 3 dp at Default (proportional to the preset) around the shell for its external rim; it does not change measured size and a clipping parent can hide it. The **Pill containers** Gallery page includes live state/width/content controls, a display-only composition and matching Kotlin. See [pill calibration](docs/SPEC.md#pill-container-calibration) for original-size reference comparisons.

Action buttons track the first touch or left mouse press that starts inside them. Moving outside, returning or scrolling keeps their held animation; releasing anywhere plays the 150 ms tail without turning a cancelled click into activation. Extra fingers do not extend the hold, and a new press interrupts the previous tail. Focus loss, disabling/loading, removal or system cancellation clears feedback without a tail and requires a new press. Callback timing is unchanged.

Select triggers use the same hold tracking on their breathing outer rim, with unchanged plate, text and arrow colors. A valid release opens the menu and continues the breathing phase without a flash. An outside release, scroll or consumed tooltip long press leaves the menu closed and plays the 150 ms rim flash. Open menus keep breathing; selecting an option or dismissing outside fades the rim with the existing 100 ms close. Window focus loss or disabling immediately closes the menu and clears feedback; restoring focus does not reopen it.

## Build

Toolchain: Compose Multiplatform 1.12.1, Kotlin 2.4.10, AGP 9.4.1, Gradle 9.8.0, JDK 25. Android compilation uses SDK 37.0. Set `ANDROID_HOME` or an untracked `local.properties` with `sdk.dir`. Use the checked-in wrapper (`gradlew.bat` on Windows).

```sh
./gradlew :zenless-ui:desktopTest :gallery:desktopTest
./gradlew :gallery:run
./gradlew :gallery:wasmJsBrowserDevelopmentRun
./gradlew :gallery:wasmJsBrowserDistribution
./gradlew :androidApp:assembleDebug
# Run each native package task on its own OS:
./gradlew :gallery:packageMsi
./gradlew :gallery:packageDmg
./gradlew :gallery:packageDeb
```

Open `iosApp/iosApp.xcodeproj` on an Apple Silicon Mac and select `iosApp` and an ARM64 iPhone simulator. For devices, select your development team in Xcode. The Xcode build phase builds and links the shared Kotlin framework. No Intel simulator target is included.

During UI iteration, local verification stays focused on the affected components; full local builds and waiting for CI are unnecessary. Pushes and pull requests still run the full **Build and verify** workflow, producing APK, native packages, Web assets and the simulator app, including desktop reports and Android/iOS launch screenshots. **UI checks and Gallery** independently runs desktop component/Gallery tests and a Web build, followed by Pages deployment, so the preview does not wait for all platform jobs. See [release instructions](docs/RELEASING.md) for Maven Central.

## Design and attribution

See [the scope and motion contract](docs/SPEC.md) and [third-party notices](THIRD_PARTY_NOTICES.md). Folder tabs and intrinsic control marks use Compose vector geometry.

MIT © Tlaster. Derived visual constants and motion retain ChrisChan's upstream MIT notice.
