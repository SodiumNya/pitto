"""
生成 Pitto 看板娘图标（原创角色）。
同一份图形定义同时输出：
  - 预览用 SVG（带圆形遮罩，模拟桌面上的样子）
  - 安卓自适应图标前景 app/src/main/res/drawable/ic_launcher_foreground.xml

用法：
  python -I tools/gen_icon.py <预览 svg 输出路径>
画布 108×108（自适应图标标准），可见安全区是以 (54,54) 为圆心、半径 33 的圆。
"""
import os
import sys

BG = "#D3F1E6"

HAIR = "#28485A"
HAIR_SHADE = "#1D3644"
SKIN = "#FFE7D6"
SKIN_SHADE = "#F6CDB8"
EYE = "#13706A"
EYE_DARK = "#0B3B3E"
EYE_LIGHT = "#5CC8B2"
PUPIL = "#06262A"
LASH = "#22303A"
BLUSH = "#FF8F9C"
MOUTH = "#C2525E"
UNIFORM = "#0E7C66"
COLLAR = "#FFFFFF"
RIBBON = "#FF8F9C"
CARD = "#1E9E5A"
ACCENT = "#7FD1B9"


def ellipse(cx, cy, rx, ry):
    return "M%g,%g a%g,%g 0 1,0 %g,0 a%g,%g 0 1,0 %g,0 Z" % (cx - rx, cy, rx, ry, rx * 2, rx, ry, -rx * 2)


# 每项: (path, fill, stroke, stroke_width, alpha)，group 项: ("group", rotation, pivotX, pivotY, [items])
def F(d, fill, alpha=1.0):
    return (d, fill, None, 0, alpha)


def S(d, stroke, width, alpha=1.0):
    return (d, None, stroke, width, alpha)


ITEMS = [
    # 后发（大头：头部占画面大部分）
    F("M54,20 C32,20 22,34 22,53 L22,80 C22,85 28,86 30,81 L33,74 L75,74 L78,81 C80,86 86,85 86,80 L86,53 C86,34 76,20 54,20 Z", HAIR_SHADE),
    # 小小的身体
    F("M37,95 C38,81 45,74.5 54,74.5 C63,74.5 70,81 71,95 Z", UNIFORM),
    F("M47.5,75 L54,81.5 L60.5,75 C58,74.2 50,74.2 47.5,75 Z", COLLAR),
    F("M54,79.6 L50,77.4 L50,81.8 Z M54,79.6 L58,77.4 L58,81.8 Z", RIBBON),
    F(ellipse(54, 79.6, 1.3, 1.3), RIBBON),
    F("M50.5,68 L50.5,75.2 C52.5,76 55.5,76 57.5,75.2 L57.5,68 Z", SKIN_SHADE),
    # 圆脸
    F("M32,52 C32,39 42,32 54,32 C66,32 76,39 76,52 C76,63 67,71 54,71 C41,71 32,63 32,52 Z", SKIN),
    # 大眼睛（左）：上深下浅的双色瞳孔 + 多层高光
    F(ellipse(44.5, 56, 5.8, 7.2), EYE_DARK),
    F(ellipse(44.5, 58.6, 4.6, 4.6), EYE),
    F(ellipse(44.5, 60.4, 3.2, 2.4), EYE_LIGHT),
    F(ellipse(44.5, 56.6, 2.2, 2.8), PUPIL),
    S("M38.2,54.6 C38.4,50.4 41,48.6 44.5,48.6 C48,48.6 50.6,50.4 50.8,54.4", LASH, 1.7),
    S("M38.4,52.8 L37,51.4", LASH, 1.3),
    F(ellipse(42.4, 53.4, 2.3, 2.6), "#FFFFFF"),
    F(ellipse(47, 60.4, 1.1, 1.1), "#FFFFFF"),
    F(ellipse(43.2, 61.4, 0.6, 0.6), "#FFFFFF", 0.8),
    # 大眼睛（右）
    F(ellipse(63.5, 56, 5.8, 7.2), EYE_DARK),
    F(ellipse(63.5, 58.6, 4.6, 4.6), EYE),
    F(ellipse(63.5, 60.4, 3.2, 2.4), EYE_LIGHT),
    F(ellipse(63.5, 56.6, 2.2, 2.8), PUPIL),
    S("M69.8,54.6 C69.6,50.4 67,48.6 63.5,48.6 C60,48.6 57.4,50.4 57.2,54.4", LASH, 1.7),
    S("M69.6,52.8 L71,51.4", LASH, 1.3),
    F(ellipse(61.4, 53.4, 2.3, 2.6), "#FFFFFF"),
    F(ellipse(66, 60.4, 1.1, 1.1), "#FFFFFF"),
    F(ellipse(62.2, 61.4, 0.6, 0.6), "#FFFFFF", 0.8),
    # 腮红（带三道小斜线）、小嘴
    F(ellipse(37.4, 63.6, 3.8, 2.1), BLUSH, 0.5),
    F(ellipse(70.6, 63.6, 3.8, 2.1), BLUSH, 0.5),
    S("M35.8,64.6 L36.8,62.6 M37.6,64.6 L38.6,62.6", BLUSH, 0.7, 0.8),
    S("M69,64.6 L70,62.6 M70.8,64.6 L71.8,62.6", BLUSH, 0.7, 0.8),
    F("M52,64.6 Q54,67.6 56,64.6 Z", MOUTH),
    # 刘海：圆润的发束
    F("M29,53 C28,33 40,23 54,23 C68,23 80,33 79,53 C77.5,49 75.5,46 73,44.5 C72.5,46.5 72,48 71.5,49.5 C69.5,45.5 67,42.5 64,40.5 C63,43 61.5,45.5 59.5,47 C58.5,43.5 56.5,40.5 54,38.5 C51.5,40.5 49.5,43.5 48.5,47 C46.5,45.5 45,43 44,40.5 C41,42.5 38.5,45.5 36.5,49.5 C36,48 35.5,46.5 35,44.5 C32.5,46 30.5,49 29,53 Z", HAIR),
    # 两侧鬓发
    F("M30,40 C28,56 29,68 33,79 L37,65 C36,58 36,51 36.5,42 Z", HAIR),
    F("M78,40 C80,56 79,68 75,79 L71,65 C72,58 72,51 71.5,42 Z", HAIR),
    # 头发高光
    S("M41,29.5 C45.5,26.6 51.5,26 56,26.6", "#FFFFFF", 1.6, 0.3),
    # NFC 发卡：三道弧
    S("M69,30.5 Q71.4,33 69,35.5", ACCENT, 1.7),
    S("M72,28.5 Q75.4,33 72,37.5", ACCENT, 1.7),
    S("M75,27 Q79.4,33 75,39", ACCENT, 1.7, 0.8),
    # 手里的交通卡
    ("group", -14, 70, 70, [
        F("M65.4,65.6 h9.2 a1.4,1.4 0 0 1 1.4,1.4 v6 a1.4,1.4 0 0 1 -1.4,1.4 h-9.2 a1.4,1.4 0 0 1 -1.4,-1.4 v-6 a1.4,1.4 0 0 1 1.4,-1.4 Z", CARD),
        F("M64,67.4 h12 v1.5 h-12 Z", "#FFFFFF", 0.45),
    ]),
    # 圆圆的小手
    F(ellipse(66, 73.4, 3, 2.6), SKIN),
]


def svg_item(it):
    if it[0] == "group":
        _, rot, px, py, items, *rest = it
        sc = rest[0] if rest else 1
        return ('<g transform="translate(%g %g) rotate(%g) scale(%g) translate(%g %g)">%s</g>'
                % (px, py, rot, sc, -px, -py, "".join(svg_item(i) for i in items)))
    d, fill, stroke, w, a = it
    if fill:
        return '<path d="%s" fill="%s" fill-opacity="%g"/>' % (d, fill, a)
    return '<path d="%s" fill="none" stroke="%s" stroke-width="%g" stroke-linecap="round" stroke-opacity="%g"/>' % (d, stroke, w, a)


def xml_item(it, indent="    "):
    if it[0] == "group":
        _, rot, px, py, items, *rest = it
        sc = rest[0] if rest else 1
        inner = "".join(xml_item(i, indent + "    ") for i in items)
        return ('%s<group android:rotation="%g" android:pivotX="%g" android:pivotY="%g" '
                'android:scaleX="%g" android:scaleY="%g">\n%s%s</group>\n'
                % (indent, rot, px, py, sc, sc, inner, indent))
    d, fill, stroke, w, a = it
    if fill:
        return '%s<path android:fillColor="%s" android:fillAlpha="%g" android:pathData="%s" />\n' % (indent, fill, a, d)
    return ('%s<path android:strokeColor="%s" android:strokeWidth="%g" android:strokeAlpha="%g" '
            'android:strokeLineCap="round" android:pathData="%s" />\n' % (indent, stroke, w, a, d))


# 整个角色缩到 90%，四周留出一点背景
SCENE = [("group", 0, 54, 60, ITEMS, 0.9)]


def main(preview):
    body = "".join(svg_item(i) for i in SCENE)
    svg = ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="432" height="432">'
           '<defs><clipPath id="c"><circle cx="54" cy="54" r="33"/></clipPath></defs>'
           '<g clip-path="url(#c)"><rect width="108" height="108" fill="%s"/>%s</g></svg>' % (BG, body))
    with open(preview, "w", encoding="utf-8") as f:
        f.write(svg)

    root = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "drawable")
    xml = ('<?xml version="1.0" encoding="utf-8"?>\n'
           '<!-- 由 tools/gen_icon.py 生成：Pitto 看板娘（原创角色） -->\n'
           '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
           '    android:width="108dp" android:height="108dp"\n'
           '    android:viewportWidth="108" android:viewportHeight="108">\n'
           + "".join(xml_item(i) for i in SCENE) + '</vector>\n')
    with open(os.path.join(root, "ic_launcher_foreground.xml"), "w", encoding="utf-8", newline="\n") as f:
        f.write(xml)
    with open(os.path.join(root, "ic_launcher_background.xml"), "w", encoding="utf-8", newline="\n") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n'
                '<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">\n'
                '    <solid android:color="%s" />\n</shape>\n' % BG)
    print("ok")


if __name__ == "__main__":
    main(sys.argv[1])
