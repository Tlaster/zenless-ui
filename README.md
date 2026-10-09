# zenless-ui

A fixed dark-theme UI library for Compose Multiplatform, with a Chinese / English interactive gallery. Layered plates, geometric tabs, a yellow-green selection pulse, release flashes, and full-width alerts.

[Web gallery](https://tlaster.github.io/zenless-ui/) · [Builds and verification](https://github.com/Tlaster/zenless-ui/actions/workflows/verify.yml) · [中文说明](docs/README.zh-CN.md)

Version **0.1.0**. Maven Central publication is configured; the first upload requires the repository owner's Central namespace, token and signing key. Do not assume the coordinate is available until a successful Publish workflow is linked in a release.

## Components

- Theme, text, surface, button, icon content slot, back and close buttons, spinner
- Horizontal tabs, redrawn folder tabs, vertical navigation
- Text field, select, slider, tri-state checkbox, radio button, switch
- Card, badge, progress, metric, information row, notice, collapse
- Alert, drawer, tooltip, overlay host with live background blur

The gallery provides categories, live examples, editable parameters and copyable Kotlin. State is owned by callers. There is one fixed palette, no color override API, no light theme, no bundled icon set, fonts or audio. Intrinsic marks are drawn by the components. Business icons are composable content. Keyboard navigation, shortcuts and gamepads are outside this version's scope; normal text editing and IME remain available.

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

Use one `ZenlessOverlayHost` around the application, including selects and tooltips. Alert and drawer callbacks run after their exit animation. Alerts do not dismiss on outside taps. Drawer outside taps dismiss. Tooltip hover delay is 600 ms; touch uses the platform long-press threshold, and moving its anchor or tapping outside dismisses it. Tooltips accept text only.

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

The build workflow uploads APK, native packages, Web assets and the simulator app. It also produces desktop test reports and captures launch screenshots from Android and iOS simulators. The Pages deployment uses the tested Web artifact. See [release instructions](docs/RELEASING.md) for Maven Central.

## Design and attribution

See [the scope and motion contract](docs/SPEC.md) and [third-party notices](THIRD_PARTY_NOTICES.md). Folder tabs and intrinsic control marks use Compose vector geometry.

MIT © Tlaster. Derived visual constants and motion retain ChrisChan's upstream MIT notice.
