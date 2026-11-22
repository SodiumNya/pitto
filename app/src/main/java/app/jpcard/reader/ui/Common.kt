package app.jpcard.reader.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** 贴卡提示动画：一张卡 + 向外扩散的波纹。 */
@Composable
fun TapHint(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val t = rememberInfiniteTransition(label = "pulse")
    val p by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "p",
    )
    Canvas(modifier.size(120.dp)) {
        val c = Offset(size.width / 2, size.height / 2)
        for (i in 0..1) {
            val f = (p + i * 0.5f) % 1f
            drawCircle(
                color = color.copy(alpha = 0.35f * (1 - f)),
                radius = size.minDimension * (0.28f + 0.22f * f),
                center = c,
                style = Stroke(width = 3.dp.toPx()),
            )
        }
        val w = size.width * 0.42f
        val h = w * 0.63f
        drawRoundRect(
            color = color,
            topLeft = Offset(c.x - w / 2, c.y - h / 2),
            size = Size(w, h),
            cornerRadius = CornerRadius(6.dp.toPx()),
        )
    }
}

@Composable
fun ReadingBar(visible: Boolean) {
    AnimatedVisibility(visible) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}

@Composable
fun Banner(
    text: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Warning, null, Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(text, Modifier.weight(1f).padding(vertical = 10.dp), style = MaterialTheme.typography.bodyMedium)
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) { Text(actionLabel) }
            }
            if (onClose != null) {
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "关闭", Modifier.size(18.dp)) }
            }
        }
    }
}

@Composable
fun StatusBanners(state: AppState, actions: AppActions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (state.nfcStatus) {
            NfcStatus.DISABLED -> Banner("NFC 未开启", "去开启", actions.openNfcSettings)
            NfcStatus.UNSUPPORTED -> Banner("这台手机不支持 NFC，无法读卡")
            NfcStatus.READY -> Unit
        }
        state.error?.let { Banner(it, onClose = actions.dismissError) }
    }
}
