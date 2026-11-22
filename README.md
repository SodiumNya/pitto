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
- `data/Stations.kt` + `assets/stations.tsv`：站名表，用“地区 + 线区 + 站”三个码查询

## 站名数据

`app/src/main/assets/stations.tsv`（铁路）和 `bus.tsv`（巴士）由 `tools/gen_stations.py` 生成，数据来自
[Metrodroid](https://github.com/metrodroid/metrodroid) 的 `data/felica_stations.db3`（GPL-3.0-or-later，
最初源自 FareBot 与 IC SFCard Fan 的站码表）。自用没有问题；如果要公开发布这个 app，需要遵守 GPL-3.0（公开源码并采用同一许可证）。

重新生成：

```powershell
python -I tools\gen_stations.py <felica_stations.db3 路径> app\src\main\assets
```
- `ui/`：Compose 界面
