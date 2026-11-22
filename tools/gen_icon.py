"""
Turns the mascot artwork (tools/art/mascot_source.png) into the adaptive icon layer.

An adaptive icon is a 108dp square; launchers mask it and show roughly the middle 72dp.
So the script:
  1. crops off the white corners and the frame around the artwork,
  2. scales it to 64dp (Samsung's mask only shows about 64-66dp) to keep the whole picture,
  3. pads the rest with the artwork's average edge color as a safety margin.
Output: app/src/main/res/drawable-nodpi/ic_launcher_art.webp (432x432, i.e. 108dp at xxxhdpi).

Usage:
  python -I tools/gen_icon.py [preview.png]
"""
import os
import sys

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
SRC = os.path.join(ROOT, "tools", "art", "mascot_source.png")
OUT = os.path.join(ROOT, "app", "src", "main", "res", "drawable-nodpi", "ic_launcher_art.webp")

CANVAS = 432          # 108dp @ xxxhdpi
ART = CANVAS * 64 // 108   # some launchers (Samsung) mask tighter than the standard 72dp
CORNER_INSET = 60     # trims the white corners and the light frame drawn around the artwork


def edge_color(im):
    w, h = im.size
    pts = [(x, 2) for x in range(0, w, 16)] + [(x, h - 3) for x in range(0, w, 16)]
    pts += [(2, y) for y in range(0, h, 16)] + [(w - 3, y) for y in range(0, h, 16)]
    px = [im.getpixel(p) for p in pts]
    return tuple(sum(c[i] for c in px) // len(px) for i in range(3))


def main(preview=None):
    im = Image.open(SRC).convert("RGB")
    w, h = im.size
    art = im.crop((CORNER_INSET, CORNER_INSET, w - CORNER_INSET, h - CORNER_INSET))
    art = art.resize((ART, ART), Image.LANCZOS)

    canvas = Image.new("RGB", (CANVAS, CANVAS), edge_color(art))
    off = (CANVAS - ART) // 2
    canvas.paste(art, (off, off))

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    canvas.save(OUT, "WEBP", quality=90, method=6)
    print("wrote %s (%d bytes)" % (OUT, os.path.getsize(OUT)))

    if preview:
        # Preview: circle mask (strictest) and Samsung-like squircle at a few sizes
        def masked(size, shape):
            img = canvas.resize((size, size), Image.LANCZOS)
            m = Image.new("L", (size, size), 0)
            d = ImageDraw.Draw(m)
            # Visible area: the middle 72dp of 108dp
            pad = size * 18 // 108
            box = (pad, pad, size - pad, size - pad)
            if shape == "circle":
                d.ellipse(box, fill=255)
            else:
                d.rounded_rectangle(box, radius=(box[2] - box[0]) * 0.28, fill=255)
            out = Image.new("RGB", (size, size), (136, 136, 136))
            out.paste(img, (0, 0), m)
            return out.crop(box)

        tiles = [masked(540, "circle"), masked(540, "squircle"), masked(216, "squircle"), masked(108, "squircle")]
        sheet = Image.new("RGB", (sum(t.width for t in tiles) + 24 * 5, 400), (136, 136, 136))
        x = 24
        for t in tiles:
            sheet.paste(t, (x, 24))
            x += t.width + 24
        sheet.save(preview)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else None)
