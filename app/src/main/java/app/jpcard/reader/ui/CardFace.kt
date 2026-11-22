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

/** Loads the custom face; reloads when [version] changes. Null when there is none or it cannot be read. */
@Composable
fun rememberCardFace(idm: String, version: Int): ImageBitmap? {
    val context = LocalContext.current
    val face by produceState(CardFaces.cached(context, idm), idm, version) {
        value = withContext(Dispatchers.IO) { CardFaces.load(context, idm) }
    }
    return face
}

/**
 * Card face background: the custom image if there is one (with a gradient so text stays readable),
 * otherwise the card type color.
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
                        // Text sits on the left, so darken the left side only and keep the image visible.
                        Brush.horizontalGradient(
                            0f to Color.Black.copy(alpha = 0.55f),
                            0.6f to Color.Black.copy(alpha = 0.15f),
                            1f to Color.Transparent,
                        ),
                    ),
                )
            }
        }
        content()
    }
}
