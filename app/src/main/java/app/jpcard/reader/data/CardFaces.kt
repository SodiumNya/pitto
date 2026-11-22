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
 * 用户自定义卡面。
 * 用户在裁剪界面里选定的区域会被缩放成 [W]×[H] 存成 JPEG，只保存在 app 私有目录里。
 */
object CardFaces {
    /** 卡面统一比例（宽:高）。详情页余额卡和主页小卡面都用这个比例，裁剪结果完全一致。 */
    const val RATIO = 2.5f
    private const val W = 1000
    private const val H = 400

    /** 裁剪界面用的原图最长边，够清晰又不占太多内存。 */
    private const val EDIT_MAX = 2048

    private val cache = HashMap<String, Pair<Long, ImageBitmap>>()

    private fun file(context: Context, idm: String) = File(context.filesDir, "faces/$idm.jpg")

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

    /** 把用户选的图片解码成适合编辑的大小，在后台线程调用。Android 9+ 会自动按 EXIF 摆正方向。 */
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

    /** 按裁剪区域保存卡面，在后台线程调用。 */
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
