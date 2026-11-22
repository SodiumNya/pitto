"""
Single source of all UI text. Edit here, then run it to regenerate strings.xml for all languages.

Usage:
  python -I tools/gen_strings.py app/src/main/res

Each entry: key: (English (default), Simplified Chinese, Traditional Chinese, Japanese, Korean)
"""
import os
import sys
from xml.sax.saxutils import escape

RES = sys.argv[1]

# key: (en, zh-Hans, zh-Hant, ja, ko)
S = {
 "home_hint_add": ("Hold a transit card to the back of your phone to add it", "将交通卡贴近手机背面以添加卡片", "將交通卡貼近手機背面以新增卡片", "交通系ICカードをスマホの背面にかざして追加", "교통카드를 휴대폰 뒷면에 대면 추가돼요"),
 "home_hint_add_or_update": ("Hold a transit card to the back of your phone to add or update it", "将交通卡贴近手机背面以添加或更新", "將交通卡貼近手機背面以新增或更新", "交通系ICカードをスマホの背面にかざして追加・更新", "교통카드를 휴대폰 뒷면에 대면 추가·갱신돼요"),
 "home_reading": ("Reading… keep the card still", "正在读取，请勿移开卡片", "正在讀取，請勿移開卡片", "読み取り中…カードを離さないでください", "읽는 중… 카드를 떼지 마세요"),
 "nfc_off": ("NFC is off", "NFC 已关闭", "NFC 已關閉", "NFCがオフです", "NFC가 꺼져 있어요"),
 "nfc_unsupported_title": ("NFC not supported", "此设备不支持 NFC", "此裝置不支援 NFC", "NFC非対応の端末です", "NFC를 지원하지 않는 기기예요"),
 "home_sub_supported": ("Works with Suica, ICOCA, PASMO and other Japanese transit IC cards", "支持 Suica、ICOCA、PASMO 等交通系 IC 卡", "支援 Suica、ICOCA、PASMO 等交通系 IC 卡", "Suica・ICOCA・PASMOなどの交通系ICカードに対応", "Suica, ICOCA, PASMO 등 일본 교통 IC 카드 지원"),
 "home_sub_nfc_off": ("Turn on NFC to read cards", "开启 NFC 后即可读卡", "開啟 NFC 後即可讀卡", "NFCをオンにすると読み取れます", "NFC를 켜면 카드를 읽을 수 있어요"),
 "home_sub_unsupported": ("This device can't read transit cards", "无法读取交通卡", "無法讀取交通卡", "交通系ICカードを読み取れません", "교통카드를 읽을 수 없어요"),
 "turn_on_nfc": ("Turn on NFC", "开启 NFC", "開啟 NFC", "NFCをオンにする", "NFC 켜기"),
 "my_cards": ("My cards", "我的卡片", "我的卡片", "マイカード", "내 카드"),
 "updated_at": ("Updated %1$s", "更新于 %1$s", "更新於 %1$s", "%1$s 更新", "%1$s 업데이트"),
 "footer_data": ("Station data from Metrodroid (GPL-3.0)", "站名数据来自 Metrodroid（GPL-3.0）", "站名資料來自 Metrodroid（GPL-3.0）", "駅名データ：Metrodroid（GPL-3.0）", "역 이름 데이터: Metrodroid(GPL-3.0)"),
 "action_turn_on": ("Turn on", "去开启", "前往開啟", "オンにする", "켜기"),
 "banner_unsupported": ("This device doesn't support NFC and can't read cards", "此设备不支持 NFC，无法读卡", "此裝置不支援 NFC，無法讀卡", "この端末はNFCに対応していないため、読み取れません", "이 기기는 NFC를 지원하지 않아 카드를 읽을 수 없어요"),
 "close": ("Close", "关闭", "關閉", "閉じる", "닫기"),
 "back": ("Back", "返回", "返回", "戻る", "뒤로"),
 "edit_card": ("Edit card", "编辑卡片", "編輯卡片", "カードを編集", "카드 편집"),
 "delete": ("Delete", "删除", "刪除", "削除", "삭제"),
 "cancel": ("Cancel", "取消", "取消", "キャンセル", "취소"),
 "recent_records": ("Recent activity", "最近记录", "最近紀錄", "利用履歴", "최근 이용 내역"),
 "no_records": ("No records yet", "暂无记录", "尚無紀錄", "履歴はありません", "이용 내역이 없어요"),
 "records_limit_note": ("The card only keeps its 20 most recent records", "卡片仅保存最近 20 条记录", "卡片僅保存最近 20 筆紀錄", "カードには直近20件の履歴のみ保存されています", "카드에는 최근 20건만 저장돼요"),
 "delete_title": ("Delete this card?", "删除这张卡？", "刪除這張卡？", "このカードを削除しますか？", "이 카드를 삭제할까요?"),
 "delete_message": ("Removes this card's records and card face from this phone. The card itself is not affected.", "将从本机移除这张卡的记录和卡面，不影响卡片本身。", "將從本機移除這張卡的紀錄和卡面，不影響卡片本身。", "この端末から履歴とカード画像を削除します。カード自体には影響しません。", "이 휴대폰에서 이용 내역과 카드 이미지가 삭제돼요. 카드 자체에는 영향이 없어요."),
 "balance_label": ("%1$s · Balance", "%1$s · 余额", "%1$s · 餘額", "%1$s・残高", "%1$s · 잔액"),
 "just_updated": ("Updated", "已更新", "已更新", "更新しました", "업데이트됨"),
 "balance_after": ("Balance %1$s", "余额 %1$s", "餘額 %1$s", "残高 %1$s", "잔액 %1$s"),
 "choose_face": ("Choose an image", "选择卡面图片", "選擇卡面圖片", "画像を選択", "이미지 선택"),
 "change_face": ("Change image", "更换图片", "更換圖片", "画像を変更", "이미지 변경"),
 "restore_default": ("Reset", "恢复默认", "恢復預設", "元に戻す", "기본값으로"),
 "face_hint": ("You can adjust position and rotation after choosing. Images stay on this phone.", "选择图片后可调整位置和方向，图片仅保存在本机。", "選擇圖片後可調整位置和方向，圖片僅保存在本機。", "選択後に位置や向きを調整できます。画像はこの端末にのみ保存されます。", "선택한 뒤 위치와 방향을 조정할 수 있어요. 이미지는 이 휴대폰에만 저장돼요."),
 "name": ("Name", "名称", "名稱", "名前", "이름"),
 "done": ("Done", "完成", "完成", "完了", "완료"),
 "crop_hint": ("Drag or pinch to adjust", "拖动或双指缩放以调整位置", "拖曳或雙指縮放以調整位置", "ドラッグやピンチで位置を調整", "드래그하거나 손가락으로 확대해 조정하세요"),
 "rotate": ("Rotate", "旋转", "旋轉", "回転", "회전"),
 "ok": ("OK", "确定", "確定", "OK", "확인"),
 "today_at": ("Today %1$s", "今天 %1$s", "今天 %1$s", "今日 %1$s", "오늘 %1$s"),
 "card_type_unknown": ("IC card", "交通系IC", "交通系IC", "交通系IC", "교통 IC"),
 "err_not_felica": ("Couldn't recognize this card. Please use a transit IC card.", "无法识别这张卡，请使用交通系 IC 卡", "無法識別這張卡，請使用交通系 IC 卡", "このカードを認識できません。交通系ICカードをご利用ください。", "이 카드를 인식할 수 없어요. 교통 IC 카드를 사용해 주세요."),
 "err_not_transit": ("This isn't a transit IC card.", "这张卡不是交通系 IC 卡", "這張卡不是交通系 IC 卡", "このカードは交通系ICカードではありません。", "교통 IC 카드가 아니에요."),
 "err_tag_lost": ("Reading was interrupted. Hold the card still and try again.", "读取中断，请将卡片贴稳后重试", "讀取中斷，請將卡片貼穩後重試", "読み取りが中断されました。カードを動かさずにもう一度お試しください。", "읽기가 중단됐어요. 카드를 고정한 채 다시 시도해 주세요."),
 "err_read_failed": ("Couldn't read the card. Hold it still and try again.", "读取失败，请将卡片贴稳后重试", "讀取失敗，請將卡片貼穩後重試", "読み取りに失敗しました。カードを動かさずにもう一度お試しください。", "읽지 못했어요. 카드를 고정한 채 다시 시도해 주세요."),
 "err_image": ("Couldn't open this image. Please choose another.", "无法读取这张图片，请换一张", "無法讀取這張圖片，請換一張", "この画像は使用できません。別の画像を選んでください。", "이 이미지는 사용할 수 없어요. 다른 이미지를 선택해 주세요."),
 "err_face_save": ("Couldn't save the card face. Please try again.", "卡面保存失败，请重试", "卡面儲存失敗，請重試", "カード画像を保存できませんでした。もう一度お試しください。", "카드 이미지를 저장하지 못했어요. 다시 시도해 주세요."),
 "settings": ("Settings", "设置", "設定", "設定", "설정"),
 "language": ("Language", "语言", "語言", "言語", "언어"),
 "language_system": ("System default", "跟随系统", "跟隨系統", "システムの設定", "시스템 설정"),
 # History categories (label + one-character badge)
 "kind_train": ("Train", "乘车", "乘車", "乗車", "승차"),
 "kind_bus": ("Bus", "巴士", "巴士", "バス", "버스"),
 "kind_shopping": ("Shopping", "购物", "購物", "物販", "구매"),
 "kind_charge": ("Top-up", "充值", "儲值", "チャージ", "충전"),
 "kind_other": ("Other", "其他", "其他", "その他", "기타"),
 "badge_train": ("T", "乘", "乘", "乗", "승"),
 "badge_bus": ("B", "巴", "巴", "バ", "버"),
 "badge_shopping": ("S", "购", "購", "買", "구"),
 "badge_charge": ("+", "充", "儲", "チ", "충"),
 "badge_other": ("•", "其", "其", "他", "기"),
 # Process types
 "proc_ride": ("Train ride", "乘车", "乘車", "乗車", "승차"),
 "proc_charge": ("Top-up", "充值", "儲值", "チャージ", "충전"),
 "proc_ticket": ("Ticket purchase", "购票", "購票", "切符購入", "승차권 구매"),
 "proc_adjust": ("Fare adjustment", "补票", "補票", "精算", "정산"),
 "proc_entry_adjust": ("Entry fare adjustment", "进站补票", "進站補票", "入場精算", "입장 정산"),
 "proc_window": ("Ticket window", "窗口处理", "窗口處理", "窓口処理", "창구 처리"),
 "proc_new_card": ("New card", "新卡", "新卡", "新規発行", "신규 발급"),
 "proc_deduction": ("Deduction", "扣除", "扣除", "控除", "공제"),
 "proc_bus": ("Bus", "巴士", "巴士", "バス", "버스"),
 "proc_reissue": ("Card reissue", "补发卡", "補發卡", "再発行", "재발급"),
 "proc_shinkansen": ("Shinkansen", "新干线", "新幹線", "新幹線", "신칸센"),
 "proc_auto_charge": ("Auto top-up", "自动充值", "自動儲值", "オートチャージ", "자동 충전"),
 "proc_bus_charge": ("Bus top-up", "巴士充值", "巴士儲值", "バスチャージ", "버스 충전"),
 "proc_bus_ticket": ("Bus ticket", "巴士购票", "巴士購票", "バス券購入", "버스 승차권 구매"),
 "proc_shopping": ("Purchase", "购物", "購物", "物販", "구매"),
 "proc_point_charge": ("Point top-up", "积分充值", "點數儲值", "ポイントチャージ", "포인트 충전"),
 "proc_register_charge": ("Top-up at register", "收银台充值", "收銀台儲值", "レジチャージ", "계산대 충전"),
 "proc_shopping_cancel": ("Purchase canceled", "购物取消", "購物取消", "物販取消", "구매 취소"),
 "proc_station_shopping": ("In-station purchase", "站内购物", "站內購物", "入場物販", "역내 구매"),
 "proc_adjust_other": ("Fare adjustment (other operator)", "补票（他社）", "補票（他社）", "精算（他社）", "정산(타사)"),
 "proc_entry_adjust_other": ("Entry adjustment (other operator)", "进站补票（他社）", "進站補票（他社）", "入場精算（他社）", "입장 정산(타사)"),
 "proc_other": ("Other", "其他", "其他", "その他", "기타"),
}

DIRS = ["values", "values-zh", "values-b+zh+Hant", "values-ja", "values-ko"]


def android_escape(s):
    s = escape(s)
    return s.replace("'", "\\'").replace('"', '\\"')


for idx, d in enumerate(DIRS):
    path = os.path.join(RES, d, "strings.xml")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
    if idx == 0:
        lines.append('    <string name="app_name" translatable="false">Pitto</string>')
    for k, v in S.items():
        lines.append('    <string name="%s">%s</string>' % (k, android_escape(v[idx])))
    lines.append("</resources>")
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")
    print(path, len(S))
