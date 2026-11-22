"""
Converts the FeliCa station database (SQLite) into the compact tables the app ships.

Source: data/felica_stations.db3 from Metrodroid
  https://github.com/metrodroid/metrodroid (GPL-3.0-or-later)
  originally from FareBot and the IC SFCard Fan station code list.

Usage:
  python -I tools/gen_stations.py <felica_stations.db3> app/src/main/assets

Output (UTF-8, tab-separated):
  stations.tsv  rail:  key (6 hex digits: area<<16 | line<<8 | station)  name  operator  line
  bus.tsv       bus:   key (8 hex digits: operator<<16 | stop)  stop name  operator
                Stop 0 means only the operator is known (listed as "共通" in the source).
"""
import os
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


def main(db_path, out_dir):
    db = sqlite3.connect(db_path)
    write_rail(db, os.path.join(out_dir, "stations.tsv"))
    write_bus(db, os.path.join(out_dir, "bus.tsv"))


def write_bus(db, out_path):
    rows = db.execute(
        "SELECT LineCode, StationCode, StationName, CompanyName FROM IruCaStationCode ORDER BY _id"
    )
    seen = {}
    for op, stop, name, company in rows:
        try:
            o, s = int(op, 16), int(stop, 16)
        except (TypeError, ValueError):
            continue
        if not (0 <= o <= 0xFFFF and 0 <= s <= 0xFFFF):
            continue
        key = (o << 16) | s
        if key in seen:
            continue
        name = clean(name)
        if s == 0 or name == "共通":
            name = ""
        seen[key] = (name, clean(company))
    with open(out_path, "w", encoding="utf-8", newline="\n") as f:
        for key in sorted(seen):
            f.write("%08x\t%s\t%s\n" % ((key,) + seen[key]))
    print("wrote %d bus stops -> %s" % (len(seen), out_path))


def write_rail(db, out_path):
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
