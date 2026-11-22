<p align="center">
  <img src="tools/art/mascot_source.png" width="160" alt="Pitto 看板娘">
</p>

<h1 align="center">Pitto</h1>

<p align="center">
  用安卓手机查看日本交通系 IC 卡（Suica、ICOCA、PASMO 等）的余额和最近记录。<br>
  <a href="README.md">English</a>
</p>

---

把卡贴在手机背面，Pitto 就会显示余额和乘车记录，并显示站名。完全离线使用，只需要 NFC 权限。

特别适合来日本旅行的人：在海外购买的手机一般无法把 Suica 加入 Google 钱包，但手机的 NFC 仍然可以读取实体卡。

## 功能

- 余额和乘车记录（乘车、巴士、充值、购物），按类别用颜色区分
- 突破卡内 20 条的限制：每次读卡时，更早的记录会保留在手机上
- 乘车记录显示站名，巴士记录显示巴士公司（部分地区可显示站点）；可在设置中开启英文站名
- 自动识别卡种：Suica、ICOCA、PASMO、TOICA、manaca、Kitaca、nimoca、PiTaPa、SUGOCA、はやかけん
- 记住读过的卡；可以重命名，也可以用照片自定义卡面（支持裁剪和旋转）
- 读卡成功/失败有提示音和震动，NFC 开关状态实时提示
- 支持 English、简体中文、繁體中文、日本語、한국어，可在 app 内切换语言
- 浅色/深色主题；release 安装包约 1.4 MB

Pitto 只读取卡片公开（无需加密）的数据，不能充值或修改卡片。

## 下载

在 [Releases](../../releases) 下载 APK。需要 Android 8.0 以上、带 NFC 的手机。

Pitto 没有联网权限，不会自己检查更新。**设置 → 检查更新** 会用浏览器打开 Releases 页面。
如果想自动收到更新提醒，可以把本仓库添加到 [Obtainium](https://github.com/ImranR98/Obtainium)。

## 编译

需要 JDK 17 和 Android SDK（platform 36），并设置 `JAVA_HOME`、`ANDROID_HOME`。

```sh
./gradlew assembleDebug          # 输出在 app/build/outputs/apk/debug/
./gradlew installDebug           # 安装到已连接的手机
./gradlew testDebugUnitTest      # 单元测试（记录解析、卡种识别、记录合并）
```

release 版本默认用 debug 密钥签名；如果 `local.properties` 里的 `pitto.signing` 指向一个包含
`storeFile`、`storePassword`、`keyAlias`、`keyPassword` 的 properties 文件，则使用该密钥签名。

## 代码结构

见 [English README](README.md#project-layout)。修改界面文字：编辑 `tools/gen_strings.py`，然后运行
`python tools/gen_strings.py app/src/main/res`。

## 致谢与许可证

- 站名、巴士站数据以及各发卡方的服务码清单来自 [Metrodroid](https://github.com/metrodroid/metrodroid)
  （GPL-3.0-or-later），最初源自 FareBot 与 IC SFCard Fan 的站码表。
- 英文站名来自 [Wikidata](https://www.wikidata.org/)（CC0）。
- Suica、ICOCA、PASMO 等卡名是各自所有者的商标。Pitto 与这些公司没有任何关系，也不使用任何官方标志。

Pitto 是自由软件，采用 [GNU 通用公共许可证 v3.0 或更高版本](LICENSE)。
