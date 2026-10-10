"""Inspect the complete Dialog frame, including curved, soft and translucent edges.

python tools/compare_dialog_border.py --prepare original.png
gradlew :zenless-ui:desktopTest --tests '*DialogFrameTest'
python tools/compare_dialog_border.py
Requires Pillow and NumPy. Prepared artwork is local calibration input only.
"""
import argparse
import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

OUT = Path(__file__).resolve().parents[1] / 'verification' / 'dialog-border'
CROP = (1126, 566, 2714, 1602)
Y, X = np.mgrid[566:1602, 1126:2714].astype(float)
X += .5; Y += .5


def distance(x, y, shift=0):
    # Signed distance, positive inside; top-right is square.
    cx = np.clip(x, 1220.75, 2619.25)
    cy = np.clip(y, 661.67 + shift, 1498.33 + shift)
    d = 63 - np.hypot(x - cx, y - cy)
    square = (x > 2619.25) & (y < 661.67 + shift)
    return np.where(square, np.minimum(2682.25-x, y-(598.67+shift)), d)


D = distance(X, Y)
G = distance(X, Y, 9)
# Fixed full-width bands retain AA pixels; no masking based on actual errors.
FRAME = (D >= -3) & (D <= 13)
BEVEL = (D < 0) & (G >= -3)
REGIONS = {
    'top_left': FRAME & (X < 1230) & (Y < 670),
    'top_right': FRAME & (X > 2610) & (Y < 670),
    'bottom_left': (FRAME | BEVEL) & (X < 1230) & (Y > 1490),
    'bottom_right': (FRAME | BEVEL) & (X > 2610) & (Y > 1490),
    'top_edge': FRAME & (X >= 1230) & (X <= 2610) & (Y < 630),
    'left_edge': FRAME & (Y >= 670) & (Y <= 1490) & (X < 1200),
    'right_edge': FRAME & (Y >= 670) & (Y <= 1490) & (X > 2640),
    'bottom_edge': FRAME & (X >= 1230) & (X <= 2610) & (Y > 1540),
    'translucent_bevel': BEVEL,
}
# Per-region regression limits, not a combined acceptance score.
BUDGETS = {
    'top_left': 2.5, 'top_right': 1.0, 'bottom_left': 2.6, 'bottom_right': 2.9,
    'top_edge': 1.1, 'left_edge': .9, 'right_edge': .7, 'bottom_edge': 1.3,
    'translucent_bevel': 3.5,
}


def sample(p, x, y):
    # Bilinear sampling; never interpolate UI edges into the cleaned inputs.
    x = np.clip(x - CROP[0] - .5, 0, p.shape[1]-1)
    y = np.clip(y - CROP[1] - .5, 0, p.shape[0]-1)
    x0 = x.astype(int); y0 = y.astype(int)
    x1 = np.minimum(x0+1, p.shape[1]-1); y1 = np.minimum(y0+1, p.shape[0]-1)
    u = (x-x0)[...,None]; v = (y-y0)[...,None]
    return (p[y0,x0]*(1-u)+p[y0,x1]*u)*(1-v)+(p[y1,x0]*(1-u)+p[y1,x1]*u)*v


def prepare(path):
    source = Image.open(path).convert('RGB')
    assert source.size == (3840,2160), 'Use the original native capture'
    OUT.mkdir(parents=True, exist_ok=True)
    ref = source.crop(CROP); ref.save(OUT/'reference.png')
    p = np.array(ref).astype(float)
    gy, gx = np.gradient(D)
    norm = np.maximum(np.hypot(gx,gy), .001); gx /= norm; gy /= norm
    # Extrapolate face from 16 px inside the border, where no frame pixels remain.
    face = sample(p, X+gx*(16-D), Y+gy*(16-D))
    face[D > 18] = 0
    # The backing is sampled beyond BOTH the frame and its displaced bevel.
    outside = np.maximum(D,G)
    oy, ox = np.gradient(outside)
    norm = np.maximum(np.hypot(ox,oy), .001); ox /= norm; oy /= norm
    backing = sample(p, X-ox*(outside+4), Y-oy*(outside+4))
    backing[outside < -3] = p[outside < -3]
    Image.fromarray(np.uint8(np.clip(face,0,255))).save(OUT/'face.png')
    Image.fromarray(np.uint8(np.clip(backing,0,255))).save(OUT/'backing.png')
    print('Prepared cleaned face and backing; border pixels were not copied into either input.')


def compare():
    ref = Image.open(OUT/'reference.png').convert('RGB'); a = np.array(ref)
    cases = {}
    names = (['baseline'] if (OUT/'baseline.png').exists() else []) + ['actual']
    full = np.logical_or.reduce(list(REGIONS.values()))
    for name in names:
        im = Image.open(OUT/f'{name}.png').convert('RGB')
        assert im.size == ref.size == (1588,1036), 'Do not resize comparison images'
        diff = np.abs(np.array(im).astype(np.int16)-a.astype(np.int16))
        cases[name] = {k:{'pixels':int(m.sum()),'mae':round(float(diff[m].mean()),4),
                          'p95':round(float(np.percentile(diff[m],95)),2)} for k,m in REGIONS.items()}
        heat = np.uint8(np.clip(diff*4,0,255))
        Image.fromarray(heat).save(OUT/f'{name}-raw-difference-x4.png')
        heat[~full]=0; Image.fromarray(heat).save(OUT/f'{name}-frame-difference-x4.png')
        Image.blend(ref,im,.5).save(OUT/f'{name}-overlay.png')
    for k,m in REGIONS.items():
        assert m.any(), k
        Image.fromarray(np.uint8(m)*255).save(OUT/f'{k}-mask.png')
    # Nearest-neighbor enlargement exposes source and renderer AA without resampling it away.
    corners = [('TL',(12,12,124,124)), ('TR',(1464,12,1576,124)),
               ('BL',(12,900,124,1012)), ('BR',(1464,900,1576,1012))]
    sheet = Image.new('RGB', (344*(len(names)+1),370*4), '#dcdcdc')
    draw = ImageDraw.Draw(sheet)
    for column, name in enumerate(['reference']+names):
        im = Image.open(OUT/f'{name}.png').convert('RGB')
        for row, (label, bounds) in enumerate(corners):
            draw.text((column*344+4,row*370+3), f'{label} {name}', fill='black')
            sheet.paste(im.crop(bounds).resize((336,336),Image.Resampling.NEAREST), (column*344+4,row*370+24))
    sheet.save(OUT/'corners-comparison.png')
    report = {'cases':cases,'limitations':[
        'All four corners, complete black bands, their soft inner edges and the lower bevel are included.',
        'Occluded face/backing are reconstructed from nearby uncontaminated pixels; their uncertainty remains in the errors.',
        'No source frame pixels enter rendered inputs. Raw differences and fixed masks remain available.',
        'These measurements are not a claim of pixel identity or cross-platform acceptance.']}
    (OUT/'metrics.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    print(json.dumps(report,indent=2))
    for region, limit in BUDGETS.items():
        assert cases['actual'][region]['mae'] <= limit, f'{region} exceeded its regression budget of {limit}'


if __name__ == '__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--prepare',type=Path);args=parser.parse_args()
    if args.prepare: prepare(args.prepare)
    else: compare()
