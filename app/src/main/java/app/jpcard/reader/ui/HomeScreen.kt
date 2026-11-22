package app.jpcard.reader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.jpcard.reader.data.SavedCard
import app.jpcard.reader.nfc.CardType
import app.jpcard.reader.ui.theme.cardColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(state: AppState, actions: AppActions) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ReadingBar(state.reading)
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        "Pitto",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }
                item { StatusBanners(state, actions, showNfc = false) }
                item { HintCard(state, actions) }
                if (state.cards.isNotEmpty()) {
                    item {
                        Text(
                            "我的卡片",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp, start = 4.dp),
                        )
                    }
                    items(state.cards, key = { it.idm }) { card ->
                        SavedCardRow(card, state.faceVersion, Modifier.animateItem()) { actions.openCard(card.idm) }
                    }
                }
                item { AboutFooter() }
            }
        }
    }
}

@Composable
private fun HintCard(state: AppState, actions: AppActions) {
    val enabled = state.nfcStatus == NfcStatus.READY
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (enabled) 0.5f else 0.2f),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TapHint(reading = state.reading, enabled = enabled, errorTick = state.errorTick)
            Spacer(Modifier.height(12.dp))
            Text(
                when {
                    state.nfcStatus == NfcStatus.UNSUPPORTED -> "此设备不支持 NFC"
                    !enabled -> "NFC 已关闭"
                    state.reading -> "正在读取，请勿移开卡片"
                    state.cards.isEmpty() -> "将交通卡贴近手机背面以添加卡片"
                    else -> "将交通卡贴近手机背面以添加或更新"
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                when {
                    state.nfcStatus == NfcStatus.UNSUPPORTED -> "无法读取交通卡"
                    !enabled -> "开启 NFC 后即可读卡"
                    else -> "支持 Suica、ICOCA、PASMO 等交通系 IC 卡"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.nfcStatus == NfcStatus.DISABLED) {
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(onClick = actions.openNfcSettings) { Text("开启 NFC") }
            }
        }
    }
}

/** 小卡面：有自定义图片就显示缩略图，否则显示卡种配色 + 卡名。 */
@Composable
private fun MiniCard(card: SavedCard, faceVersion: Int) {
    val face = rememberCardFace(card.idm, faceVersion)
    CardFaceBox(
        type = card.data.type,
        face = face,
        shape = RoundedCornerShape(6.dp),
        scrim = false,
        modifier = Modifier.size(width = 54.dp, height = 34.dp),
    ) {
        if (face == null) {
            Text(
                card.data.type.label,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun SavedCardRow(card: SavedCard, faceVersion: Int, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            MiniCard(card, faceVersion)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(card.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "更新于 " + readTime(card.data.readAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(yen(card.data.balance), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** 主页底部：版本、版权与数据来源（站名数据为 GPL-3.0，需要注明出处）。 */
@Composable
private fun AboutFooter() {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }
    Column(
        Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Pitto $version · © 2026",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            "站名数据来自 Metrodroid（GPL-3.0）",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}
