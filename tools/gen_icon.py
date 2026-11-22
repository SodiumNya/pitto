"""
把看板娘插画（tools/art/mascot_source.png）转换成安卓自适应图标。

自适应图标是 108dp 的正方形，系统会按手机的图标形状裁掉外圈，只露出中间约 72dp。
所以：
  1. 裁掉原图四角的白色圆角
  2. 把画面缩放到 64dp（三星桌面实际只露出中间约 64-66dp），完整保留原图构图
  3. 外圈用原图边缘的平均色补满，作为保险
输出 app/src/main/res/drawable-nodpi/ic_launcher_art.webp（432×432，相当于 xxxhdpi 的 108dp）。

用法：
  python -I tools/gen_icon.py [预览 png 输出路径]
"""
import os
import sys

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
SRC = os.path.join(ROOT, "tools", "art", "mascot_source.png")
OUT = os.path.join(ROOT, "app", "src", "main", "res", "drawable-nodpi", "ic_launcher_art.webp")

CANVAS = 432          # 108dp @ xxxhdpi
ART = CANVAS * 64 // 108   # 三星等桌面的遮罩比标准 72dp 可见区还小一圈，留足余量完整保留原图构图
CORNER_INSET = 72     # 原图四角白色圆角沿对角线约 67px


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
        # 预览：圆形遮罩（最严格）与三星的圆角方形，各两种尺寸
        def masked(size, shape):
            img = canvas.resize((size, size), Image.LANCZOS)
            m = Image.new("L", (size, size), 0)
            d = ImageDraw.Draw(m)
            # 可见区：108dp 中间 72dp
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
