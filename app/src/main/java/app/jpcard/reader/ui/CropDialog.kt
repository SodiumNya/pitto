package app.jpcard.reader.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.jpcard.reader.R
import app.jpcard.reader.data.CardFaces
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Card face cropper: the image always covers the frame; drag, pinch to zoom, rotate in 90-degree steps.
 * Outside the frame is dimmed; inside is exactly the resulting face.
 */
@Composable
fun CropDialog(source: Bitmap, onCancel: () -> Unit, onConfirm: (Bitmap, android.graphics.Rect) -> Unit) {
    var bitmap by remember { mutableStateOf(source) }
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    var zoom by remember(bitmap) { mutableFloatStateOf(1f) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }
    var area by remember { mutableStateOf(Size.Zero) }
    val margin = with(LocalDensity.current) { 24.dp.toPx() }

    // All geometry uses the center of the drawing area as origin.
    val geo = remember(bitmap, area, margin) { CropGeometry(bitmap.width, bitmap.height, area, margin) }

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(Color.Black).windowInsetsPadding(WindowInsets.safeDrawing)) {
            Text(
                stringResource(R.string.crop_hint),
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 20.dp),
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(geo) {
                        detectTransformGestures { centroid, pan, zoomChange, _ ->
                            val newZoom = (zoom * zoomChange).coerceIn(1f, 6f)
                            val k = newZoom / zoom
                            // Zoom around the gesture centroid.
                            val c = centroid - Offset(area.width / 2, area.height / 2)
                            zoom = newZoom
                            offset = geo.clamp((offset - c) * k + c + pan, zoom)
                        }
                    },
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    area = size
                    if (geo.area != size) return@Canvas
                    val s = geo.scale(zoom)
                    val o = geo.clamp(offset, zoom)
                    val w = bitmap.width * s
                    val h = bitmap.height * s
                    drawImage(
                        image,
                        dstOffset = IntOffset(
                            (size.width / 2 + o.x - w / 2).roundToInt(),
                            (size.height / 2 + o.y - h / 2).roundToInt(),
                        ),
                        dstSize = IntSize(w.roundToInt(), h.roundToInt()),
                    )
                    val f = geo.frame
                    val topLeft = Offset((size.width - f.width) / 2, (size.height - f.height) / 2)
                    val radius = CornerRadius(16.dp.toPx())
                    val dim = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRect(Rect(Offset.Zero, size))
                        addRoundRect(RoundRect(Rect(topLeft, f), radius))
                    }
                    drawPath(dim, Color.Black.copy(alpha = 0.6f))
                    drawRoundRect(Color.White, topLeft, f, radius, style = Stroke(1.5.dp.toPx()))
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel), color = Color.White) }
                TextButton(onClick = { bitmap = CardFaces.rotate90(bitmap) }) {
                    Icon(Icons.Filled.Refresh, null, tint = Color.White)
                    Text("  " + stringResource(R.string.rotate), color = Color.White)
                }
                Button(
                    enabled = area != Size.Zero,
                    onClick = { onConfirm(bitmap, geo.cropRect(offset, zoom)) },
                ) { Text(stringResource(R.string.ok)) }
            }
        }
    }
}

/** Geometry between the crop frame and the image; origin at the center of the drawing area. */
private class CropGeometry(val bw: Int, val bh: Int, val area: Size, margin: Float) {
    /** Crop frame: [margin] on both sides, card face aspect ratio. */
    val frame: Size = (area.width - margin * 2).coerceAtLeast(1f).let { Size(it, it / CardFaces.RATIO) }

    /** At zoom 1 the image just covers the frame. */
    private val base = max(frame.width / bw, frame.height / bh)

    fun scale(zoom: Float) = base * zoom

    /** Limits panning so the frame is always covered by the image. */
    fun clamp(o: Offset, zoom: Float): Offset {
        val s = scale(zoom)
        val maxX = ((bw * s - frame.width) / 2).coerceAtLeast(0f)
        val maxY = ((bh * s - frame.height) / 2).coerceAtLeast(0f)
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }

    /** The frame mapped to pixels of the source image. */
    fun cropRect(offset: Offset, zoom: Float): android.graphics.Rect {
        val s = scale(zoom)
        val o = clamp(offset, zoom)
        val left = bw / 2f - (frame.width / 2 + o.x) / s
        val top = bh / 2f - (frame.height / 2 + o.y) / s
        return android.graphics.Rect(
            left.roundToInt(),
            top.roundToInt(),
            (left + frame.width / s).roundToInt(),
            (top + frame.height / s).roundToInt(),
        )
    }
}
