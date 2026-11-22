package app.jpcard.reader.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Custom card faces.
 * The area chosen in the crop screen is scaled to [W]x[H] and stored as a JPEG in app-private storage.
 */
object CardFaces {
    /** Card face aspect ratio (width:height), shared by the detail balance card and the crop frame. */
    const val RATIO = 2.5f
    private const val W = 1000
    private const val H = 400

    /** Longest side of the photo loaded into the crop screen: sharp enough without using too much memory. */
    private const val EDIT_MAX = 2048

    private val cache = HashMap<String, Pair<Long, ImageBitmap>>()

    private fun file(context: Context, idm: String) = File(context.filesDir, "faces/$idm.jpg")

    /** Returns the in-memory cached face or null. Does no disk I/O, so it is safe on the main thread. */
    fun cached(context: Context, idm: String): ImageBitmap? {
        val f = file(context, idm)
        val hit = synchronized(cache) { cache[idm] } ?: return null
        return if (f.exists() && f.lastModified() == hit.first) hit.second else null
    }

    /** Loads and decodes from disk; call off the main thread. Returns null if missing or broken, so the default face is used. */
    fun load(context: Context, idm: String): ImageBitmap? {
        cached(context, idm)?.let { return it }
        val f = file(context, idm)
        if (!f.exists()) return null
        val bmp = BitmapFactory.decodeFile(f.path) ?: return null
        val img = bmp.asImageBitmap()
        synchronized(cache) { cache[idm] = f.lastModified() to img }
        return img
    }

    /** Decodes the picked photo at an editable size; call off the main thread. Android 9+ applies the EXIF rotation automatically. */
    fun decodeForEdit(context: Context, uri: Uri): Bitmap? = runCatching {
        val cr = context.contentResolver
        if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(cr, uri)) { d, info, _ ->
                val longSide = max(info.size.width, info.size.height)
                if (longSide > EDIT_MAX) {
                    val s = EDIT_MAX.toFloat() / longSide
                    d.setTargetSize((info.size.width * s).roundToInt(), (info.size.height * s).roundToInt())
                }
                d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= EDIT_MAX) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        }
    }.getOrNull()

    fun rotate90(src: Bitmap): Bitmap =
        Bitmap.createBitmap(src, 0, 0, src.width, src.height, Matrix().apply { postRotate(90f) }, true)

    /** Saves the cropped area as the card face; call off the main thread. */
    fun save(context: Context, idm: String, src: Bitmap, crop: Rect): Boolean = runCatching {
        val r = Rect(crop).apply { if (!intersect(0, 0, src.width, src.height)) return false }
        val cropped = Bitmap.createBitmap(src, r.left, r.top, r.width(), r.height())
        val scaled = Bitmap.createScaledBitmap(cropped, W, H, true)

        val f = file(context, idm)
        f.parentFile?.mkdirs()
        val tmp = File(f.path + ".tmp")
        tmp.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        if (!tmp.renameTo(f)) {
            f.delete()
            tmp.renameTo(f)
        }
        synchronized(cache) { cache.remove(idm) }
        true
    }.getOrDefault(false)

    fun delete(context: Context, idm: String) {
        file(context, idm).delete()
        synchronized(cache) { cache.remove(idm) }
    }
}
