"""Local, deterministic shell calibration; the reference artwork is never bundled.

python tools/compare_feed_card.py --prepare "path/to/original-3840x2160.png"
gradlew :zenless-ui:desktopTest --tests "*FeedCardTest.captureReferenceFixtures*"
python tools/compare_feed_card.py

Requires Pillow and NumPy. Masks are fixed reference coordinates, not adjusted to
hide differences in the rendered output. Raw differences are also preserved.
"""
import argparse
import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = Path(__file__).resolve().parents[1] / "verification" / "feed-card"
CASES = {
    "normal": (186, 416, 640, 931, 631),
    "selected": (893, 1167, 640, 931, 631),
    "status": (186, 1398, 640, 700, 400),
}


def prepare(path):
    original = Image.open(path).convert("RGB")
    if original.size != (3840, 2160):
        raise ValueError("Use the unresized 3840 x 2160 reference PNG")
    OUT.mkdir(parents=True, exist_ok=True)
    for name, (x, y, w, h, seam) in CASES.items():
        reference = original.crop((x, y, x+w, y+h))
        reference.save(OUT / f"{name}-reference.png")
        cover = reference.crop((10, 10, 631, seam))
        # Remove baked-in component pixels from the caller artwork before rendering.
        # These backing patches only affect translucent/antialiased edges; their
        # uncertainty is retained in the reported error, never copied over the output.
        pixels = np.array(cover)
        yy, xx = np.mgrid[:cover.height, :cover.width]
        remove = ((xx-98.5)**2 + (yy-(seam-5.5))**2 < 73.5**2)
        remove |= (yy > cover.height-74) & (xx < 203) & np.all(pixels == (24, 24, 24), axis=2)
        capsule_mask = Image.new("L", cover.size)
        draw = ImageDraw.Draw(capsule_mask)
        if name == "normal":
            draw.rounded_rectangle((25, 17, 337, 87), radius=34, fill=255)
        if name == "status":
            draw.rounded_rectangle((462, cover.height-86, 606, cover.height-15), radius=34, fill=255)
        remove |= np.array(capsule_mask) > 0
        for row in range(cover.height):
            xs = np.where(remove[row])[0]
            for group in np.split(xs, np.where(np.diff(xs) > 1)[0]+1):
                if not len(group):
                    continue
                left, right = max(0, group[0]-2), min(cover.width-1, group[-1]+2)
                pixels[row, group] = np.linspace(pixels[row, left], pixels[row, right], len(group))
        cover = Image.fromarray(pixels)
        cover.save(OUT / f"{name}-cover.png")
        reference.crop((43, seam-61, 175, seam+71)).save(OUT / f"{name}-avatar.png")


def masks(reference, seam, name):
    h, w = reference.shape[:2]
    yy, xx = np.mgrid[:h, :w]
    # The lower panel, excluding caller glyph rectangles and the avatar artwork.
    panel = (yy >= seam) & (xx >= 10) & (xx < 630) & (yy < h-9)
    panel &= ~((xx-108.5)**2 + (yy-(seam+4.5))**2 < 66.5**2)
    panel &= ~((xx >= 182) & (xx < 605) & (yy < seam+67))
    panel &= ~((xx >= 47) & (xx < 606) & (yy >= seam+92) & (yy < h-27))
    # Crest + its surrounding cover: include the reference silhouette and a
    # three-pixel exterior ring to detect either too much or too little panel.
    crest_area = (yy >= seam-75) & (yy < seam) & (xx >= 9) & (xx < 213)
    crest = crest_area & np.all(reference == (24, 24, 24), axis=2)
    crest = np.array(Image.fromarray(crest).filter(ImageFilter.MaxFilter(7))) & crest_area
    crest &= ~((xx-108.5)**2 + (yy-(seam+4.5))**2 < 66.5**2)
    # Straight rim and lower corner. The cover's curved top edge is checked
    # separately on the selected reference where the yellow contour is observable.
    rim = ((xx < 10) | (xx >= 630)) & (yy >= 90)
    rim |= yy >= h-10
    regions = {"panel_and_divider": panel, "avatar_join": crest, "rim": rim}
    if name == "normal":
        badge = (xx >= 34) & (xx < 347) & (yy >= 27) & (yy < 97)
        badge &= ~((xx >= 57) & (xx < 112) & (yy >= 35) & (yy < 91))
        badge &= ~((xx >= 117) & (xx < 317) & (yy >= 33) & (yy < 85))
        regions["category_capsule"] = badge
    if name == "status":
        badge = (xx >= 471) & (xx < 615) & (yy >= 315) & (yy < 384)
        badge &= ~((xx >= 496) & (xx < 586) & (yy >= 321) & (yy < 372))
        regions["status_capsule"] = badge
    if name == "selected":
        yellow = (reference[:, :, 0] > 180) & (reference[:, :, 1] > 140) & (reference[:, :, 2] < 35)
        # This fixed area spans both sides of the expected curved rim.
        outline = np.array(Image.fromarray(yellow).filter(ImageFilter.MaxFilter(9)))
        outline &= (yy < 100) | (xx < 18) | (xx > 622) | (yy > h-90)
        regions["selected_contour"] = outline
    return regions


def compare():
    report = {"baseline": "Windows Desktop / density 2 / fontScale 1 / Default", "cases": {}}
    sheets = []
    for name, (_, _, w, h, seam) in CASES.items():
        ref = Image.open(OUT / f"{name}-reference.png").convert("RGB")
        rendered = Image.open(OUT / f"{name}-actual.png").convert("RGB")
        if rendered.size != ref.size:
            raise AssertionError(f"{name}: actual {rendered.size}, reference {ref.size}; do not rescale a mismatch")
        a, b = np.array(ref), np.array(rendered)
        diff = np.abs(a.astype(np.int16)-b.astype(np.int16))
        regions = masks(a, seam, name)
        mask = np.logical_or.reduce(list(regions.values()))
        assert mask.any() and not mask.all()
        data = {}
        for region, valid in {**regions, "all_component_regions": mask}.items():
            values = diff[valid]
            data[region] = {"pixels": int(valid.sum()), "mae_rgb_255": round(float(values.mean()), 3),
                "p95_channel_error": round(float(np.percentile(values, 95)), 2),
                "max_channel_error": int(values.max())}
        if name == "selected":
            def yellow(p):
                return (p[:, :, 0] > 180) & (p[:, :, 1] > 140) & (p[:, :, 2] < 35)
            yy, xx = np.mgrid[:h, :w]
            contour_area = (yy < 100) | (xx < 20) | (xx > 620) | ((yy > h-100) & (xx < 100)) | (yy > h-20)
            r, s = yellow(a) & contour_area, yellow(b) & contour_area
            data["yellow_contour_iou"] = round(float((r & s).sum()/(r | s).sum()), 5)
        report["cases"][name] = data
        overlay = Image.blend(ref, rendered, .5)
        heat = np.zeros_like(a)
        heat[mask] = np.clip(diff[mask]*4, 0, 255).astype(np.uint8)
        Image.fromarray(np.clip(diff*4, 0, 255).astype(np.uint8)).save(OUT / f"{name}-raw-difference-x4.png")
        Image.fromarray(mask.astype(np.uint8)*255).save(OUT / f"{name}-mask.png")
        overlay.save(OUT / f"{name}-overlay.png")
        Image.fromarray(heat).save(OUT / f"{name}-difference-x4.png")
        sheet = Image.new("RGB", (w*4, h+48), "#202024")
        draw = ImageDraw.Draw(sheet)
        for i, (label, im) in enumerate(zip(("REFERENCE", "RENDERED (caller text omitted)", "50% OVERLAY", "COMPONENT DIFFERENCE x4"), (ref, rendered, overlay, Image.fromarray(heat)))):
            sheet.paste(im, (i*w, 48)); draw.text((i*w+12, 16), label, fill="white")
        sheet.save(OUT / f"{name}-comparison.png")
        sheets.append(sheet)
    report["limitations"] = [
        "Caller text, cover artwork and avatar artwork are excluded by fixed coordinate masks; see mask PNGs.",
        "Reference cover regions underneath badges/crest are occluded. Prepared artwork removes baked-in UI; antialiased edge error includes the reconstructed backing.",
        "Selected color uses the existing library breathing palette, not a sampled replacement palette.",
        "Raw x4 differences retain every pixel; masked x4 differences show only evaluated component regions.",
    ]
    (OUT / "metrics.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
    combined = Image.new("RGB", (2560, sum(im.height for im in sheets)), "#202024")
    y = 0
    for im in sheets:
        combined.paste(im, (0, y)); y += im.height
    combined.save(OUT / "comparison.png")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--prepare", type=Path)
    args = parser.parse_args()
    if args.prepare:
        prepare(args.prepare)
    else:
        compare()
