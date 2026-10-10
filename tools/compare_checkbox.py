"""Compare original-size checkbox glyphs, excluding the retained platform font.

python tools/compare_checkbox.py --prepare off.png on.png
gradlew :zenless-ui:desktopTest --tests '*CheckboxTest'
python tools/compare_checkbox.py
Requires Pillow and NumPy; reference pixels are never rendered by the component.
"""
import argparse
import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

OUT = Path(__file__).resolve().parents[1] / "verification" / "checkbox"
CROP = (1750, 1090, 2100, 1210)
ICON = (38, 23, 102, 87)
Y, X = np.mgrid[23:87, 38:102]
MASK = (X + .5 - 68.4) ** 2 + (Y + .5 - 54) ** 2 < 29 ** 2


def prepare(paths):
    OUT.mkdir(parents=True, exist_ok=True)
    for state, path in zip(("off", "on"), paths):
        original = Image.open(path).convert("RGB")
        if original.size != (3840, 2160):
            raise ValueError("Use the original 3840 x 2160 PNG without resizing")
        original.crop(CROP).save(OUT / f"{state}-reference.png")


def compare():
    sheet = Image.new("RGB", (1024, 592), "#292929")
    draw = ImageDraw.Draw(sheet)
    report = {"baseline": "Windows Desktop; density 2; font scale 1; Default indicator; fixed 140dp empty label fixture", "states": {}}
    for row, state in enumerate(("off", "on")):
        ref = Image.open(OUT / f"{state}-reference.png").convert("RGB")
        actual = Image.open(OUT / f"{state}-actual.png").convert("RGB")
        assert ref.size == actual.size == (350, 120)
        a, b = np.array(ref.crop(ICON)), np.array(actual.crop(ICON))
        diff = np.abs(a.astype(np.int16) - b.astype(np.int16))
        def green(p):
            return (p[:, :, 1] > 100) & (p[:, :, 0] < 50) & (p[:, :, 2] < 50)
        r, s = green(a), green(b)
        assert r.any() and s.any()
        report["states"][state] = {"icon_mae_rgb_255": round(float(diff[MASK].mean()), 3),
            "green_iou": round(float((r & s).sum() / (r | s).sum()), 5)}
        overlay = Image.blend(ref, actual, .5)
        overlay.save(OUT / f"{state}-overlay.png")
        heat = np.clip(diff * 4, 0, 255).astype(np.uint8)
        Image.fromarray(heat).save(OUT / f"{state}-raw-difference-x4.png")
        heat[~MASK] = 0
        Image.fromarray(heat).save(OUT / f"{state}-icon-difference-x4.png")
        for col, (label, im) in enumerate((("REFERENCE", Image.fromarray(a)), ("RENDERED", Image.fromarray(b)),
            ("OVERLAY", overlay.crop(ICON)), ("DIFFERENCE x4", Image.fromarray(heat)))):
            x, y = col * 256, row * 296
            draw.text((x + 8, y + 10), state.upper() + " / " + label, fill="white")
            sheet.paste(im.resize((256, 256), Image.Resampling.NEAREST), (x, y + 35))
    Image.fromarray(MASK.astype(np.uint8) * 255).save(OUT / "icon-mask.png")
    report["limitations"] = ["A fixed radius-29px icon mask includes all glyph edges and a 4px safety band; text, shell and backdrop are not scored.",
        "The glyph is drawn as vectors. Native antialiasing can differ from the source raster.",
        "Font, font size and three control presets are retained by request; this is not full-control pixel identity."]
    (OUT / "metrics.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
    sheet.save(OUT / "comparison.png")
    print(json.dumps(report, indent=2))
    for state, max_error, min_iou in (("off", 3.3, .95), ("on", 5.4, .90)):
        assert report["states"][state]["icon_mae_rgb_255"] < max_error, f"{state} color regression"
        assert report["states"][state]["green_iou"] > min_iou, f"{state} contour regression"


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--prepare", nargs=2, metavar=("OFF", "ON"))
    args = parser.parse_args()
    if args.prepare:
        prepare(args.prepare)
    else:
        compare()
