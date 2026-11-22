package app.jpcard.reader.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * 贴卡提示图标。
 * 平时完全静止（卡片 + 非接触符号），不让人误以为一直在读取；
 * 只有正在读卡时才出现扩散波纹；读取失败时左右抖一下。
 */
@Composable
fun TapHint(reading: Boolean, enabled: Boolean, errorTick: Int, modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.primary
    val color = if (enabled) base else MaterialTheme.colorScheme.outline

    val shake = remember { Animatable(0f) }
    LaunchedEffect(errorTick) {
        if (errorTick == 0) return@LaunchedEffect
        for (x in listOf(10f, -8f, 6f, -4f, 0f)) shake.animateTo(x, tween(50))
    }

    Box(modifier.size(120.dp).graphicsLayer { translationX = shake.value.dp.toPx() }) {
        if (reading) Ripples(base)
        Canvas(Modifier.size(120.dp)) {
            val c = Offset(size.width / 2, size.height / 2)
            val w = size.width * 0.46f
            val h = w * 0.63f
            val left = c.x - w / 2 - 6.dp.toPx()
            val top = c.y - h / 2
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(w, h),
                cornerRadius = CornerRadius(7.dp.toPx()),
            )
            // 非接触符号的三道弧
            val cx = left + w + 4.dp.toPx()
            for (i in 1..3) {
                val r = (4 + i * 5).dp.toPx()
                drawArc(
                    color = color.copy(alpha = 1f - i * 0.22f),
                    startAngle = -45f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(cx - r, c.y - r),
                    size = Size(r * 2, r * 2),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
    }
}

@Composable
private fun Ripples(color: Color) {
    val t = rememberInfiniteTransition(label = "pulse")
    val p by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "p",
    )
    Canvas(Modifier.size(120.dp)) {
        val c = Offset(size.width / 2, size.height / 2)
        for (i in 0..1) {
            val f = (p + i * 0.5f) % 1f
            drawCircle(
                color = color.copy(alpha = 0.4f * (1 - f)),
                radius = size.minDimension * (0.3f + 0.2f * f),
                center = c,
                style = Stroke(width = 3.dp.toPx()),
            )
        }
    }
}

/** 重命名对话框（首页长按、详情页共用）。 */
@Composable
fun RenameDialog(current: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("卡片名称") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(20) },
                singleLine = true,
                placeholder = { Text("例如：我的 Suica") },
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text.trim()) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
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
