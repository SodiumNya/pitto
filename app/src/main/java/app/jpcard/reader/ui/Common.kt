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
import app.jpcard.reader.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 贴卡提示图：手机正面（屏幕上写着 NFC）+ 左右两组感应波纹。
 * 平时完全静止，不让人误以为一直在读取；只有正在读卡时从手机向外扩散波纹；读取失败时左右抖一下。
 */
@Composable
fun TapHint(reading: Boolean, enabled: Boolean, errorTick: Int, modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.primary
    val color = if (enabled) base else MaterialTheme.colorScheme.outline
    val surface = MaterialTheme.colorScheme.surface

    val shake = remember { Animatable(0f) }
    LaunchedEffect(errorTick) {
        if (errorTick == 0) return@LaunchedEffect
        for (x in listOf(10f, -8f, 6f, -4f, 0f)) shake.animateTo(x, tween(50))
    }

    Box(
        modifier.size(128.dp).graphicsLayer { translationX = shake.value.dp.toPx() },
        contentAlignment = Alignment.Center,
    ) {
        if (reading) Ripples(base)
        Canvas(Modifier.size(128.dp)) {
            val w = size.width
            val h = size.height
            val line = 2.5.dp.toPx()

            // 手机正面，居中
            val phoneSize = Size(w * 0.36f, h * 0.72f)
            val phoneTopLeft = Offset((w - phoneSize.width) / 2, (h - phoneSize.height) / 2)
            val phoneCorner = CornerRadius(10.dp.toPx())
            drawRoundRect(surface, phoneTopLeft, phoneSize, phoneCorner)
            drawRoundRect(color, phoneTopLeft, phoneSize, phoneCorner, style = Stroke(line))
            // 屏幕
            val inset = 5.dp.toPx()
            drawRoundRect(
                color.copy(alpha = 0.10f),
                phoneTopLeft + Offset(inset, inset),
                Size(phoneSize.width - inset * 2, phoneSize.height - inset * 2),
                CornerRadius(6.dp.toPx()),
            )
            // 前置摄像头开孔
            drawCircle(
                color.copy(alpha = 0.6f),
                radius = 2.dp.toPx(),
                center = phoneTopLeft + Offset(phoneSize.width / 2, inset + 5.dp.toPx()),
            )

            // 左右两组感应波纹，以手机中心为圆心
            val c = Offset(w / 2, h / 2)
            for (i in 0..1) {
                val r = phoneSize.width / 2 + (16 + i * 9).dp.toPx()
                val alpha = 0.8f - i * 0.35f
                for (start in listOf(-30f, 150f)) {
                    drawArc(
                        color = color.copy(alpha = alpha),
                        startAngle = start,
                        sweepAngle = 60f,
                        useCenter = false,
                        topLeft = c - Offset(r, r),
                        size = Size(r * 2, r * 2),
                        style = Stroke(width = line, cap = StrokeCap.Round),
                    )
                }
            }
        }
        // 屏幕上的 NFC 字样
        Text(
            "NFC",
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
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
    Canvas(Modifier.size(128.dp)) {
        val c = Offset(size.width / 2, size.height / 2)
        for (i in 0..1) {
            val f = (p + i * 0.5f) % 1f
            drawCircle(
                color = color.copy(alpha = 0.4f * (1 - f)),
                radius = size.minDimension * (0.26f + 0.24f * f),
                center = c,
                style = Stroke(width = 3.dp.toPx()),
            )
        }
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
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, stringResource(R.string.close), Modifier.size(18.dp)) }
            }
        }
    }
}

@Composable
fun StatusBanners(state: AppState, actions: AppActions, showNfc: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 主页的提示卡已经说明了 NFC 状态，避免重复
        if (showNfc) {
            when (state.nfcStatus) {
                NfcStatus.DISABLED -> Banner(stringResource(R.string.nfc_off), stringResource(R.string.action_turn_on), actions.openNfcSettings)
                NfcStatus.UNSUPPORTED -> Banner(stringResource(R.string.banner_unsupported))
                NfcStatus.READY -> Unit
            }
        }
        state.error?.let { Banner(stringResource(it), onClose = actions.dismissError) }
    }
}
