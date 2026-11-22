package app.jpcard.reader.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 用户自定义卡面。
 * 选中的图片会被居中裁剪成真实卡片比例（85.6 × 54 mm），缩放后存成 JPEG，
 * 只保存在 app 私有目录里。
 */
object CardFaces {
    const val RATIO = 85.6f / 54f
    private const val W = 1000
    private val H = (W / RATIO).roundToInt()

    private val cache = HashMap<String, Pair<Long, ImageBitmap>>()

    private fun file(context: Context, idm: String) = File(context.filesDir, "faces/$idm.jpg")

    fun has(context: Context, idm: String) = file(context, idm).exists()

    /** 从内存缓存直接取，取不到返回 null（不读盘，可在主线程调用）。 */
    fun cached(context: Context, idm: String): ImageBitmap? {
        val f = file(context, idm)
        val hit = synchronized(cache) { cache[idm] } ?: return null
        return if (f.exists() && f.lastModified() == hit.first) hit.second else null
    }

    /** 读盘解码，在后台线程调用。图片不存在或损坏时返回 null，界面会用默认卡面。 */
    fun load(context: Context, idm: String): ImageBitmap? {
        cached(context, idm)?.let { return it }
        val f = file(context, idm)
        if (!f.exists()) return null
        val bmp = BitmapFactory.decodeFile(f.path) ?: return null
        val img = bmp.asImageBitmap()
        synchronized(cache) { cache[idm] = f.lastModified() to img }
        return img
    }

    /** 处理并保存用户选的图片，在后台线程调用。 */
    fun save(context: Context, idm: String, uri: Uri): Boolean = runCatching {
        val src = decode(context, uri) ?: return false
        // 居中裁剪成卡片比例
        val (cw, ch) = if (src.width.toFloat() / src.height > RATIO) {
            (src.height * RATIO).roundToInt() to src.height
        } else {
            src.width to (src.width / RATIO).roundToInt()
        }
        val cropped = Bitmap.createBitmap(src, (src.width - cw) / 2, (src.height - ch) / 2, cw, ch)
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

    /** 解码时直接缩小到够用的尺寸，避免大照片占用太多内存；Android 9+ 会自动按 EXIF 旋转。 */
    private fun decode(context: Context, uri: Uri): Bitmap? {
        val cr = context.contentResolver
        return if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(cr, uri)) { d, info, _ ->
                val s = max(1, minOf(info.size.width / W, info.size.height / H))
                d.setTargetSize(info.size.width / s, info.size.height / s)
                d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= W && bounds.outHeight / (sample * 2) >= H) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        }
    }
}
