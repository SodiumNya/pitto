package app.jpcard.reader.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.jpcard.reader.data.SavedCard
import kotlinx.coroutines.launch

private val CardShape = RoundedCornerShape(20.dp)

@Composable
fun HomeScreen(state: AppState, actions: AppActions) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var renaming by remember { mutableStateOf<SavedCard?>(null) }

    // 删除后给 5 秒撤销机会，比确认弹窗更不打断操作，也能防误触
    val deleteWithUndo: (SavedCard) -> Unit = { card ->
        actions.delete(card.idm)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar(
                message = "已删除「${card.name}」",
                actionLabel = "撤销",
                duration = SnackbarDuration.Long,
            )
            if (r == SnackbarResult.ActionPerformed) actions.restore(card)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ReadingBar(state.reading)
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        "交通卡余额",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }
                item { StatusBanners(state, actions) }
                item { HintCard(state) }
                if (state.cards.isNotEmpty()) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 12.dp, start = 4.dp, end = 4.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(
                                "我的卡片",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "长按可重命名或删除",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                    items(state.cards, key = { it.idm }) { card ->
                        SavedCardRow(
                            card = card,
                            modifier = Modifier.animateItem(),
                            onClick = { actions.openCard(card.idm) },
                            onRename = { renaming = card },
                            onDelete = { deleteWithUndo(card) },
                        )
                    }
                }
            }
        }
    }

    renaming?.let { card ->
        RenameDialog(card.name, onDismiss = { renaming = null }) {
            actions.rename(card.idm, it)
            renaming = null
        }
    }
}

@Composable
private fun HintCard(state: AppState) {
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
                    !enabled -> "NFC 未开启，暂时无法读卡"
                    state.reading -> "正在读取，请不要移开…"
                    else -> "把 Suica / ICOCA 贴在手机背面"
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (enabled) "一般在摄像头附近，贴住 1 秒" else "开启 NFC 后即可使用",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SavedCardRow(
    card: SavedCard,
    modifier: Modifier,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = CardShape,
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .combinedClickable(onClick = onClick, onLongClick = { menu = true }),
        ) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(card.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "上次读取 " + readTime(card.data.readAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(yen(card.data.balance), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text("重命名") },
                leadingIcon = { Icon(Icons.Filled.Edit, null) },
                onClick = { menu = false; onRename() },
            )
            DropdownMenuItem(
                text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = { menu = false; onDelete() },
            )
        }
    }
}
