"""
Converts the FeliCa station database (SQLite) into the compact tables the app ships,
adding English station names.

Sources:
  - data/felica_stations.db3 from Metrodroid
    https://github.com/metrodroid/metrodroid (GPL-3.0-or-later),
    originally from FareBot and the IC SFCard Fan station code list.
  - Optional: Wikidata station labels (CC0) for better English names. Download them with the
    SPARQL query in WIKIDATA_QUERY (one file per station type, saved as JSON results).

Usage:
  python -I tools/gen_stations.py <felica_stations.db3> app/src/main/assets [wikidata_json_dir]

Output (UTF-8, tab-separated):
  stations.tsv  key (6 hex digits: area<<16 | line<<8 | station)  name  operator  line  english name
  bus.tsv       key (8 hex digits: operator<<16 | stop)  stop name  operator  english operator
                Operator names appear on each operator's first row only.
                Stop 0 means only the operator is known (listed as "共通" in the source).

English rail names come from Wikidata, matched on the Japanese name and, when the name is
ambiguous, the operator. Without a match the database's romanization is used. Bus stop names are
not translated: the database's English stop names are machine translations and often wrong.
"""
import collections
import glob
import json
import os
import re
import sqlite3
import sys
import unicodedata

WIKIDATA_QUERY = """
SELECT ?s ?ja ?en ?opja WHERE {
  ?s wdt:P31 wd:<TYPE>; wdt:P17 wd:Q17.
  ?s rdfs:label ?ja FILTER(LANG(?ja) = "ja")
  ?s rdfs:label ?en FILTER(LANG(?en) = "en")
  OPTIONAL { ?s wdt:P137 ?op. ?op rdfs:label ?opja FILTER(LANG(?opja) = "ja") }
}
"""
# Station types queried: railway station, metro station, tram stop, light rail station, monorail station
WIKIDATA_TYPES = ["Q55488", "Q928830", "Q2175765", "Q1793804", "Q4663385"]

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

# Operators renamed since the station list was made (old name -> current name on Wikidata)
OPERATOR_ALIASES = {
    "大阪市交通局": "大阪市高速電気軌道",
    "営団地下鉄": "東京地下鉄",
}


# Hand fixes for the few names neither Wikidata nor the database romanize correctly.
ENGLISH_OVERRIDES = {
    "先端医療センター前": "Iryo Center",
    "京コンピュータ前": "Keisan Kagaku Center",
}


def clean(s):
    return (s or "").replace("\t", " ").replace("\n", " ").strip()


def strip_macrons(s):
    """Tōkyō -> Tokyo, matching Google Maps and most station signs."""
    return "".join(c for c in unicodedata.normalize("NFD", s) if unicodedata.category(c) != "Mn")


def norm_ja(label):
    label = re.sub(r"[（(].*?[)）]", "", label).strip()
    return label[:-1] if label.endswith("駅") and len(label) > 1 else label


def norm_en(label):
    label = re.sub(r"\s*\(.*?\)", "", label).strip()
    label = re.sub(r"\s+[Ss]tation$", "", label).strip()
    return strip_macrons(label)


def fix_romanization(name):
    """Cleans up the database's own romanization, used when Wikidata has no match."""
    name = strip_macrons(name)
    return name.replace("dzu", "zu").replace("Dzu", "Zu").replace("dzi", "ji")


def load_wikidata(directory):
    candidates = collections.defaultdict(list)  # Japanese name -> [(english, operator)]
    if not directory:
        return candidates
    for path in glob.glob(os.path.join(directory, "*.json")):
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        for b in data["results"]["bindings"]:
            ja, en = norm_ja(b["ja"]["value"]), norm_en(b["en"]["value"])
            op = b.get("opja", {}).get("value", "")
            if ja and en and not re.search(r"[぀-ヿ一-鿿]", en):
                candidates[ja].append((en, op))
    return candidates


def same_operator(a, b):
    a, b = OPERATOR_ALIASES.get(a, a), OPERATOR_ALIASES.get(b, b)
    return bool(a and b) and (a in b or b in a)


def english_name(name, company, fallback, candidates, stats):
    if name in ENGLISH_OVERRIDES:
        stats["manual override"] += 1
        return ENGLISH_OVERRIDES[name]
    cands = candidates.get(name, [])
    names = {en for en, _ in cands}
    if len(names) == 1:
        stats["wikidata"] += 1
        return names.pop()
    if names:
        by_op = {en for en, op in cands if same_operator(op, company)}
        if len(by_op) == 1:
            stats["wikidata (by operator)"] += 1
            return by_op.pop()
    stats["database romanization"] += 1
    return fix_romanization(fallback)


def main(db_path, out_dir, wikidata_dir=None):
    db = sqlite3.connect(db_path)
    write_rail(db, os.path.join(out_dir, "stations.tsv"), load_wikidata(wikidata_dir))
    write_bus(db, os.path.join(out_dir, "bus.tsv"))


def write_bus(db, out_path):
    rows = db.execute(
        "SELECT LineCode, StationCode, StationName, CompanyName, CompanyName_en FROM IruCaStationCode ORDER BY _id"
    )
    seen = {}
    for op, stop, name, company, company_en in rows:
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
        seen[key] = (name, clean(company), re.sub(r"\bbus\b", "Bus", strip_macrons(clean(company_en))))
    with open(out_path, "w", encoding="utf-8", newline="\n") as f:
        named = set()  # operator names are written on each operator's first row only
        for key in sorted(seen):
            name, company, company_en = seen[key]
            op = key >> 16
            if op in named:
                company, company_en = "", ""
            named.add(op)
            f.write("%08x\t%s\t%s\t%s\n" % (key, name, company, company_en))
    print("wrote %d bus stops -> %s" % (len(seen), out_path))


def write_rail(db, out_path, candidates):
    rows = db.execute(
        "SELECT AreaCode, LineCode, StationCode, StationName, CompanyName, LineName, StationName_en "
        "FROM StationCode ORDER BY _id"
    )
    seen = {}
    stats = collections.Counter()
    for area, line, station, name, company, line_name, name_en in rows:
        try:
            a, l, s = int(area), int(line), int(station)
        except (TypeError, ValueError):
            continue
        if not (0 <= a <= 3 and 0 <= l <= 255 and 0 <= s <= 255):
            continue
        key = (a << 16) | (l << 8) | s
        name = clean(name)
        if key in seen or not name:
            continue
        company = clean(company)
        en = english_name(name, company, clean(name_en), candidates, stats)
        seen[key] = (name, COMPANY_SHORT.get(company, company), clean(line_name), en)

    with open(out_path, "w", encoding="utf-8", newline="\n") as f:
        for key in sorted(seen):
            f.write("%06x\t%s\t%s\t%s\t%s\n" % ((key,) + seen[key]))
    print("wrote %d stations -> %s" % (len(seen), out_path))
    for source, n in stats.most_common():
        print("  English names from %s: %d" % (source, n))


if __name__ == "__main__":
    main(*sys.argv[1:4])
