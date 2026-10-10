"""Compare unscaled pill shells; reference artwork remains local and is not distributed.

python tools/compare_pill.py --prepare normal.png selected.png background.png
gradlew :zenless-ui:desktopTest --tests "*PillTest*"
python tools/compare_pill.py
Requires Pillow and NumPy. No reference UI pixels are used as rendered content.
"""
import argparse
import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

OUT = Path(__file__).resolve().parents[1] / "verification" / "pill"
CROP = (340, 24, 1000, 180)
CASES = ("normal", "selected", "background")
Y, X = np.mgrid[:156, :660]
# Original shell: (355, 40)..(984, 165), circular ends, radius 62.5.
DISTANCE = 62.5 - np.hypot(np.maximum(np.maximum(77.5-(X+.5), (X+.5)-581.5), 0), Y+.5-78.5)
CALLER = (X+.5-79)**2 + (Y+.5-78.5)**2 < 53**2
for x0, y0, x1, y1 in ((145, 35, 277, 89), (142, 89, 501, 127), (520, 35, 609, 125)):
    CALLER |= (X >= x0) & (X < x1) & (Y >= y0) & (Y < y1)


def prepare(paths):
    OUT.mkdir(parents=True, exist_ok=True)
    for name, path in zip(CASES, paths):
        original = Image.open(path).convert("RGB")
        if original.size != (3840, 2160):
            raise ValueError("Reference must be the original 3840 x 2160 PNG")
        ref = original.crop(CROP)
        ref.save(OUT / f"{name}-reference.png")
        if name == "background":
            backing = np.array(ref)
            # Reconstruct the occluded backing, including a 2px exterior safety band.
            # Its uncertainty is included in the outer-edge error, not masked away.
            for y in range(156):
                xs = np.flatnonzero(DISTANCE[y] >= -2)
                if len(xs):
                    left, right = xs[0]-1, xs[-1]+1
                    backing[y, xs] = np.linspace(backing[y, left], backing[y, right], len(xs)+2)[1:-1]
            Image.fromarray(backing).save(OUT / "backing.png")


def regions(name):
    masks = {
        "face": (DISTANCE >= 9) & ~CALLER,
        "bevel": (DISTANCE >= 3) & (DISTANCE < 9) & ~CALLER,
        "black_surround": (DISTANCE >= 0) & (DISTANCE < 3) & ~CALLER,
        "outer_edge": (DISTANCE >= -2) & (DISTANCE < 0),
    }
    if name == "selected":
        masks["highlight"] = (DISTANCE >= -8) & (DISTANCE < 0)
    return masks


def compare():
    report = {"baseline": "Windows Desktop, density 2, fontScale 1, Default; explicit 314.5 x 62.5 dp shell", "cases": {}}
    sheet = Image.new("RGB", (1320, 3*2*188), "#202024")
    draw = ImageDraw.Draw(sheet)
    for index, name in enumerate(CASES):
        ref = Image.open(OUT / f"{name}-reference.png").convert("RGB")
        actual = Image.open(OUT / f"{name}-actual.png").convert("RGB")
        assert ref.size == actual.size == (660, 156), "Never resize a mismatched fixture"
        a, b = np.array(ref), np.array(actual)
        diff = np.abs(a.astype(np.int16)-b.astype(np.int16))
        masks = regions(name)
        mask = np.logical_or.reduce(list(masks.values()))
        assert mask.any() and not mask.all() and not np.any(mask & CALLER)
        metrics = {}
        for key, valid in {**masks, "all_shell_regions": mask}.items():
            values = diff[valid]
            metrics[key] = {"pixels": int(valid.sum()), "mae_rgb_255": round(float(values.mean()), 3),
                            "p95_channel_error": round(float(np.percentile(values, 95)), 2), "max_channel_error": int(values.max())}
        if name == "selected":
            def green(p):
                return (p[:,:,0] > 60) & (p[:,:,1] > 75) & (p[:,:,2] < 50) & (DISTANCE < 0)
            r, s = green(a), green(b)
            metrics["highlight_iou"] = round(float((r & s).sum()/(r | s).sum()), 6)
        report["cases"][name] = metrics
        overlay = Image.blend(ref, actual, .5)
        raw = np.clip(diff*4, 0, 255).astype(np.uint8)
        heat = raw.copy(); heat[~mask] = 0
        Image.fromarray(raw).save(OUT / f"{name}-raw-difference-x4.png")
        Image.fromarray(mask.astype(np.uint8)*255).save(OUT / f"{name}-mask.png")
        overlay.save(OUT / f"{name}-overlay.png")
        Image.fromarray(heat).save(OUT / f"{name}-difference-x4.png")
        for n, (label, im) in enumerate(zip(("REFERENCE", "RENDERED (empty content slot)", "50% OVERLAY", "SHELL DIFFERENCE x4"), (ref, actual, overlay, Image.fromarray(heat)))):
            x, y = n % 2 * 660, index*376 + n//2*188
            draw.text((x+10, y+8), name.upper()+" / "+label, fill="white")
            sheet.paste(im, (x, y+32))
    report["limitations"] = [
        "Fixed caller masks exclude avatar, text and progress; all shell edges remain evaluated.",
        "The colored backing hidden by the source shell is interpolated; exterior safety-band error includes that uncertainty.",
        "Selected fixture freezes the existing 2-second palette at a measured matching phase. Motion timing is a product contract, not inferred from a PNG.",
        "Antialiasing and source raster noise can differ. Raw differences are retained; zero error is not assumed.",
    ]
    (OUT / "metrics.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
    sheet.save(OUT / "comparison.png")
    print(json.dumps(report, indent=2))
    # Regression budgets for this measured calibration, not a definition of pixel identity.
    for name, limit in (("normal", 1.0), ("selected", 2.0), ("background", 1.7)):
        case = report["cases"][name]
        assert case["all_shell_regions"]["mae_rgb_255"] <= limit, f"{name}: shell calibration regressed"
        assert case["face"]["mae_rgb_255"] <= 1.0, f"{name}: opaque face calibration regressed"
    assert report["cases"]["selected"]["highlight_iou"] >= .975, "Highlight contour regressed"


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--prepare", nargs=3, type=Path, metavar=("NORMAL", "SELECTED", "BACKGROUND"))
    args = parser.parse_args()
    if args.prepare:
        prepare(args.prepare)
    else:
        compare()
