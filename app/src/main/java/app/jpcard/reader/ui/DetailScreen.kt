package app.jpcard.reader.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Shadow
import app.jpcard.reader.data.CardFaces
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.jpcard.reader.data.SavedCard
import app.jpcard.reader.nfc.HistoryEntry
import app.jpcard.reader.ui.theme.IncomeColor
import app.jpcard.reader.ui.theme.cardColor
import app.jpcard.reader.ui.theme.kindColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(card: SavedCard, state: AppState, actions: AppActions) {
    var editing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(card.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = actions.back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
                actions = {
                    IconButton(onClick = { editing = true }) { Icon(Icons.Filled.Edit, "编辑卡片") }
                    IconButton(onClick = { deleting = true }) { Icon(Icons.Filled.Delete, "删除") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ReadingBar(state.reading)
            LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
                item { StatusBanners(state, actions) }
                item { BalanceHero(card, state) }
                item {
                    Text(
                        "最近记录",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp, start = 4.dp),
                    )
                }
                if (card.data.history.isEmpty()) {
                    item {
                        Text(
                            "暂无记录",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                itemsIndexed(card.data.history, key = { i, e -> "${e.serial}-$i" }) { i, e ->
                    HistoryRow(e, state.stationsReady)
                    if (i < card.data.history.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
                if (card.data.history.isNotEmpty()) {
                    item {
                        Text(
                            "卡片仅保存最近 20 条记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (editing) EditCardSheet(card, state, actions) { editing = false }
    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text("删除这张卡？") },
            text = { Text("将从本机移除这张卡的记录和卡面，不影响卡片本身。") },
            confirmButton = {
                TextButton(onClick = { deleting = false; actions.delete(card.idm) }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text("取消") } },
        )
    }
}

/**
 * 余额卡片，按真实卡片比例显示。有自定义卡面就用图片，否则用卡种配色。
 * 刚读卡成功时：整块轻微弹一下，并短暂显示“✓ 已更新”。
 */
@Composable
private fun BalanceHero(card: SavedCard, state: AppState) {
    val scale = remember { Animatable(1f) }
    var justUpdated by remember { mutableStateOf(false) }
    LaunchedEffect(state.readTick) {
        if (state.readTick > state.consumedTick && state.readIdm == card.idm) {
            state.consumedTick = state.readTick
            justUpdated = true
            scale.snapTo(0.94f)
            scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
            delay(2200)
            justUpdated = false
        }
    }
    val face = rememberCardFace(card.idm, state.faceVersion)
    val shadow = if (face != null) Shadow(Color.Black.copy(alpha = 0.5f), blurRadius = 8f) else null

    CardFaceBox(
        type = card.data.type,
        face = face,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .aspectRatio(CardFaces.RATIO),
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            // 与最初版本一样的紧凑排版：卡名 · 余额 / 金额 / 读取时间
            Column(Modifier.align(Alignment.CenterStart).padding(horizontal = 24.dp)) {
                Text(
                    card.data.type.label + " · 余额",
                    style = MaterialTheme.typography.labelLarge.copy(shadow = shadow),
                )
                Text(
                    yen(card.data.balance),
                    style = MaterialTheme.typography.displaySmall.copy(shadow = shadow),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                AnimatedContent(
                    targetState = justUpdated,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "updated",
                ) { updated ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (updated) {
                            Icon(Icons.Filled.CheckCircle, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "已更新",
                                style = MaterialTheme.typography.bodySmall.copy(shadow = shadow),
                                fontWeight = FontWeight.SemiBold,
                            )
                        } else {
                            Text(
                                "更新于 " + readTime(card.data.readAt),
                                style = MaterialTheme.typography.bodySmall.copy(shadow = shadow),
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 类别色块：浅色底 + 类别首字。 */
@Composable
private fun KindBadge(kind: HistoryEntry.Kind) {
    val color = kindColor(kind)
    Box(
        Modifier.size(36.dp).background(color.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(kind.label.take(1), color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
@Suppress("UNUSED_PARAMETER") // stationsReady 只用来在站名表加载完后触发重组
private fun HistoryRow(e: HistoryEntry, stationsReady: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KindBadge(e.kind)
        Column(Modifier.weight(1f)) {
            Text(entryTitle(e), style = MaterialTheme.typography.bodyLarge)
            val sub = listOfNotNull(entryDate(e), entryDetail(e)).joinToString("  ·  ")
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            val d = e.delta
            if (d != null) {
                Text(
                    signedYen(d),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (d > 0) IncomeColor else MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                "余额 " + yen(e.balance),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

