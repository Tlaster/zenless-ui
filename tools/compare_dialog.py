"""Compare native Dialog regions independently; requires Pillow and NumPy.

python tools/compare_dialog.py --prepare capture.png
gradlew :zenless-ui:desktopTest --tests "*DialogTest"
python tools/compare_dialog.py
"""
import argparse
import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "verification" / "dialog"
CROP = (1150, 590, 2690, 1580)
Y, X = np.mgrid[590:1580, 1150:2690]


def box(x0, y0, x1, y1):
    return (X >= x0) & (X < x1) & (Y >= y0) & (Y < y1)


def compare():
    ref = Image.open(OUT / "reference.png").convert("RGB")
    actual = Image.open(ROOT / "verification" / "dialog-reference.png").convert("RGB")
    assert actual.size == (3840, 2160), "Never resize the native fixture"
    actual = actual.crop(CROP)
    assert ref.size == actual.size == (1540, 990)
    a, b = np.array(ref), np.array(actual)
    diff = np.abs(a.astype(np.int16) - b.astype(np.int16))
    regions = {
        "shell_header": box(1170, 610, 2674, 824) & ~box(1275, 705, 1580, 800) & ~box(2425, 645, 2640, 788),
        "shell_opaque_straight_edges": box(1224, 599, 2678, 604) | box(1224, 1555, 2618, 1561) | box(1158, 670, 1164, 1490) | box(2676, 602, 2682, 1490),
        "close_button": box(2440, 650, 2630, 780),
    }
    # Fixed capsule masks retain contour/face, excluding system-font labels and outside backing.
    for index, (left, top) in enumerate(((1330, 966), (1944, 966), (1330, 1126), (1944, 1126), (1330, 1286))):
        dx = np.maximum(np.maximum(left + 58 - (X + .5), X + .5 - (left + 566 - 58)), 0)
        distance = 58 - np.hypot(dx, Y + .5 - (top + 58))
        regions[f"content_button_{index + 1}"] = (distance >= 0) & ~box(left + 65, top + 24, left + 500, top + 94)
    report = {"crop": CROP, "metric": "mean absolute RGB error, 0-255", "regions": {}}
    combined = np.zeros(X.shape, dtype=bool)
    for name, mask in regions.items():
        assert mask.any(), name
        combined |= mask
        report["regions"][name] = {"pixels": int(mask.sum()), "mae": round(float(diff[mask].mean()), 4)}
        Image.fromarray(mask.astype(np.uint8) * 255).save(OUT / f"{name}-mask.png")
    close = regions["close_button"]
    ref_red = (a[:, :, 0] > 100) & (a[:, :, 1] < 50) & close
    out_red = (b[:, :, 0] > 100) & (b[:, :, 1] < 50) & close
    report["close_red_iou_percent"] = round(100 * float((ref_red & out_red).sum()) / (ref_red | out_red).sum(), 4)
    report["exceptions"] = [
        "No combined score: shell, close and every example button are reported independently.",
        "System-font glyphs are excluded. Caller background artwork is intentionally different and unscored.",
        "Backdrop, translucent inner plate and curved shell edges have different backing; inspect their geometry visually.",
        "Motion, background tiling, clipping and lifecycle are checked separately in DialogTest, not by this still image.",
    ]
    actual.save(OUT / "actual.png")
    Image.blend(ref, actual, .5).save(OUT / "overlay.png")
    heat = np.clip(diff * 4, 0, 255).astype(np.uint8)
    Image.fromarray(heat).save(OUT / "raw-difference-x4.png")
    heat[~combined] = 0
    Image.fromarray(heat).save(OUT / "masked-difference-x4.png")
    sheet = Image.new("RGB", (3080, 1020), "#181818")
    sheet.paste(ref, (0, 30)); sheet.paste(actual, (1540, 30))
    draw = ImageDraw.Draw(sheet)
    draw.text((12, 8), "REFERENCE / original resolution", fill="white")
    draw.text((1552, 8), "ACTUAL / font and background artwork intentionally differ", fill="white")
    sheet.save(OUT / "comparison.png")
    (OUT / "metrics.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(json.dumps(report, indent=2))
    budgets = {"shell_header": 1.1, "shell_opaque_straight_edges": .1, "close_button": 4.2}
    budgets.update({f"content_button_{i}": 1.6 for i in range(1, 6)})
    for name, limit in budgets.items():
        assert report["regions"][name]["mae"] <= limit, f"{name} exceeded its regression budget"
    assert report["close_red_iou_percent"] >= 97, "Close contour regressed"


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--prepare", type=Path)
    args = parser.parse_args()
    if args.prepare:
        source = Image.open(args.prepare).convert("RGB")
        assert source.size == (3840, 2160), "Use the original capture without resizing"
        OUT.mkdir(parents=True, exist_ok=True)
        source.crop(CROP).save(OUT / "reference.png")
    else:
        compare()
