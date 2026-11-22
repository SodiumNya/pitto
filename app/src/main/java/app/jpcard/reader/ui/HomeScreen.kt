package app.jpcard.reader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.jpcard.reader.R
import app.jpcard.reader.data.SavedCard

@Composable
fun HomeScreen(state: MainViewModel, actions: AppActions) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ReadingBar(state.reading)
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    // Title plus the settings entry at the top right (future options go there).
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Pitto",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = actions.openSettings) {
                            Icon(Icons.Outlined.Settings, stringResource(R.string.settings), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item { StatusBanners(state, actions, showNfc = false) }
                item { HintCard(state, actions) }
                if (state.cards.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.my_cards),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp, start = 4.dp),
                        )
                    }
                    items(state.cards, key = { it.idm }) { card ->
                        SavedCardRow(card, state.faceVersion, Modifier.animateItem()) { actions.openCard(card.idm) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HintCard(state: MainViewModel, actions: AppActions) {
    val enabled = state.nfcStatus == NfcStatus.READY
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (enabled) 0.5f else 0.2f),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(vertical = 28.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TapHint(reading = state.reading, enabled = enabled, errorTick = state.errorTick)
            Spacer(Modifier.height(12.dp))
            Text(
                when {
                    state.nfcStatus == NfcStatus.UNSUPPORTED -> stringResource(R.string.nfc_unsupported_title)
                    !enabled -> stringResource(R.string.nfc_off)
                    state.reading -> stringResource(R.string.home_reading)
                    state.cards.isEmpty() -> stringResource(R.string.home_hint_add)
                    else -> stringResource(R.string.home_hint_add_or_update)
                },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                when {
                    state.nfcStatus == NfcStatus.UNSUPPORTED -> stringResource(R.string.home_sub_unsupported)
                    !enabled -> stringResource(R.string.home_sub_nfc_off)
                    else -> stringResource(R.string.home_sub_supported)
                },
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.nfcStatus == NfcStatus.DISABLED) {
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(onClick = actions.openNfcSettings) { Text(stringResource(R.string.turn_on_nfc)) }
            }
        }
    }
}

/** Mini card: the custom face thumbnail, or the type color with its name. */
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
                card.data.type.displayName(LocalContext.current),
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
                    stringResource(R.string.updated_at, readTime(card.data.readAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(yen(card.data.balance), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}
