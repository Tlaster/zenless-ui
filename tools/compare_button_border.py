"""Compare the resource-management border at native resolution; requires Pillow and NumPy.

python tools/compare_button_border.py --prepare capture.png
gradlew :zenless-ui:desktopTest --tests "*ButtonBorderTest"
python tools/compare_button_border.py
"""
import argparse
import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

OUT = Path(__file__).resolve().parents[1] / "verification" / "button-border"
CROP = (2435, 1980, 3085, 2125)
Y, X = np.mgrid[:145, :650]
DISTANCE = 57.75 - np.hypot(np.maximum(np.maximum(84.75-(X+.5), (X+.5)-565.25), 0), Y+.5-77.25)
REGIONS = {
    "outer_rim": (DISTANCE >= -1) & (DISTANCE < 12),
    "upper_straight": (Y >= 19) & (Y < 29) & (X >= 150) & (X < 565),
    "left_curve": (X < 80) & (DISTANCE >= -1) & (DISTANCE < 12),
}


def prepare(path):
    source = Image.open(path).convert("RGB")
    if source.size != (3840, 2160):
        raise ValueError("Use the original 3840 x 2160 capture without resizing")
    OUT.mkdir(parents=True, exist_ok=True)
    source.crop(CROP).save(OUT / "reference-context.png")


def compare():
    reference = Image.open(OUT / "reference-context.png").convert("RGB")
    names = (["before"] if (OUT / "before.png").exists() else []) + ["actual"]
    images = [reference] + [Image.open(OUT / f"{name}.png").convert("RGB") for name in names]
    assert all(im.size == (650, 145) for im in images), "Never resize mismatched fixtures"
    report = {"crop": CROP, "metric": "mean absolute RGB error, 0-255", "cases": {}}
    sheet = Image.new("RGB", (1300, 180 * len(images)), "#181818")
    draw = ImageDraw.Draw(sheet)
    mask = REGIONS["outer_rim"]
    for index, (name, im) in enumerate(zip(["reference"] + names, images)):
        p = np.array(im)
        rim = np.zeros_like(p); rim[mask] = p[mask]
        for column, (label, part) in enumerate(((name.upper()+" / caller artwork differs", im), ("OUTER RIM ONLY", Image.fromarray(rim)))):
            draw.text((column*650+12, index*180+8), label, fill="white")
            sheet.paste(part, (column*650, index*180+30))
        if name == "reference":
            continue
        diff = np.abs(p.astype(np.int16)-np.array(reference).astype(np.int16))
        report["cases"][name] = {region: {"pixels": int(valid.sum()), "mae": round(float(diff[valid].mean()), 4)}
                                  for region, valid in REGIONS.items()}
        heat = np.clip(diff*4, 0, 255).astype(np.uint8)
        Image.fromarray(heat).save(OUT / f"{name}-raw-difference-x4.png")
        heat[~mask] = 0
        Image.fromarray(heat).save(OUT / f"{name}-rim-difference-x4.png")
    report["limitations"] = [
        "The fixed outer-rim mask excludes caller text, badge, inner ring and most of the face.",
        "The separate upper/left regions expose local errors; they overlap the outer-rim mask.",
        "The exterior black surround is tested on a flat backdrop, not scored against the game's background.",
        "Native curved-edge antialiasing remains sharper than the capture; this is not pixel identity.",
    ]
    Image.fromarray(mask.astype(np.uint8)*255).save(OUT / "rim-mask.png")
    sheet.save(OUT / "comparison.png")
    (OUT / "metrics.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(json.dumps(report, indent=2))
    assert report["cases"]["actual"]["outer_rim"]["mae"] < 1.7, "Outer rim regressed"
    assert report["cases"]["actual"]["upper_straight"]["mae"] < .6, "Upper highlight regressed"


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--prepare", type=Path)
    args = parser.parse_args()
    if args.prepare:
        prepare(args.prepare)
    else:
        compare()
