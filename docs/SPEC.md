# 0.1.0 implementation contract

## Visual foundation

One dark theme, platform default font, fixed semantic tones: Neutral `#333333`, Accent `#FFEA00`, Primary `#008BFF`, Success `#00CC0D`, Warning `#FFC300`, Danger `#C01C00`, Info `#CCCCCC`. There is no palette or motion-disable API.

Button minimum heights are 30 / 34 / 40 / 46 / 52 dp. Typography scales are 12 / 12 / 14 / 16 / 18 sp. Horizontal paddings are 17 / 23 / 29 / 47 / 59 dp. These values are logical dimensions; text scaling may increase measured height. Business icons belong in content slots. Checkbox checks, mixed marks, close, back and dropdown arrows are intrinsic geometry.

## State and motion

- Controlled values, selection and visibility are supplied by callers. Radio groups are caller-owned; wrap grouped radios in `selectableGroup()`.
- Checkbox toggling maps Off or Indeterminate to On, and On to Off. Switch toggles on click/tap; dragging is not a switch interaction.
- Disabled/loading buttons suppress activation. Loading reserves the existing content footprint. Delayed back/close and Alert actions guard repeat activation.
- Press/selected plates use a 1.5-second yellow-to-green cycle. Outline expansion is 15% of the shorter edge, with the reference 150 ms attack and 640 ms repeating pulse. Release uses a 150 ms double flash. Vertical navigation changes color without expansion. Folder tabs shift 7 dp over 160 ms.
- Select opens over 200 ms with 32 dp translation, closes over 100 ms, scrolls the selected row into view, and flips/clamps against the viewport. Input height is at least 62 dp, option height at least 64 dp.
- Alert background blur is live, up to 4.5 dp, with a black mask at 0.58 opacity. Opening is staged over 250 ms; closing lasts 220 ms. The mask blocks background interaction through exit. Drawer slides in over 280 ms with cubic ease-out and out over 220 ms with cubic ease-in. Its 0.40-opacity scrim fades over 180 / 220 ms, and its content scrolls independently of the header/footer.
- Tooltip accepts supplementary text. Mouse hover delay is 600 ms; touch long press uses the platform threshold. Outside tap, scrolling the anchor away, or another tooltip dismisses it. No interactive or essential content belongs inside.
- Text input delegates IME, selection and clipboard behavior to Compose. No custom keyboard navigation/shortcuts or gamepad mapping is added.

## Gallery

Chinese and English follow the system language initially and can be toggled. Wide windows use a category sidebar; narrow windows show categories and detail separately. Examples use public library APIs. Parameter changes update the matching code snippets. Example state is ephemeral and resets when leaving a category. Code copying operates on the visible example.

## Acceptance

Build coverage and visual acceptance are separate. Automated checks cover state transitions, disabled/loading actions, modal exit callbacks, popup placement, timing curves, generated strings and responsive gallery screenshots. Native platform launch checks verify startup. Final motion fidelity and touch behavior still need human review on real devices; desktop screenshots and simulator startup are not substitutes.
