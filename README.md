# Pitto（交通卡余额）

读取 Suica / ICOCA 余额和最近 20 条记录的安卓 app。Kotlin + Jetpack Compose，离线使用，只需要 NFC 权限。

## 环境（都在 F 盘）

| 内容 | 位置 |
|---|---|
| Android SDK | `F:\dev\android-sdk` |
| Gradle 缓存 | `F:\dev\gradle-home` |
| adb 配置 | `F:\dev\android-user` |
| Gradle 安装包 | `F:\dev\_downloads\gradle-8.14.3-bin.zip`（wrapper 从这里解压，不要删） |
| JDK 17 | 复用已有的 `C:\Program Files\Java\jdk-17` |

## 在 VS Code 里使用

`Ctrl+Shift+B` → 「安装到手机并启动 (Debug)」。其他任务在 `终端 → 运行任务` 里。

手机准备：设置里打开「开发者选项 → USB 调试」，用 USB 连接电脑，手机上点「允许」。

## 代码结构

- `nfc/FelicaReader.kt`：FeliCa 底层通信（Polling、Read Without Encryption）
- `nfc/CardParser.kt`：CJRC 格式解析（余额、记录、处理种类）
- `data/CardStore.kt`：本机保存读过的卡
- `data/Stations.kt` + `assets/stations.tsv` / `bus.tsv`：站名表和巴士表
- `data/CardFaces.kt`：自定义卡面
- `ui/`：Compose 界面

## 多语言

界面支持英语（默认/兜底）、简体中文、繁体中文、日语、韩语，跟随系统语言；
Android 13+ 可在系统设置里单独给 Pitto 选语言。文字在 `app/src/main/res/values*/strings.xml`。
站名、巴士公司名保留日文原名；Suica 等卡名为专有名词不翻译。

## 签名

debug 和 release 都用正式密钥签名（两者可以互相覆盖安装）：

| 内容 | 位置 |
|---|---|
| 密钥库 | `F:\dev\keys\pitto-release.jks` |
| 密码等配置 | `F:\dev\keys\pitto-signing.properties` |
| 引用方式 | `local.properties` 里的 `pitto.signing=...`（不进 git） |

**务必备份 `F:\dev\keys` 整个文件夹**（例如网盘或 U 盘）。丢了密钥就无法再给已安装的 app 升级，只能卸载重装。
找不到这些文件时会自动退回 debug 签名，不影响编译。

## 站名数据

`app/src/main/assets/stations.tsv`（铁路）和 `bus.tsv`（巴士）由 `tools/gen_stations.py` 生成，数据来自
[Metrodroid](https://github.com/metrodroid/metrodroid) 的 `data/felica_stations.db3`（GPL-3.0-or-later，
最初源自 FareBot 与 IC SFCard Fan 的站码表）。自用没有问题；如果要公开发布这个 app，需要遵守 GPL-3.0（公开源码并采用同一许可证）。

重新生成：

```powershell
python -I tools\gen_stations.py <felica_stations.db3 路径> app\src\main\assets
```
