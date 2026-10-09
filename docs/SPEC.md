# 0.1.0 implementation contract

## Visual foundation

One dark theme, platform default font, fixed semantic tones: Neutral `#333333`, Accent `#FFEA00`, Primary `#008BFF`, Success `#00CC0D`, Warning `#FFC300`, Danger `#C01C00`, Info `#CCCCCC`. There is no palette or motion-disable API.

Button minimum heights are 30 / 34 / 40 / 46 / 52 dp. Typography scales are 12 / 12 / 14 / 16 / 18 sp. Horizontal paddings are 17 / 23 / 29 / 47 / 59 dp. These values are logical dimensions; text scaling may increase measured height. Business icons belong in content slots. Checkbox checks, mixed marks, close, back and dropdown arrows are intrinsic geometry.

All general button tones and variants, including standalone icon buttons, share a `#262626` shell with a `#3D3D3D` upper bevel. Enabled text is white, disabled text is `#565657`, and held text is black. The face is inset by 8.35% of the shorter edge from the rim bounds; disabling does not change the shell. Dark faces have black / `#090909` checks at a 4.8 dp period. Filled leading-icon buttons keep a dark face: the tone colors a badge at the left edge (Danger `#FF2B00`, Warning `#FFB000`), with black caller artwork and bold italic platform-default text at 46% of the size's nominal height. Disabled badges retain 35.3% of their color. The leading disc uses the same shell drawing as the body.

Control text inherits its surrounding style (14 sp by default). Explicit Body / Title / Subtitle / Caption / Number styles are 24 / 42 / 28 / 20 / 44 sp; text inside a button inherits its state ink. Other enabled plates have a 1 dp outer inset, 3 dp semantic edge, black separator and face inset of 5 dp. Inputs and other disabled plates use a 4 dp face inset. Their ordinary texture repeats every 6 dp; colors interpolate by channel. Back and close plates are 90 by 60 dp cubic contours, with contour-normal expansion during presses. The back plate has a separately measured inner contour, a `#C50600` filled arrow and rim, an upper-left bevel, and soft black / `#090909` checker cells repeating every 4.806 dp. Slider knobs are 24 dp concentric metal discs.

Folder tabs occupy an 84 dp rail, with 18 dp top corners and square bottom corners. Their unbranded faces are redrawn vector artwork. A single-line Alert has a centered 186 dp band and 272 by 56 dp actions centered across its lower edge; content grows for multiple lines and actions adapt to narrow screens. Alert actions reuse `ZenlessButton` with `leadingIcon`, including its shell, state ink and interaction feedback. Drawer width is 46% of the viewport, capped at 680 dp, or full width below 600 dp. It has a 96 dp header and a draggable 8 dp scroll thumb.

## State and motion

- Controlled values, selection and visibility are supplied by callers. Radio groups are caller-owned; wrap grouped radios in `selectableGroup()`.
- Checkbox toggling maps Off or Indeterminate to On, and On to Off. Switch toggles on click/tap; dragging is not a switch interaction.
- Disabled/loading buttons suppress activation. Loading reserves the existing content footprint. Delayed back/close and Alert actions guard repeat activation.
- The back arrow changes from `#C50600` to black with the pressed highlight, remains black throughout a hold, follows the release flash, and restores its resting red after release or cancellation.
- Held buttons use black text, including explicit Caption text. Standalone and inline icons use black ink; icons inside a leading black disc instead follow the plate's yellow-green pulse in phase. `ZenlessButton.leadingIcon` and Alert action badges use that leading-disc rule. Caller-drawn icons read the read-only `zenlessContentColor` inside their slot. Tabs, navigation rows, select triggers and options provide a bright pressed face behind their black text. Resting, disabled and loading colors remain distinct from a held state.
- Pressed plates and horizontal tabs use a 1.5-second yellow-to-green cycle. Outline expansion is 15% of the shorter edge, with a 150 ms attack and 640 ms repeating pulse. Release uses a 150 ms double flash, including a down/up within one rendered frame. Selected ordinary button borders, options and vertical navigation use a 2-second color cycle without expansion. Folder tabs shift 7 dp and interpolate their face/text colors over 160 ms.
- Select opens over 200 ms with 32 dp translation, closes over 100 ms, scrolls the selected row into view, and flips/clamps against the viewport. Input height is at least 62 dp, option height at least 64 dp.
- Alert background blur is live, up to 4.5 dp, with a black mask at 0.58 opacity. Opening is staged over 250 ms; closing lasts 220 ms. The mask blocks background interaction through exit. Drawer slides in over 280 ms with cubic ease-out and out over 220 ms with cubic ease-in. Its 0.40-opacity scrim fades over 180 / 220 ms, and its content scrolls independently of the header/footer.
- Tooltip accepts supplementary text. Mouse hover delay is 600 ms; touch long press uses the platform threshold. Outside tap, scrolling the anchor away, or another tooltip dismisses it. No interactive or essential content belongs inside.
- Text input delegates IME, selection and clipboard behavior to Compose. No custom keyboard navigation/shortcuts or gamepad mapping is added.

## Gallery

Chinese and English follow the system language initially and can be toggled. Wide windows use a category sidebar; narrow windows show categories and detail separately. Examples use public library APIs. Parameter changes update the matching code snippets. Example state is ephemeral and resets when leaving a category. Code copying operates on the visible example.

## Acceptance

Build coverage and visual acceptance are separate. Automated checks cover state transitions, disabled/loading actions, modal exit callbacks, popup placement, timing curves, generated strings and responsive gallery screenshots. Native platform launch checks verify startup. Final motion fidelity and touch behavior still need human review on real devices; desktop screenshots and simulator startup are not substitutes.

The unit-density geometry fixture captures 1920 by 1080 pixels without labels. The test suite generates all Gallery category screenshots plus Alert, drawer and geometry captures under the ignored `verification/` directory. Default fonts, device scale and rasterizer antialiasing differ by platform.

The enabled/disabled leading-button fixture renders 244 by 58 dp controls at density 2. Native reference crops are 488 by 116 pixels, starting at (132, 1999) and (673, 1999) in the supplied 3840 by 2160 screenshot. After excluding the label rectangle and central icon disc, RGB mean absolute error is 0.90 for disabled and 1.02 for enabled (0–255 per channel); the previous disabled shell measured 7.06 under the same mask. The mask excludes x=136..414, y=26..89 and the circle centered at (59, 58.5) with radius 35. Regression checks cover the rim brightness, reference contour scanlines, unchanged disabled shell/texture and badge colors. These are static calibration measurements, not a claim of identical rasterizer antialiasing. Reference captures are not bundled or committed.

The back button uses the upper-left navigation control in the supplied 3840 by 2160 screenshot as its separate reference. Its fixture renders at density 2 into a 188 by 128 pixel crop, placing the 90 by 60 dp control at (4, 4). The matching reference crop starts at (128, 40). RGB mean absolute error decreased from 18.15 to 1.75 over that crop; on the union of pixels with a channel above 10 it is 5.08. Red-mask intersection over union is 97.92% (red > 100, green < 50). These static measurements include the arrow and texture. Regression assertions cover reference scanlines within one pixel, the red fill, bevel and checker samples. Edge antialiasing remains different from the captured raster; the reference image is not bundled with the library.
