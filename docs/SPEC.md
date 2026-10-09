# 0.1.0 implementation contract

## Visual foundation

One dark theme, platform default font, fixed semantic tones: Neutral `#333333`, Accent `#FFEA00`, Primary `#008BFF`, Success `#00CC0D`, Warning `#FFC300`, Danger `#C01C00`, Info `#CCCCCC`. There is no palette or motion-disable API.

Button minimum heights are 30 / 34 / 40 / 46 / 52 dp. Typography scales are 12 / 12 / 14 / 16 / 18 sp. Horizontal paddings are 17 / 23 / 29 / 47 / 59 dp. These values are logical dimensions; text scaling may increase measured height. Business icons belong in content slots. Checkbox checks, mixed marks, close, back and dropdown arrows are intrinsic geometry.

Control text inherits its surrounding style (14 sp by default). Explicit Body / Title / Subtitle / Caption / Number styles are 24 / 42 / 28 / 20 / 44 sp. Enabled plates have a 1 dp outer inset, 3 dp semantic edge, black separator and face inset of 5 dp. Inputs and disabled plates use a 4 dp face inset. Texture repeats every 6 dp; colors interpolate by channel. Back and close plates are 90 by 60 dp cubic contours, with contour-normal expansion during presses. Slider knobs are 24 dp concentric metal discs.

Folder tabs occupy an 84 dp rail, with 18 dp top corners and square bottom corners. Their unbranded faces are redrawn vector artwork. A single-line Alert has a centered 186 dp band and 272 by 56 dp actions centered across its lower edge; content grows for multiple lines and actions adapt to narrow screens. Drawer width is 46% of the viewport, capped at 680 dp, or full width below 600 dp. It has a 96 dp header and a draggable 8 dp scroll thumb.

## State and motion

- Controlled values, selection and visibility are supplied by callers. Radio groups are caller-owned; wrap grouped radios in `selectableGroup()`.
- Checkbox toggling maps Off or Indeterminate to On, and On to Off. Switch toggles on click/tap; dragging is not a switch interaction.
- Disabled/loading buttons suppress activation. Loading reserves the existing content footprint. Delayed back/close and Alert actions guard repeat activation.
- Pressed plates and horizontal tabs use a 1.5-second yellow-to-green cycle. Outline expansion is 15% of the shorter edge, with a 150 ms attack and 640 ms repeating pulse. Release uses a 150 ms double flash, including a down/up within one rendered frame. Accent text, selected ordinary button borders, options and vertical navigation use a 2-second color cycle without expansion. Folder tabs shift 7 dp and interpolate their face/text colors over 160 ms.
- Select opens over 200 ms with 32 dp translation, closes over 100 ms, scrolls the selected row into view, and flips/clamps against the viewport. Input height is at least 62 dp, option height at least 64 dp.
- Alert background blur is live, up to 4.5 dp, with a black mask at 0.58 opacity. Opening is staged over 250 ms; closing lasts 220 ms. The mask blocks background interaction through exit. Drawer slides in over 280 ms with cubic ease-out and out over 220 ms with cubic ease-in. Its 0.40-opacity scrim fades over 180 / 220 ms, and its content scrolls independently of the header/footer.
- Tooltip accepts supplementary text. Mouse hover delay is 600 ms; touch long press uses the platform threshold. Outside tap, scrolling the anchor away, or another tooltip dismisses it. No interactive or essential content belongs inside.
- Text input delegates IME, selection and clipboard behavior to Compose. No custom keyboard navigation/shortcuts or gamepad mapping is added.

## Gallery

Chinese and English follow the system language initially and can be toggled. Wide windows use a category sidebar; narrow windows show categories and detail separately. Examples use public library APIs. Parameter changes update the matching code snippets. Example state is ephemeral and resets when leaving a category. Code copying operates on the visible example.

## Acceptance

Build coverage and visual acceptance are separate. Automated checks cover state transitions, disabled/loading actions, modal exit callbacks, popup placement, timing curves, generated strings and responsive gallery screenshots. Native platform launch checks verify startup. Final motion fidelity and touch behavior still need human review on real devices; desktop screenshots and simulator startup are not substitutes.

The unit-density geometry fixture captures 1920 by 1080 pixels without labels. Measured RGB mean absolute error against the visual baseline on 2026-10-10 (0–255 per channel): buttons 1.00, square button 0.21, back 1.88, close 2.31, card 1.02, text input 0.15, select 0.41, progress 0.46, badges 0.27. These measurements cover component rectangles, including their corner/background pixels, and exclude text, redrawn artwork and animated frames. They are calibration measurements, not a claim of identical pixels across renderers. Default fonts, device scale and rasterizer antialiasing differ by platform. The test suite generates all Gallery category screenshots plus Alert, drawer and geometry captures under the ignored `verification/` directory.
