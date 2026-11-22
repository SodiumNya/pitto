"""
把 FeliCa 车站数据库（SQLite）转换成 app 用的精简站名表。

数据来源：Metrodroid 项目的 data/felica_stations.db3
  https://github.com/metrodroid/metrodroid （GPL-3.0-or-later）
  该数据最初来自 FareBot 项目和 IC SFCard Fan 的站码表。

用法：
  python -I tools/gen_stations.py <felica_stations.db3> app/src/main/assets/stations.tsv

输出格式（UTF-8，制表符分隔，每行一个车站）：
  key(6 位十六进制: 地区<<16 | 线区<<8 | 站)  站名  公司  线路
"""
import sqlite3
import sys

COMPANY_SHORT = {
    "東日本旅客鉄道": "JR東日本",
    "西日本旅客鉄道": "JR西日本",
    "東海旅客鉄道": "JR東海",
    "北海道旅客鉄道": "JR北海道",
    "四国旅客鉄道": "JR四国",
    "九州旅客鉄道": "JR九州",
    "東京地下鉄": "東京メトロ",
    "東京都交通局": "都営",
    "大阪市高速電気軌道": "大阪メトロ",
    "大阪市交通局": "大阪市営",
}


def clean(s):
    return (s or "").replace("\t", " ").replace("\n", " ").strip()


def main(db_path, out_path):
    db = sqlite3.connect(db_path)
    rows = db.execute(
        "SELECT AreaCode, LineCode, StationCode, StationName, CompanyName, LineName "
        "FROM StationCode ORDER BY _id"
    )
    seen = {}
    for area, line, station, name, company, line_name in rows:
        try:
            a, l, s = int(area), int(line), int(station)
        except (TypeError, ValueError):
            continue
        if not (0 <= a <= 3 and 0 <= l <= 255 and 0 <= s <= 255):
            continue
        key = (a << 16) | (l << 8) | s
        if key in seen or not clean(name):
            continue
        company = clean(company)
        seen[key] = (clean(name), COMPANY_SHORT.get(company, company), clean(line_name))

    with open(out_path, "w", encoding="utf-8", newline="\n") as f:
        for key in sorted(seen):
            f.write("%06x\t%s\t%s\t%s\n" % ((key,) + seen[key]))
    print("wrote %d stations -> %s" % (len(seen), out_path))


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
