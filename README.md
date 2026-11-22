<p align="center">
  <img src="tools/art/mascot_source.png" width="160" alt="Pitto mascot">
</p>

<h1 align="center">Pitto</h1>

<p align="center">
  Check the balance and recent trips of Japanese transit IC cards (Suica, ICOCA, PASMO, ...) with your Android phone.<br>
  <a href="README.zh-CN.md">简体中文</a>
</p>

---

Tap a card on the back of your phone and Pitto shows its balance and trip history with station names.
It works fully offline and only needs the NFC permission.

It is especially handy for visitors to Japan: phones bought outside Japan usually cannot add Suica to Google Wallet,
but their NFC can still read a physical card.

## Features

- Balance and trip history (rides, buses, top-ups, purchases) with color-coded categories
- History beyond the card's 20-record limit: older records are kept on your phone each time you read the card
- Station names for rail rides, operator (and stop where known) for bus rides; optional English station names
- Detects the card type: Suica, ICOCA, PASMO, TOICA, manaca, Kitaca, nimoca, PiTaPa, SUGOCA, Hayakaken
- Remembers your cards; rename them and set your own card face (crop and rotate a photo)
- Sound and vibration feedback, live NFC on/off state
- English, 简体中文, 繁體中文, 日本語, 한국어, with an in-app language picker
- Light and dark theme; release APK about 1.4 MB

Pitto only reads data that the cards expose without encryption. It cannot top up or modify a card.

## Download

Get the APK from [Releases](../../releases). Requires Android 8.0+ and a phone with NFC.

Pitto has no network permission, so it never checks for updates by itself. **Settings → Check for updates**
opens the Releases page in your browser. For automatic update notifications, add this repository to
[Obtainium](https://github.com/ImranR98/Obtainium).

## Building

Requirements: JDK 17 and the Android SDK (platform 36). Set `JAVA_HOME` and `ANDROID_HOME`.

```sh
./gradlew assembleDebug          # app/build/outputs/apk/debug/
./gradlew installDebug           # install on a connected phone
./gradlew testDebugUnitTest      # unit tests (parser, card type detection, history merge)
```

Release builds are signed with the debug key unless `pitto.signing` in `local.properties` points to a
properties file with `storeFile`, `storePassword`, `keyAlias` and `keyPassword`.

## Project layout

```
app/src/main/java/app/jpcard/reader/
├── MainActivity.kt        NFC reader mode, lifecycle, feedback
├── Feedback.kt            synthesized chime + vibration
├── nfc/                   FeliCa commands and the CJRC record format
│   ├── FelicaReader.kt    Polling, Request Service, Read Without Encryption
│   ├── CardParser.kt      balance / history parsing, categories
│   ├── CardType.kt        card type detection by issuer-specific service codes
│   └── CardData.kt        data classes
├── data/                  storage and lookup tables
│   ├── CardStore.kt       saved cards (SharedPreferences, JSON)
│   ├── CardFaces.kt       custom card face images
│   ├── Stations.kt        station / bus stop lookup (assets/*.tsv)
│   └── AppLanguage.kt     in-app language selection
└── ui/                    Jetpack Compose screens; MainViewModel holds state and logic
tools/
├── gen_strings.py         single source of all UI text in every language
├── gen_stations.py        builds assets/stations.tsv and bus.tsv (English names from Wikidata)
└── gen_icon.py            builds the launcher icon from tools/art/mascot_source.png
```

To change UI text, edit `tools/gen_strings.py` and run `python tools/gen_strings.py app/src/main/res`.

## How it works

Transit IC cards in Japan are FeliCa cards using the common CJRC format. Balance and history live in services
that can be read without keys: history is service `0x090F` (20 blocks of 16 bytes), and the card type is detected
by asking (Request Service) which issuer-specific service codes exist. Station names are looked up by
area + line + station code; the area comes from the record itself, so a Suica used in Osaka resolves Kansai
stations correctly.

## Credits and license

- Station and bus stop data, and the issuer service code lists, come from
  [Metrodroid](https://github.com/metrodroid/metrodroid) (GPL-3.0-or-later), originally from FareBot and the
  IC SFCard Fan station code list.
- English station names come from [Wikidata](https://www.wikidata.org/) (CC0).
- Suica, ICOCA, PASMO and other card names are trademarks of their respective owners. Pitto is not affiliated
  with any of them and uses no official logos.

Pitto is free software under the [GNU General Public License v3.0 or later](LICENSE).
