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
    # 后发
    F("M54,22 C35,22 25,36 25,55 L25,83 C25,87 30,88 32,84 L36,76 L72,76 L76,84 C78,88 83,87 83,83 L83,55 C83,36 73,22 54,22 Z", HAIR_SHADE),
    # 身体（制服）+ 领子 + 蝴蝶结
    F("M33,90 C35,80 43,75 54,75 C65,75 73,80 75,90 Z", UNIFORM),
    F("M46,75.5 L54,84 L62,75.5 C59,74.5 49,74.5 46,75.5 Z", COLLAR),
    F("M54,81 L49.5,78.5 L49.5,84 Z M54,81 L58.5,78.5 L58.5,84 Z", RIBBON),
    F(ellipse(54, 81, 1.4, 1.4), RIBBON),
    # 脖子
    F("M49,68 L49,76 C51,77.5 57,77.5 59,76 L59,68 Z", SKIN_SHADE),
    # 脸
    F("M35,52 C35,39 44,33 54,33 C64,33 73,39 73,52 C73,63 64,72 54,72 C44,72 35,63 35,52 Z", SKIN),
    # 眼睛（左）
    F(ellipse(46, 56, 4.4, 5.6), EYE),
    F(ellipse(46, 57.6, 2.6, 3.2), EYE_DARK),
    S("M40.2,53.6 L41.6,54.8 C41.8,51.6 43.7,50.3 46,50.3 C48.3,50.3 50.2,51.6 50.4,54.6", LASH, 1.5),
    F(ellipse(44.6, 54, 1.7, 1.9), "#FFFFFF"),
    F(ellipse(47.6, 59, 0.8, 0.8), "#FFFFFF"),
    # 眼睛（右）
    F(ellipse(62, 56, 4.4, 5.6), EYE),
    F(ellipse(62, 57.6, 2.6, 3.2), EYE_DARK),
    S("M67.8,53.6 L66.4,54.8 C66.2,51.6 64.3,50.3 62,50.3 C59.7,50.3 57.8,51.6 57.6,54.6", LASH, 1.5),
    F(ellipse(60.6, 54, 1.7, 1.9), "#FFFFFF"),
    F(ellipse(63.6, 59, 0.8, 0.8), "#FFFFFF"),
    # 腮红、嘴
    F(ellipse(40.5, 63.2, 3.2, 1.7), BLUSH, 0.55),
    F(ellipse(67.5, 63.2, 3.2, 1.7), BLUSH, 0.55),
    S("M51.2,65.4 Q54,68 56.8,65.4", MOUTH, 1.3),
    # 刘海
    F("M32,53 C31,36 41,26 54,26 C67,26 77,36 76,53 C74.5,49.5 72.5,46.5 70.5,45 C70,47.5 69.5,49.5 69,51 C67.5,47 65.5,44 63,42 C62,44.5 60.5,46.5 58.5,48 C57.5,45 56,42 54,40 C52,42 50.5,45 49.5,48 C47.5,46.5 46,44.5 45,42 C42.5,44 40.5,47 39,51 C38.5,49.5 38,47.5 37.5,45 C35.5,46.5 33.5,49.5 32,53 Z", HAIR),
    # 两侧鬓发
    F("M34,41 C32,56 33,67 37,76 L41,63 C40,57 40,51 40.5,43 Z", HAIR),
    F("M74,41 C76,56 75,67 71,76 L67,63 C68,57 68,51 67.5,43 Z", HAIR),
    # 呆毛
    F("M53,27 C52,20.5 56,16 62,15.5 C58.5,18.5 57,22 56,27 Z", HAIR),
    # 头发高光
    S("M43.5,32 C47.5,29.6 52.5,29 56.5,29.8", "#FFFFFF", 1.6, 0.3),
    # NFC 发卡：三道弧
    S("M68.5,33 Q71,35.5 68.5,38", ACCENT, 1.7),
    S("M71.5,31 Q75,35.5 71.5,40", ACCENT, 1.7),
    S("M74.5,29.5 Q79,35.5 74.5,41.5", ACCENT, 1.7, 0.8),
    # 手里的交通卡
    ("group", -12, 76, 66, [
        F("M70.4,61.6 h11.2 a1.6,1.6 0 0 1 1.6,1.6 v5.6 a1.6,1.6 0 0 1 -1.6,1.6 h-11.2 a1.6,1.6 0 0 1 -1.6,-1.6 v-5.6 a1.6,1.6 0 0 1 1.6,-1.6 Z", CARD),
        F("M68.8,63.6 h14.4 v1.6 h-14.4 Z", "#FFFFFF", 0.45),
    ]),
    # 举卡的手（几根手指搭在卡片下沿）
    F("M68.6,70.6 C68.2,67.8 70.6,66.6 72.4,67.4 C74,68.2 74.2,70.6 73,72 C71.6,73.4 69,72.6 68.6,70.6 Z", SKIN),
    S("M70.2,68.4 L71.6,70.6", SKIN_SHADE, 0.7),
]


def svg_item(it):
    if it[0] == "group":
        _, rot, px, py, items = it
        return '<g transform="rotate(%g %g %g)">%s</g>' % (rot, px, py, "".join(svg_item(i) for i in items))
    d, fill, stroke, w, a = it
    if fill:
        return '<path d="%s" fill="%s" fill-opacity="%g"/>' % (d, fill, a)
    return '<path d="%s" fill="none" stroke="%s" stroke-width="%g" stroke-linecap="round" stroke-opacity="%g"/>' % (d, stroke, w, a)


def xml_item(it, indent="    "):
    if it[0] == "group":
        _, rot, px, py, items = it
        inner = "".join(xml_item(i, indent + "    ") for i in items)
        return ('%s<group android:rotation="%g" android:pivotX="%g" android:pivotY="%g">\n%s%s</group>\n'
                % (indent, rot, px, py, inner, indent))
    d, fill, stroke, w, a = it
    if fill:
        return '%s<path android:fillColor="%s" android:fillAlpha="%g" android:pathData="%s" />\n' % (indent, fill, a, d)
    return ('%s<path android:strokeColor="%s" android:strokeWidth="%g" android:strokeAlpha="%g" '
            'android:strokeLineCap="round" android:pathData="%s" />\n' % (indent, stroke, w, a, d))


def main(preview):
    body = "".join(svg_item(i) for i in ITEMS)
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
           + "".join(xml_item(i) for i in ITEMS) + '</vector>\n')
    with open(os.path.join(root, "ic_launcher_foreground.xml"), "w", encoding="utf-8", newline="\n") as f:
        f.write(xml)
    with open(os.path.join(root, "ic_launcher_background.xml"), "w", encoding="utf-8", newline="\n") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n'
                '<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">\n'
                '    <solid android:color="%s" />\n</shape>\n' % BG)
    print("ok")


if __name__ == "__main__":
    main(sys.argv[1])
