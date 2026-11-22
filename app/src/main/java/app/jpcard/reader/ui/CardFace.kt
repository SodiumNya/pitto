package app.jpcard.reader.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import app.jpcard.reader.data.CardFaces
import app.jpcard.reader.nfc.CardType
import app.jpcard.reader.ui.theme.cardColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 读取自定义卡面；version 变化（用户换了图）时重新加载。没有或读不出来时为 null。 */
@Composable
fun rememberCardFace(idm: String, version: Int): ImageBitmap? {
    val context = LocalContext.current
    val face by produceState(CardFaces.cached(context, idm), idm, version) {
        value = withContext(Dispatchers.IO) { CardFaces.load(context, idm) }
    }
    return face
}

/**
 * 卡面背景：有自定义图片就用图片（加一层渐变保证文字看得清），
 * 否则用卡种配色兜底。
 */
@Composable
fun CardFaceBox(
    type: CardType,
    face: ImageBitmap?,
    shape: Shape,
    modifier: Modifier = Modifier,
    scrim: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier.clip(shape).background(cardColor(type))) {
        if (face != null) {
            Image(face, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (scrim) {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.35f),
                            0.3f to Color.Transparent,
                            0.5f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.6f),
                        ),
                    ),
                )
            }
        }
        content()
    }
}
